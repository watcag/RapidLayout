package Experiment;

import Opt.PlaceModule;
import Opt.StartOptimization;
import Utils.Utility;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.xilinx.rapidwright.device.Site;
import main.Tool;
import org.opt4j.core.Individual;
import org.opt4j.core.Objective;
import org.opt4j.core.Objectives;
import org.opt4j.core.common.completer.IndividualCompleterModule;
import org.opt4j.core.optimizer.Archive;
import org.opt4j.core.optimizer.Control;
import org.opt4j.core.optimizer.OptimizerIterationListener;
import org.opt4j.core.start.Opt4JModule;
import org.opt4j.core.start.Opt4JTask;
import org.opt4j.optimizers.ea.EvolutionaryAlgorithmModule;
import org.opt4j.optimizers.ea.Nsga2Module;
import org.opt4j.optimizers.sa.SimulatedAnnealingModule;
import org.opt4j.viewer.ViewerModule;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static main.Tool.changeProperty;

public class TuneNSGA {

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
                double size = individual.getObjectives().get(new Objective("Spread")).getDouble();
                double bestSize = best.getObjectives().get(new Objective("Spread")).getDouble();
                if (size < bestSize)
                    best = individual;

                iterator++;
            }

            // convergence checker
            assert best != null;
            int checkPeriod = 3000;

            Objectives objectives = best.getObjectives();
            if (i % checkPeriod == 0) {
                old_size = objectives.get(new Objective("Spread")).getDouble();
                old_wirelength = objectives.get(new Objective("unifWireLength")).getDouble();
                old_iteration = i;
            }

            double size = objectives.get(new Objective("Spread")).getDouble();
            double wirelength = objectives.get(new Objective("unifWireLength")).getDouble();

//            if (old_size - size <= 10 && i > 2 * checkPeriod && i % checkPeriod == checkPeriod - 1) {
//                System.out.println("Terminated at iteration: " + i);
//                control.doTerminate();
//            }
            if (size < 1200 || i > 40000) {
                System.out.println("Terminated at iteration: " + i);
                control.doTerminate();
            }

            // data collection options
            if (i % 100 == 0) {
                String converge_data_path = System.getenv("RAPIDWRIGHT_PATH") + "/result/EA_convergence_data";
                File data_path = new File(converge_data_path);
                if (data_path.mkdirs())
                    System.out.println("directory " + data_path + " is created");
                String this_run = converge_data_path + "/" + Global.time + ".txt";
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


    public static double[] run() {

        String method = "EA";
        int population = 5;
        int parents = 100;
        int children = 200;
        double crossoverR = 1;

        boolean visual = true;
        String device = "xcvu11p";
        Global.method = method;
        Global.time = "run_at_" + System.currentTimeMillis();
        int iteration = (int)1e8;

        EvolutionaryAlgorithmModule evolutionaryAlgorithmModule = new EvolutionaryAlgorithmModule();
        evolutionaryAlgorithmModule.setGenerations(iteration);
        evolutionaryAlgorithmModule.setAlpha(population); // population size
        evolutionaryAlgorithmModule.setMu(parents); // number of parents
        evolutionaryAlgorithmModule.setLambda(children); // number of children
        evolutionaryAlgorithmModule.setCrossoverRate(crossoverR);

        Nsga2Module nsga2Module = new Nsga2Module();
        nsga2Module.setTournament(1);

        // initialize placement module and set parameters
        PlaceModule placementModule = new PlaceModule();
        placementModule.setBlock_num(80);
        placementModule.setDevice(device);
        placementModule.setX_min(0);
        placementModule.setX_max(6000);
        placementModule.setY_min(0);
        placementModule.setY_max(240);
        placementModule.setMethod(method);


        ViewerModule viewer = new ViewerModule();
        viewer.setCloseOnStop(false);
        viewer.setTitle("Placement: " + 80 + "-block " + method);

        Opt4JModule observer = new Opt4JModule() {
            @Override
            protected void config() {
                addOptimizerIterationListener(monitor.class);
            }
        };

        // parallel decoding and evalutation
        IndividualCompleterModule individualCompleterModule = new IndividualCompleterModule();
        // Multi-threading enabled, change threads here
        individualCompleterModule.setThreads(100);
        individualCompleterModule.setType(IndividualCompleterModule.Type.PARALLEL);

        // Optimization Task Initialization
        Opt4JTask task = new Opt4JTask(false);

        Collection<Module> modules = new ArrayList<>();
        modules.add(evolutionaryAlgorithmModule);
        modules.add(nsga2Module);
        modules.add(individualCompleterModule);

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

                double size = individual.getObjectives().get(new Objective("Spread")).getDouble();
                double bestSize = best.getObjectives().get(new Objective("Spread")).getDouble();
                if (size < bestSize)
                    best = individual;

                iterator++;
            }


            Map<Integer, List<Site[]>> phenotype = (Map<Integer, List<Site[]>>) best.getPhenotype();
            Utility U = new Utility(phenotype, device);
            double unitWireLength = U.getUnifiedWireLength();
            double size = U.getMaxBBoxSize();

            map.putAll(phenotype);

            // collect results
            String results_path = System.getenv("RAPIDWRIGHT_PATH") + "/result/" + method + "_results";
            File data_path = new File(results_path);
            if (data_path.mkdirs())
                System.out.println("directory " + data_path + " is created");

            String xdcName = results_path + "/" + method + "-" + size + ".xdc";
            Tool.write_XDC(map, new PrintWriter(new FileWriter(xdcName), true));

            return new double[]{size, unitWireLength};

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            task.close();
        }

        return new double[]{};
    }



    public static void collect_data() throws IOException {
        String perfFile = System.getenv("RAPIDWRIGHT_PATH") + "/result/EA_perf.txt";
        int times = 100;

        PrintWriter pr = new PrintWriter(new FileWriter(perfFile, true), true);

        for (int i=0; i < times; i++) {

            long start = System.currentTimeMillis();
            double[] perfs = run();
            long end = System.currentTimeMillis();
            double secs = (end-start) / 1e3;

            if (perfs.length < 2) {
                System.out.println("Run #" + i + " failed.");
            }

            System.out.println("secs = " + secs + " bbox size = " + perfs[0] + " wirelength = "  + perfs[1]);

            pr.println(secs + " " + perfs[0] + " " + perfs[1]);
        }

    }



    public static void main(String[] args) throws IOException {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));

        changeProperty("transfer", "false");
        collect_data();
    }

}
