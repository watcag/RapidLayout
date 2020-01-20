package Opt;

import Utils.Utils;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.xilinx.rapidwright.device.Site;
import main.Tool;
import org.opt4j.core.*;
import org.opt4j.core.common.completer.IndividualCompleterModule;
import org.opt4j.core.optimizer.*;
import org.opt4j.core.start.Opt4JModule;
import org.opt4j.core.start.Opt4JTask;
import org.opt4j.optimizers.ea.*;
import org.opt4j.optimizers.sa.*;
import org.opt4j.viewer.ViewerModule;

import java.io.*;
import java.util.*;

class Global {
    public static String method;
    // we add this global time method to help label convergence data from each run
    // this will make things easier
    // so the plan is, everytime I start an optimization, I update this time string
    // and when the optimization happens, at each iteration, the string remain unchanged
    public static String time = "";
}


public class StartOptimization {


    static class CoolingScheduleLinear implements CoolingSchedule {
        @Override
        public double getTemperature(int i, int n) {
            return 100 * n - 10* i;
        }
    }

    public static class monitor implements OptimizerIterationListener {
        Archive archive;
        Control control;
        int old_iteration;
        double old_width;
        double old_wirelength;

        @Inject
        public monitor(Archive archive, Control control) {
            this.archive = archive;
            this.control = control;
        }

        @Override
        public void iterationComplete(int i) {
            Individual best = null;
            int iterator = 0;
            for (Individual individual : archive) {
                if (iterator == 0) {
                    best = individual;
                    iterator++;
                    continue;
                }
                double sumOfObjectives = 0;
                for (Objective objective : individual.getObjectives().getKeys())
                    sumOfObjectives += individual.getObjectives().get(objective).getDouble();

                double sumOfCurrentObjectives = 0;
                for (Objective objective : best.getObjectives().getKeys())
                    sumOfCurrentObjectives += best.getObjectives().get(objective).getDouble();

                if (sumOfObjectives < sumOfCurrentObjectives)
                    best = individual;

                iterator++;
            }


            assert best != null;
            int checkPeriod = 50000; // SA
            if (Global.method.equals("EA"))
                checkPeriod = 2000;

            Objectives objectives = best.getObjectives();
            if (i % checkPeriod == 0) {
                old_width = objectives.get(new Objective("Spread")).getDouble();
                old_wirelength = objectives.get(new Objective("unifWireLength")).getDouble();
                old_iteration = i;
            }

            double width = objectives.get(new Objective("Spread")).getDouble();
            double wirelength = objectives.get(new Objective("unifWireLength")).getDouble();

            /* ------- Print out the solution information for visualization ---------*/
            /*String path = System.getProperty("RAPIDWRIGHT_PATH") + "/result/" + Global.method + "/";
            File file = new File(path);
            if (file.mkdirs())
                System.out.println("directory " + path + " is created");
            if (i>30000) control.doTerminate();
            if (i%10==0) {
                String file_name = path + i + ".xdc";
                try {
                    PrintWriter pw = new PrintWriter(new FileWriter(file_name), true);
                    Tool.write_XDC((Map<Integer, List<Site[]>>) best.getPhenotype(), pw);
                    pw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }*/
            /*-----------------------------------------------------------------------*/


            /*--------------------plot convergence data----------------------*/
            if (i > 30000) control.doTerminate();
            String converge_data_path = System.getenv("RAPIDWRIGHT_PATH") + "/result/" + Global.method + "_convergence_data";
            File data_path = new File(converge_data_path);
            if (!data_path.exists())
                data_path.mkdirs();
            String this_run = converge_data_path + "/" + Global.time + ".txt";
            try {
                FileWriter this_run_fw = new FileWriter(this_run, true);
                PrintWriter this_run_pw = new PrintWriter(this_run_fw, true);
                this_run_pw.println(i + " " + wirelength + " " + width);
            } catch (IOException e) {
                e.printStackTrace();
            }
            /*--------------------plot convergence data---------------------*/

            if (width >= old_width - 10 && wirelength >= old_wirelength - 1e5 && i > 2 * checkPeriod && i % checkPeriod == checkPeriod - 1) {
                System.out.println("Terminated at iteration: " + i);
                control.doTerminate();
            }

        }
    }


    public Map<Integer, List<Site[]>> main(int block_num, String device,
                                           boolean visual,
                                           String method,
                                           int population, int parents, int children, double crossoverR,
                                           int x_min, int x_max, int y_min, int y_max
    ) {

        Global.method = method;
        Global.time = "run_at_" + System.currentTimeMillis();
        int iteration = (int)1e8;

        EvolutionaryAlgorithmModule evolutionaryAlgorithmModule = new EvolutionaryAlgorithmModule();
        evolutionaryAlgorithmModule.setGenerations(iteration);
        evolutionaryAlgorithmModule.setAlpha(population); // population size
        evolutionaryAlgorithmModule.setMu(parents); // number of parents
        evolutionaryAlgorithmModule.setLambda(children); // number of children
        evolutionaryAlgorithmModule.setCrossoverRate(crossoverR);

        SimulatedAnnealingModule sa = new SimulatedAnnealingModule();
        sa.setIterations(iteration);

        Nsga2Module nsga2Module = new Nsga2Module();
        nsga2Module.setTournament(20);

        CoolingScheduleModule coolingScheduleModule = new CoolingScheduleModule() {
            @Override
            protected void config() {
                bindCoolingSchedule(CoolingScheduleLinear.class);
                //bindCoolingSchedule(CoolingScheduleDefault.class);
            }
        };

        // initialize placement module and set parameters
        PlaceModule placementModule = new PlaceModule();
        placementModule.setBlock_num(block_num);
        placementModule.setDevice(device);
        placementModule.setX_min(x_min);
        placementModule.setX_max(x_max);
        placementModule.setY_min(y_min);
        placementModule.setY_max(y_max);
        placementModule.setMethod(method);


        ViewerModule viewer = new ViewerModule();
        viewer.setCloseOnStop(false);
        viewer.setTitle("Placement: " + block_num + "-block " + method);

        Opt4JModule observer = new Opt4JModule() {
            @Override
            protected void config() {
                addOptimizerIterationListener(monitor.class);
            }
        };

        // parallel decoding and evalutation
        IndividualCompleterModule individualCompleterModule = new IndividualCompleterModule();
        individualCompleterModule.setThreads(100);
        individualCompleterModule.setType(IndividualCompleterModule.Type.PARALLEL);

        // Optimization Task Initialization
        Opt4JTask task = new Opt4JTask(false);

        Collection<Module> modules = new ArrayList<>();
        if (method.equals("EA")) {
            modules.add(evolutionaryAlgorithmModule);
            modules.add(nsga2Module);
            modules.add(individualCompleterModule);
        } else {
            modules.add(sa);
            modules.add(coolingScheduleModule);
        }
        modules.add(placementModule);
        if (visual)
            modules.add(viewer);
        modules.add(observer);

        task.init(modules);


        Map<Integer, List<Site[]>> map = new HashMap<>();

        try {
            task.execute();
            Archive archive = task.getInstance(Archive.class);

            Individual best = null;
            int iterator = 0;
            for (Individual individual : archive) {
                if (iterator == 0) {
                    best = individual;
                    iterator++;
                    continue;
                }
                double sumOfObjectives = 0;
                for (Objective objective : individual.getObjectives().getKeys())
                    sumOfObjectives += individual.getObjectives().get(objective).getDouble();

                double sumOfCurrentObjectives = 0;
                for (Objective objective : best.getObjectives().getKeys())
                    sumOfCurrentObjectives += best.getObjectives().get(objective).getDouble();

                if (sumOfObjectives < sumOfCurrentObjectives)
                    best = individual;

                iterator++;
            }


            Map<Integer, List<Site[]>> phenotype = (Map<Integer, List<Site[]>>) best.getPhenotype();
            int blockNum = phenotype.size();
            Utils U = new Utils(phenotype, device);
            double unitWireLength = U.getUnifiedWireLength();
            // write out results
            System.out.println("Number of Blocks = " + blockNum);
            System.out.println("WireLengthPerBock = " + unitWireLength);
            System.out.println("----------------");

            Objectives objectives = best.getObjectives();
            for (Objective objective : objectives.getKeys()) {
                System.out.println(objective.getName() + " = " + objectives.get(objective).getDouble());
            }

            map.putAll(phenotype);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            task.close();
        }

        return map;
    }


}
