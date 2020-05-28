package Experiment;


import Utils.Utility;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.device.Site;
import main.AutoPipeline;
import main.AutoPlacement;
import main.Tool;
import main.Vivado;

import java.io.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EvaluateFmax {

    private static final String root = System.getenv("RAPIDWRIGHT_PATH");
    private static final String checkpoint = root + "/checkpoint/";
    private static final String result = root + "/result/";
    private static final String device = "xcvu11p";
    private static final String part = new Design("name", device).getPartName();


    // get FMax for one solution
    // return double[]{frequency (in MHz, .1 floating point), runtime (min)}
    // save the checkpoint file: checkpoint/METHOD_eval/BBOXSIZE_FREQMHz.dcp
    public static double[] evaluate(String xdcFile, String method, double bboxSize, int depth) throws IOException {

        double t = 0; // overall time in min

        long start = System.nanoTime();

        // synthesize
        Design d = Vivado.synthesize_vivado(160, part, 0, true);
        //Design d = Vivado.synthesize_with_seed(160, device, 0, part, true);
        System.out.println("One SLR synthesis finished.");

        // read in 80 block placement and replicate to 1 SLR
        Map<Integer, List<Site[]>> p = Tool.getMapFromXDC(xdcFile, device);
        Map<Integer, List<Site[]>> placement = method.equals("Manual")
                ? p
                : AutoPlacement.populateFixed(p, device, 2);

        // placement
        System.out.println("Placement Start...");
        for (Integer index : placement.keySet()) {
            List<Site[]> blockConfig = placement.get(index);
            AutoPlacement.place_block(d, index, blockConfig);
        }
        System.out.println("Site-Routing ...");
        d.routeSites();

        // pipeline
        if (depth > 0)
            AutoPipeline.fixed_pipeline(d, depth, 160);

        // route and report frequency
        String placedDCPPath = checkpoint + method + "_placed.dcp";
        File file = new File(placedDCPPath);
        if (file.exists())
            file.delete();
        d.writeCheckpoint(placedDCPPath);
        double freq = Helpers.finishPlacementNRoutePBlock(placedDCPPath, method,
                bboxSize, placement, device, true);

        long end = System.nanoTime();

        t = (end-start) / 1e9 / 60;

        return new double[]{freq, t};
    }



    // evaluate the solutions inside one folder
    public static void oneBatch(String method) throws IOException {
        // setup checkpoint folders
        String ckptFolder = checkpoint + method + "_eval";
        File ckptPath = new File(ckptFolder);
        if (ckptPath.mkdirs())
            System.out.println("directory " + ckptFolder + " is created");

        // we put a text file inside the checkpoint folder to record following message:
        // implementation time (min), pipeline depth, max frequency, optimization runtime (secs), bboxsize, wirelength
        // each separated by a space
        String record_file = ckptFolder + "/record.txt";
        PrintWriter pw = new PrintWriter(new FileWriter(record_file, true), true);

        // get all xdc filenames in this folder
        final File srcFoler = new File(result + method + "_results");

        // implement each xdc placement result
        for(final File f : Objects.requireNonNull(srcFoler.listFiles())) {

            if (f.isDirectory()) continue;

            if (method.equals("Manual")) {
                Map<Integer, List<Site[]>> manual_pl = Tool.getMapFromXDC(f.getAbsolutePath(), device);
                Utility U = new Utility(manual_pl, device);
                double bboxSize = U.getMaxBBoxSize();
                double wirelength = U.getUnifiedWireLength();

                // implement the placement
                for (int pipeline = 0; pipeline <= 4; pipeline++) {
                    double[] impl = evaluate(f.getAbsolutePath(), method, bboxSize, pipeline);
                    double freq = impl[0];
                    double implRuntime = impl[1];

                    pw.println(implRuntime + " " + pipeline + " " + freq + " " + 0 + " " + bboxSize + " " + wirelength);
                    System.out.println("$$ Implementation Runtime: " + implRuntime + " FMax = " + freq + " pipeline = " + pipeline);
                }

            } else {

                // setup optimization perf information
                String name = f.getName();
                double bboxSize = Double.parseDouble(name.substring(method.length() + 1, name.length() - 4));
                double[] perfs = Helpers.getOptPerf(method, bboxSize);
                double optRuntime = perfs[0];
                double wirelength = perfs[2];

                // implement the placement
                for (int pipeline = 0; pipeline <= 4; pipeline++) {
                    double[] impl = evaluate(f.getAbsolutePath(), method, bboxSize, pipeline);
                    double freq = impl[0];
                    double implRuntime = impl[1];

                    pw.println(implRuntime + " " + pipeline + " " + freq + " " + optRuntime + " " + bboxSize + " " + wirelength);
                    System.out.println("$$ Implementation Runtime: " + implRuntime + " FMax = " + freq + " pipeline = " + pipeline);
                }
            }
        }

        pw.close();

    }


    public static void main(String[] args) throws IOException {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));

        // we evaluate one folder at one call
        /* File Naming Format:
        *  input: result/METHOD_results/xxx.xdc
        *  output: checkpoint/METHOD_eval/BBOXSIZE_FREQMHz.dcp
        * */

        String method = "EA-reduced"; // SA, EA, EA-reduced, CMA, GA, Manual

        oneBatch(method);

    }
}






class Helpers {

    private static final String root = System.getenv("RAPIDWRIGHT_PATH");
    private static final String checkpoint = root + "/checkpoint/";
    private static final String result = root + "/result/";
    private static final String tcl = root + "/tcl/";
    // we need a function to match optimization information from bboxsize

    // input method, bboxsize
    // return: optimization runtime (secs), bboxsize, wirelength
    public static double[] getOptPerf(String method, double bboxSize) throws IOException {

        String perfs = result + method + "_perf.txt";

        // read in data and build a list
        BufferedReader br = new BufferedReader(new FileReader(perfs));
        String line;
        Map<Double, double[]> map = new HashMap<>();
        while ((line = br.readLine()) != null) {
            double runtime = Double.parseDouble(line.split(" ")[0]);
            double size = Double.parseDouble(line.split(" ")[1]);
            double wirelength = Double.parseDouble(line.split(" ")[2]);

            if (!map.containsKey(size)) {
                map.put(size, new double[]{runtime, size, wirelength});
            }
        }

        return map.containsKey(bboxSize) ? map.get(bboxSize) : new double[]{0,0,0};
    }





    // checkpoint/METHOD_eval/BBOXSIZE_FREQMHz.dcp
    public static double finishPlacementNRoutePBlock(String placedDCP, String method, double bboxSize,
                                                     Map<Integer, List<Site[]>> placement, String device,
                                                     boolean verbose) throws IOException {
        String tcl_path = tcl + "finish_placement.tcl";
        String output_path = checkpoint + method + "_eval/routed.dcp";
        String output_edif = checkpoint + method + "_eval/routed.edf";
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

        String slack = Vivado.vivado_cmd("vivado -mode tcl -source " + tcl_path, verbose);

        double violation = Double.parseDouble(slack.substring(slack.indexOf("-"), slack.indexOf("ns")));
        double clk_period = 1 - violation;

        double freq = (1e9 / clk_period) / 1e6; // MHz

        DecimalFormat fmt = new DecimalFormat("#.##");

        // rename checkpoints
        File ckpt = new File(output_path);
        File edf = new File(output_edif);
        boolean success = ckpt.renameTo(new File(ckpt.getParent() + "/" + bboxSize + "_" + fmt.format(freq) + "MHz.dcp"));
        if (!success) System.out.println("rename DCP failed");
        success = edf.renameTo(new File(ckpt.getParent() + "/" + bboxSize + "_" + fmt.format(freq) + "MHz.edf"));
        if (!success) System.out.println("rename EDF failed");

        return freq;
    }



}
