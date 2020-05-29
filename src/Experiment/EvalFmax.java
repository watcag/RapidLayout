package Experiment;

import main.AutoPlacement;
import main.Tool;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class EvalFmax {

    // two experiments here
    // 1. randomly optimize and implement a few placement and record performance, keep pipeline depth=0
    // 2. frequency vs pipeline depth, run a few placements and increase pipeline depth for each

    // the results are all concatenated in result/eval/<method>_eval.txt
    // format: <impl_runtime> <pipeline_depth> <frequency> <opt_runtime> <bbox_size> <wirelength>

    private static final String device = "xcvu11p";

    public static void impl(String device, String method, int depth) throws IOException {

        Tool.changeProperty("device", device);
        Tool.changeProperty("method", method);
        Tool.changeProperty("optimization", "true");
        Tool.changeProperty("rapidSynth", "true");
        Tool.changeProperty("SLRCopy", "true");
        Tool.changeProperty("autoPipeline", "false");
        Tool.changeProperty("pipelineDepth", Integer.toString(depth));
        Tool.changeProperty("vivado_verboe", "true");
        Tool.changeProperty("generate_gif", "false");
        Tool.changeProperty("transfer", "false");

        double[] metrics = AutoPlacement.toolflow();

        double freq = metrics[1];
        double impl_runtime = metrics[0];
        double opt_runtime = metrics[2];
        double bbox_size = metrics[3];
        double wirelength = metrics[4];

        String eval = System.getenv("RAPIDWRIGHT_PATH") + "/result/eval";
        File eval_dir = new File(eval);
        if (eval_dir.mkdirs())
            System.out.println("[RapidLayout] created directory: " + eval);

        String filename = eval + "/" + method + "_eval.txt";
        PrintWriter pr = new PrintWriter(new FileWriter(filename, true), true);
        pr.println(impl_runtime + " " + depth + " " + freq + " " + opt_runtime + " " + bbox_size + " " + wirelength);
        pr.close();
    }

    public static void impl_manual(String device, int depth, String placement) throws IOException {

        Tool.changeProperty("device", device);
        Tool.changeProperty("optimization", "false");
        Tool.changeProperty("placement", placement);
        Tool.changeProperty("rapidSynth", "true");
        Tool.changeProperty("SLRCopy", "true");
        Tool.changeProperty("autoPipeline", "false");
        Tool.changeProperty("pipelineDepth", Integer.toString(depth));
        Tool.changeProperty("vivado_verboe", "true");
        Tool.changeProperty("generate_gif", "false");
        Tool.changeProperty("transfer", "false");

        double[] metrics = AutoPlacement.toolflow();

        double freq = metrics[1];
        double impl_runtime = metrics[0];
        double opt_runtime = metrics[2];
        double bbox_size = metrics[3];
        double wirelength = metrics[4];

        String eval = System.getenv("RAPIDWRIGHT_PATH") + "/result/eval";
        File eval_dir = new File(eval);
        if (eval_dir.mkdirs())
            System.out.println("[RapidLayout] created directory: " + eval);

        String filename = eval + "/Manual_eval.txt";
        PrintWriter pr = new PrintWriter(new FileWriter(filename, true), true);
        pr.println(impl_runtime + " " + depth + " " + freq + " " + opt_runtime + " " + bbox_size + " " + wirelength);
        pr.close();
    }

    public static void compare_fmax(int iteration) throws IOException {
        String[] methods = {"SA", "EA", "EA-reduced", "CMA", "GA"};
        for (String method : methods) {
            for (int i = 0; i < iteration; i++) {
                impl(device, method, 0);
            }
        }
    }

    public static void fmax_vs_depth(int iteration) throws IOException {
        String[] methods = {"SA", "EA", "EA-reduced", "CMA", "GA", "Manual"};
        String manual_placement = System.getenv("RAPIDWRIGHT_PATH") + "/src/verilog/dsp_conv_chip.xdc";
        for (String method : methods) {
            for (int depth = 0; depth <=4; depth++) {
                for (int i = 0; i < iteration; i++) {
                    if (method.equals("Manual"))
                        impl_manual(device, depth, manual_placement);
                    else
                        impl(device, method, depth);
                }
            }
        }
    }


    public static void main(String[] args) throws IOException {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));


        compare_fmax(10);

        fmax_vs_depth(5);

    }
}
