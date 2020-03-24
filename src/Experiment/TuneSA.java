package Experiment;

import Opt.PlaceModule;
import Utils.Utility;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.xilinx.rapidwright.device.Site;
import org.opt4j.core.Individual;
import org.opt4j.core.Objective;
import org.opt4j.core.Objectives;
import org.opt4j.core.optimizer.Archive;
import org.opt4j.core.optimizer.Control;
import org.opt4j.core.optimizer.OptimizerIterationListener;
import org.opt4j.core.start.Opt4JModule;
import org.opt4j.core.start.Opt4JTask;
import org.opt4j.optimizers.sa.CoolingSchedule;
import org.opt4j.optimizers.sa.CoolingScheduleModule;
import org.opt4j.optimizers.sa.SimulatedAnnealingModule;
import org.opt4j.viewer.ViewerModule;

import java.io.*;
import java.util.*;


class Global {
    public static String method;
    public static String time = "";
    public static String schedule = "";
}

public class TuneSA {

    static class CoolingScheduleLinear implements CoolingSchedule {
        @Override
        // linear
//        public double getTemperature(int i, int n) {
//            Global.schedule = "10*n-100*i";
//            return 10*n-100*i;
//        }
//        public double getTemperature(int i, int n) {
//            // tn + t0 * Math.pow(alpha, i)
//            Global.schedule = "alpha=85%";
//            return 100 + 100 * Math.pow(0.85, i);
//        }
        public double getTemperature(int i, int n) {
            Global.schedule = "hyp-tn=100t0=1000";
            double tn = 100;
            double t0 = 1000;
            return t0 * Math.pow((tn/t0), (double)i/(double)n);
        }
    }

    public static class monitor implements OptimizerIterationListener {
        Archive archive;
        Control control;
        int old_iteration;
        double old_size;
        double old_wirelength;

        @Inject
        public monitor(Archive archive, Control control) {
            // extract archive and control handle of Opt4J task
            this.archive = archive;
            this.control = control;
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public void iterationComplete(int i) {
            // this function is called every time a iteration completes

            // select best individual
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

            // convergence checker
            assert best != null;
            int checkPeriod = 50000;

            Objectives objectives = best.getObjectives();
            if (i % checkPeriod == 0) {
                old_size = objectives.get(new Objective("Spread")).getDouble();
                old_wirelength = objectives.get(new Objective("unifWireLength")).getDouble();
                old_iteration = i;
            }

            double size = objectives.get(new Objective("Spread")).getDouble();
            double wirelength = objectives.get(new Objective("unifWireLength")).getDouble();

            if (size >= old_size - 10 && wirelength >= old_wirelength - 1e5 && i > 2 * checkPeriod && i % checkPeriod == checkPeriod - 1) {
                System.out.println("Terminated at iteration: " + i);
                control.doTerminate();
            }

            // data collection options
            if (i % 1000 == 0) {
                String converge_data_path = System.getenv("RAPIDWRIGHT_PATH") + "/result/TuneSA" + "_convergence_data";
                File data_path = new File(converge_data_path);
                if (data_path.mkdirs())
                    System.out.println("directory " + data_path + " is created");
                String this_run = converge_data_path + "/" + Global.schedule + ".txt";
                try {
                    FileWriter this_run_fw = new FileWriter(this_run, true);
                    PrintWriter this_run_pw = new PrintWriter(this_run_fw, true);
                    this_run_pw.println(i + " " + wirelength + " " + size);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }



    public static void run() {

        boolean visual = true;
        String device = "xcvu11p";

        Global.method = "SA";
        Global.time = "run_at_" + System.currentTimeMillis();
        int iteration = (int)1e8;

        SimulatedAnnealingModule sa = new SimulatedAnnealingModule();
        sa.setIterations(iteration);

        CoolingScheduleModule coolingScheduleModule = new CoolingScheduleModule() {
            @Override
            protected void config() {
                bindCoolingSchedule(CoolingScheduleLinear.class);
            }
        };

        // initialize placement module and set parameters
        PlaceModule placementModule = new PlaceModule();
        placementModule.setBlock_num(80);
        placementModule.setDevice("xcvu11p");
        placementModule.setX_min(0);
        placementModule.setX_max(6000);
        placementModule.setY_min(0);
        placementModule.setY_max(240);
        placementModule.setMethod("SA");


        ViewerModule viewer = new ViewerModule();
        viewer.setCloseOnStop(false);
        viewer.setTitle("Placement: " + 80 + "-block " + "SA");

        Opt4JModule observer = new Opt4JModule() {
            @Override
            protected void config() {
                addOptimizerIterationListener(monitor.class);
            }
        };


        // Optimization Task Initialization
        Opt4JTask task = new Opt4JTask(false);

        Collection<Module> modules = new ArrayList<>();
        modules.add(sa);
        modules.add(coolingScheduleModule);
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
            Utility U = new Utility(phenotype, device);
            double unitWireLength = U.getUnifiedWireLength();
            double size = U.getMaxBBoxSize();
            // write out results
            System.out.println("Number of Blocks = " + blockNum);
            System.out.println("WireLengthPerBock = " + unitWireLength);
            System.out.println("Size = " + size);
            System.out.println("------------------------");

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


    }

    public static void main(String[] args) {
        run();
    }
}
