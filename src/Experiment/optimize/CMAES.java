package Experiment.optimize;

import Utils.Utility;
import cma.cmaes;
import com.xilinx.rapidwright.device.Site;
import main.MinRect;
import main.Tool;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import static main.Tool.changeProperty;

public class CMAES {

    private static String device;

    public static double[] run() throws IOException {

        MinRect mr = new MinRect(device, 18, 8, 2);
        int blockn = mr.getBlocknum();
        int ymax = mr.getYmax();

        Map<Integer, List<Site[]>> result = cmaes.opt_search(
                0, 6000, 0, ymax,
                device, blockn);

        Utility U = new Utility(result, device);
        double unitWireLength = U.getUnifiedWireLength();
        double size = U.getMaxBBoxSize();

        return new double[]{size, unitWireLength};
    }

    public static void collect_data() throws IOException {
        String perfFile = System.getenv("RAPIDWRIGHT_PATH") + "/result/CMA_perf.txt";
        int times = 50;

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

    public static void call(String dev) throws IOException {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));
        device = dev;
        collect_data();
    }

    public static void main(String[] args) throws IOException {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));
        device = "vu11p";
        collect_data();
    }
}
