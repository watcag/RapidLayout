package Experiment;

import Utils.Utility;
import cma.cmaes;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.Site;
import main.Tool;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import static main.Tool.changeProperty;

public class TuneCMAES {

    public static double[] run() throws IOException {

        String device = "xcvu11p";
        int blockn = 80;

        Map<Integer, List<Site[]>> result = cmaes.opt_search(
                0, 6000, 0, 2400,
                device, blockn);


        Utility U = new Utility(result, device);
        double unitWireLength = U.getUnifiedWireLength();
        double size = U.getMaxBBoxSize();

        // collect results
        String results_path = System.getenv("RAPIDWRIGHT_PATH") + "/result/CMA" + "_results";
        File data_path = new File(results_path);
        if (data_path.mkdirs())
            System.out.println("directory " + data_path + " is created");

        String xdcName = results_path + "/CMA-" + size + ".xdc";
        Tool.write_XDC(result, new PrintWriter(new FileWriter(xdcName), true));

        return new double[]{size, unitWireLength};
    }

    public static void collect_data() throws IOException {
        String perfFile = System.getenv("RAPIDWRIGHT_PATH") + "/result/CMA_perf.txt";
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
