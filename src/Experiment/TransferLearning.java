package Experiment;

import Experiment.optimize.NSGA;
import main.*;

import java.io.*;

public class TransferLearning {

    private static final String root = System.getenv("RAPIDWRIGHT_PATH");
    private static final String result = root + "/result/";


    /* Function: gen seed placement
       Description: generate seed placement solution by calling NSGA-II optimization function
    */
    public static void gen_seed_placement(String dev, boolean visualize) throws IOException {

        Tool.changeProperty("transfer", "false");
        // use absolute checker
        NSGA.call(dev, visualize, true);
    }


    /*  Function: transfer
        Description: transfer learning function, calls opt() to perform optimization
        Output: "result/Transfer/opt_runtime.txt" to store runtime information
    */
    public static void transfer(String srcDevice, String targetDevice, boolean visualize) throws IOException {

        Tool.changeProperty("transfer", "true");
        Tool.changeProperty("initial_xdc", result + "Transfer/" + srcDevice + ".xdc");

        long start = System.nanoTime();

        // do not use absolute checker
        NSGA.call(targetDevice, visualize, false);

        long end = System.nanoTime();

        double secs = (end-start) / 1e9;

        PrintWriter pr = new PrintWriter(new FileWriter(result + "Transfer/opt_runtime.txt", true), true);
        pr.println(targetDevice + " " + secs);
    }


    /* Function: get all xdc
       Description: use vu3p and vu11p as seed device, to generate transferred placement solution
       on vu 3p, vu7p, vu9p. vu13p
    */
    public static void get_all_xdc(boolean visualize) throws IOException {

        // seed devices
        String[] seed_devs = new String[]{"xcvu11p", "xcvu3p"};
        // generate seed placement
        for (String seed_dev : seed_devs) {
            File seed_dev_file = new File(result + "Transfer/" + seed_dev + ".xdc");
            if (seed_dev_file.exists()) continue;
            gen_seed_placement(seed_dev, visualize);
        }

        // generate other devices' placement with transfer learning
        transfer("xcvu3p", "xcvu5p", visualize);
        transfer("xcvu3p", "xcvu7p", visualize);
        transfer("xcvu3p", "xcvu9p", visualize);
        transfer("xcvu11p", "xcvu13p", visualize);
    }


    /* Function: Batch Implementation
       Description: finish placement and route for a batch of placement results.
                    calls AutoPlacement.toolflow() to finish implementation
    */
    public static void batch_implement() throws IOException {
        String[] dev_list = new String[]{"xcvu3p", "xcvu5p", "xcvu7p", "xcvu9p", "xcvu13p"};
        for (String dev : dev_list) {
            Tool.changeProperty("device", dev);
            Tool.changeProperty("method", "EA");
            Tool.changeProperty("optimization", "false");
            Tool.changeProperty("rapidSynth", "true");
            Tool.changeProperty("SLRCopy", "true");
            Tool.changeProperty("autoPipeline", "true");
            Tool.changeProperty("placement", result + "Transfer/" + dev + ".xdc");
            Tool.changeProperty("vivado_verbose", "true");
            Tool.changeProperty("generate_gif", "false");
            Tool.changeProperty("transfer", "false");

            double[] perf = AutoPlacement.toolflow();

            PrintWriter pr = new PrintWriter(new FileWriter(result + "Transfer/freq.txt", true), true);
            pr.println(dev + " " + perf[1] + " " + perf[0]);
        }

    }

    /* Function: scratch time
       Description: test placement from scratch runtime
    */
    public static void scratch_time(boolean visualize) throws IOException {

        // seed devices
        String[] seed_devs = new String[]{"xcvu7p", "xcvu5p", "xcvu9p", "xcvu13p"};
        for (String seed_dev : seed_devs) {
            long start = System.nanoTime();
            gen_seed_placement(seed_dev, visualize);
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

        boolean visualize = false;

        get_all_xdc(visualize);
        batch_implement();
        //scratch_time();
    }

}
