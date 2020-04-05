package Experiment;

import Opt.PlaceModule;
import Utils.Utility;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.device.Site;
import main.*;
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
import org.opt4j.optimizers.sa.CoolingScheduleModule;
import org.opt4j.optimizers.sa.SimulatedAnnealingModule;
import org.opt4j.viewer.ViewerModule;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

import static main.Vivado.vivado_cmd;

public class OtherDevices {

    private static String root = System.getenv("RAPIDWRIGHT_PATH");
    private static String checkpoint = root + "/checkpoint/";
    private static String result = root + "/result/";
    private static String device = "xcvu37p";
    private static String part = new Design("name", device).getPartName();
    private static int blocknum = 0;
    private static int ymax = 0;
    private static int repl = 1;

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
            int checkPeriod = 300;

            Utility U = new Utility((Map<Integer, List<Site[]>>)best.getPhenotype(), device);
            double size = U.getMaxBBoxSize();
            double wirelength = U.getUnifiedWireLength();
            if (i % checkPeriod == 0) {
                old_size = size;
                old_wirelength = wirelength;
                old_iteration = i;
            }

            //if (size < 1200 && i > checkPeriod) {
            if (Math.abs(old_size-size) < 10 && Math.abs(old_wirelength-wirelength) < 100 && i > 2 * checkPeriod && i % checkPeriod == checkPeriod-1) {
                System.out.println("Terminated at iteration: " + i);
                control.doTerminate();
            }

            // data collection options
            if (i % 100 == 0) {
                String converge_data_path = result + "/Transfer";
                File data_path = new File(converge_data_path);
                if (data_path.mkdirs())
                    System.out.println("directory " + data_path + " is created");
                String this_run = converge_data_path + "/" + device + ".txt";
                try {
                    FileWriter this_run_fw = new FileWriter(this_run);
                    PrintWriter this_run_pw = new PrintWriter(this_run_fw, true);
                    this_run_pw.println(i + " " + wirelength + " " + size);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public static double[] opt() {

        String method = "EA";
        int population = 5;
        int parents = 100;
        int children = 200;
        double crossoverR = 1;

        boolean visual = true;
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
        // placement module supports Transfer Learning,
        // just change the Property file
        PlaceModule placementModule = new PlaceModule();
        placementModule.setBlock_num(blocknum);
        placementModule.setDevice(device);
        placementModule.setX_min(0);
        placementModule.setX_max(6000);
        placementModule.setY_min(0);
        placementModule.setY_max(ymax);
        placementModule.setMethod(method);


        ViewerModule viewer = new ViewerModule();
        viewer.setCloseOnStop(false);
        viewer.setTitle("Placement: " + blocknum + "-block " + device);

        Opt4JModule observer = new Opt4JModule() {
            @Override
            protected void config() {
                addOptimizerIterationListener(monitor.class);
            }
        };

        // parallel decoding and evalutation
        IndividualCompleterModule individualCompleterModule = new IndividualCompleterModule();
        individualCompleterModule.setThreads(1000);
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
            String results_path = result + "Transfer/";
            File data_path = new File(results_path);
            if (data_path.mkdirs())
                System.out.println("directory " + data_path + " is created");

            String xdcName = results_path + "/" + device + ".xdc";
            Tool.write_XDC(map, new PrintWriter(new FileWriter(xdcName), true));

            return new double[]{size, unitWireLength};

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            task.close();
        }

        return new double[]{};
    }



    // get FMax for one solution
    // return double[]{frequency (in MHz, .1 floating point), runtime (min)}
    public static double[] impl(String xdcFile, int depth) throws IOException {

        double t = 0; // overall time in min

        long start = System.nanoTime();

        // synthesize
        Design d = Helper2.synthesize_vivado(blocknum, part, 0, true);

        // read in 80 block placement and replicate to 1 SLR
        Map<Integer, List<Site[]>> p = Tool.getMapFromXDC(xdcFile, device);
        Map<Integer, List<Site[]>> placement = AutoPlacement.populateFixed(p, device, repl);

        // placement
        System.out.println("Placement Start...");
        for (Integer index : placement.keySet()) {
            List<Site[]> blockConfig = placement.get(index);
            AutoPlacement.place_block(d, index, blockConfig);
        }
        System.out.println("Site-Routing ...");
        d.routeSites();

        // pipeline
        AutoPipeline.fixed_pipeline(d, depth, blocknum);

        // route and report frequency
        String placedDCPPath = checkpoint + "transfer_tmp_placed.dcp";
        File file = new File(placedDCPPath);
        if (file.exists())
            file.delete();
        d.writeCheckpoint(placedDCPPath);
        double freq = Helper2.finishPlacementNRoutePBlock(placedDCPPath, placement, device, true);

        long end = System.nanoTime();

        t = (end-start) / 1e9 / 60;

        return new double[]{freq, t};
    }


    public static void checkResources() {
    // check resources of all UltraScale+ Non-HBM devices
        String[] dev_list = new String[]{
                "xcvu3p", "xcvu5p", "xcvu7p", "xcvu9p", "xcvu11p", "xcvu13p", "xcvu19p", "xcvu27p", "xcvu29p"
        };
        for (String dev : dev_list) {
            System.out.println(dev);
            MinRect mr = new MinRect(dev, 18, 8, 2);
            System.out.println("min area height (ymax): " + mr.getYmax() + " blocknum = " + mr.getBlocknum()
                    + " replication = " + mr.getReplication());
            System.out.println("\n\n");
        }
    }

    public static void batch_implement() throws IOException {
        //String[] dev_list = new String[]{"xcvu3p", "xcvu5p", "xcvu7p", "xcvu9p", "xcvu27p", "xcvu29p"};
        String[] dev_list = new String[]{"xcvu3p", "xcvu9p"};

        for (String dev : dev_list) {
            MinRect mr = new MinRect(dev, 18, 8, 2);
            blocknum = dev.equals("xcvu9p") ? 123 :  mr.getBlocknum();
            device = dev;
            part = new Design("name", device).getPartName();
            ymax = mr.getYmax();
            repl = mr.getReplication();

            double[] perf = impl(result + "Transfer/" + dev + ".xdc", 4);

            PrintWriter pr = new PrintWriter(new FileWriter(result + "Transfer/freq.txt", true), true);
            pr.println(dev + " " + perf[1] + " " + perf[0]);

        }

    }

    public static void gen_seed_placement(String dev) throws IOException {
        MinRect mr = new MinRect(dev, 18, 8, 2);
        // minrect has a problem with vu9p that I don't have energy to fix right now
        blocknum = dev.equals("xcvu9p") ? 123 :  mr.getBlocknum();
        device = dev;
        part = new Design("name", device).getPartName();
        ymax = mr.getYmax();

        Tool.changeProperty("transfer", "false");

        opt();
    }

    public static void transfer(String srcDevice, String targetDevice) throws IOException {
        MinRect mr = new MinRect(targetDevice, 18, 8, 2);
        blocknum = targetDevice.equals("xcvu9p") ? 123 :  mr.getBlocknum();
        System.out.println(blocknum);
        device = targetDevice;
        part = new Design("name", device).getPartName();
        ymax = mr.getYmax();

        Tool.changeProperty("transfer", "true");
        Tool.changeProperty("initial_xdc", result + "Transfer/" + srcDevice + ".xdc");

        long start = System.nanoTime();

        opt();

        long end = System.nanoTime();

        double secs = (end-start) / 1e9;

        PrintWriter pr = new PrintWriter(new FileWriter(result + "Transfer/opt_runtime.txt", true), true);
        pr.println(targetDevice + " " + secs);
    }

    public static void get_all_xdc() throws IOException {

        // seed devices
        String[] seed_devs = new String[]{"xcvu11p", "xcvu3p"};
        for (String seed_dev : seed_devs) {
            File seed_dev_file = new File(result + "Transfer/" + seed_dev + ".xdc");
            if (seed_dev_file.exists()) continue;
            gen_seed_placement(seed_dev);
        }

        // generate other devices' placement from transfer
        transfer("xcvu3p", "xcvu5p");
        transfer("xcvu3p", "xcvu7p");
        transfer("xcvu3p", "xcvu9p");

        transfer("xcvu11p", "xcvu27p");
        transfer("xcvu11p", "xcvu29p");

    }

    public static void scratch_time() throws IOException {

        // seed devices
        String[] seed_devs = new String[]{"xcvu3p", "xcvu7p", "xcvu5p", "xcvu9p"};
        for (String seed_dev : seed_devs) {
            long start = System.nanoTime();
            gen_seed_placement(seed_dev);
            long end = System.nanoTime();
            System.out.println("Scratch Runtime: " + seed_dev + " " + (end-start)/1e9);
        }

    }


    public static void main(String[] args) throws IOException {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));

        //get_all_xdc();
        //batch_implement();
        //checkResources();
        scratch_time();
    }
}




























/*-------------------------------------------------------------------------------------------------------------------*/

class Helper2 {

    private static String root = System.getenv("RAPIDWRIGHT_PATH");
    private static String checkpoint = root + "/checkpoint/";
    private static String result = root + "/result/";
    private static String tcl = root + "/tcl/";

    // checkpoint/METHOD_eval/BBOXSIZE_FREQMHz.dcp
    public static double finishPlacementNRoutePBlock(String placedDCP,
                                                     Map<Integer, List<Site[]>> placement, String device,
                                                     boolean verbose) throws IOException {
        String tcl_path = tcl + "finish_placement.tcl";
        String output_path = checkpoint + "Transfer/routed.dcp";
        String output_edif = checkpoint +  "Transfer/routed.edf";
        // write tcl script
        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("open_checkpoint " + placedDCP);
            Vivado.PBlockConstraint(printWriter, placement, device);
            printWriter.println("create_clock -period 1.000 -waveform {0.000 0.500} [get_nets clk];");
            printWriter.println("place_design;");
            printWriter.println("route_design");
            printWriter.println("report_timing;");
            printWriter.println("write_checkpoint -force -file " + output_path);
            printWriter.println("write_edf -force -file " + output_edif);
            printWriter.println("exit");
            printWriter.close();
        }

        String slack = vivado_cmd("vivado -mode tcl -source " + tcl_path, verbose);

        double violation = Double.parseDouble(slack.substring(slack.indexOf("-"), slack.indexOf("ns")));
        double clk_period = 1 - violation;

        double freq = (1e9 / clk_period) / 1e6; // MHz

        DecimalFormat fmt = new DecimalFormat("#.##");

        // rename checkpoints
        File ckpt = new File(output_path);
        File edf = new File(output_edif);
        ckpt.renameTo(new File(ckpt.getParent() + "/" + device + "_" + fmt.format(freq) + "MHz.dcp"));
        edf.renameTo(new File(ckpt.getParent() + "/" + device + "_" + fmt.format(freq) + "MHz.edf"));

        return freq;
    }



    public static Design synthesize_vivado(int block_num, String part, int depth, boolean verbose){
        String tcl_path = tcl + "synth.tcl";
        String output_path = checkpoint  + block_num + "_" + part;
        File checkpoint = new File(output_path+".dcp");

        if (checkpoint.exists())
            return Design.readCheckpoint(output_path+".dcp");

        // write tcl script
        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("read_verilog ../verilog/addr_gen.v ../verilog/dsp_conv.v ../verilog/dsp_conv_top.v ../verilog/dsp_conv_chip.sv");
            printWriter.println("set_property generic {NUMBER_OF_REG=" + depth + " Y="+block_num+"} [current_fileset]");
            printWriter.println("synth_design -mode out_of_context -part "+ part +" -top dsp_conv_chip;");
            printWriter.println("write_checkpoint -force -file " + output_path + ".dcp");
            printWriter.println("write_edif -force -file " + output_path + ".edf");
            printWriter.println("exit");
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        long start_time = System.nanoTime();
        vivado_cmd("vivado -mode tcl -source " + tcl_path, verbose);
        long end_time = System.nanoTime();
        String s = "Synthesis - " + block_num + " conv blocks, time = " + (end_time-start_time)/1e9/60 + " min";
        System.out.println(">>>-----------------------------------------------");
        System.out.println(s);
        System.out.println(">>>-----------------------------------------------");

        return Design.readCheckpoint(output_path+".dcp");
    }






}