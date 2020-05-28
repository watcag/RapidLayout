package Experiment.optimize;

import Utils.Utility;
import cma.PlaceDecoder;
import cma.cmaes;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.device.SiteTypeEnum;
import main.MinRect;
import main.Tool;
import org.apache.commons.math3.optim.ConvergenceChecker;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.random.JDKRandomGenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CMAES {

    private static String device;
    private static final SiteTypeEnum DSP_SITE_TYPE = SiteTypeEnum.DSP48E2;
    private static final SiteTypeEnum BRAM_SITE_TYPE = SiteTypeEnum.RAMB180;
    private static final SiteTypeEnum URAM_SITE_TYPE = SiteTypeEnum.URAM288;
    private static final SiteTypeEnum DSP_MAP = SiteTypeEnum.SLICEL;
    private static final SiteTypeEnum BRAM_MAP = SiteTypeEnum.BUFG;
    private static final SiteTypeEnum URAM_MAP = SiteTypeEnum.LAGUNA;
    private static int iteration;

    private static boolean collect_gif_data;
    private static boolean collect_converge_data;

    public static Map<Integer, List<Site[]>> opt_search(
            int x_min, int x_max, int y_min, int y_max,
            String Device,
            int block_num
    ) throws IOException {

        /* optimization setup */
        com.xilinx.rapidwright.device.Device dev = (new Design("new Design", Device)).getDevice();
        int maxIteration = 100000;
        double stopFitness = 4500;
        boolean isActiveCMA = false;
        int diagonalOnly = 1000000;
        int checkFeasibleCount = 0;
        boolean generateStatistics = true;

        /* define two convergence checkers for data collection */
        String cvg_data_dir = System.getProperty("RAPIDWRIGHT_PATH") + "/result/CMA_convergence_data/";
        if (collect_converge_data) {
            File cvg_dir = new File(cvg_data_dir);
            if (cvg_dir.mkdirs())
                System.out.println("created dir " + cvg_dir);
            else
                System.out.println("directory " + cvg_dir + " exists");
        }
        String converge_data = cvg_data_dir + "run_at_" + System.currentTimeMillis() + ".txt";

        /* convergence checker: called after each iteration */
        ConvergenceChecker<PointValuePair> checker_for_convergence_data = (i, previous, current) -> {
            Map<SiteTypeEnum, List<List<Site>>> allAvailSites = Opt.PlaceCreator.getAvailableSites(dev, x_min, x_max, y_min, y_max);
            // change the keys of selected sites to accustom cma decoder
            Map<SiteTypeEnum, List<List<Site>>> adapted = new HashMap<>();
            adapted.put(DSP_MAP, allAvailSites.get(DSP_SITE_TYPE));
            adapted.put(BRAM_MAP, allAvailSites.get(BRAM_SITE_TYPE));
            adapted.put(URAM_MAP, allAvailSites.get(URAM_SITE_TYPE));
            Map<SiteTypeEnum, List<Site[]>> selectedSites = cmaes.chooseSiteUniformly(adapted, block_num);
            double wirelength = 0, size;
            try {
                PrintWriter pr = new PrintWriter(new FileWriter(converge_data, true), true);
                Map<Integer, List<Site[]>> placement = PlaceDecoder.decode(current.getPoint(), selectedSites);
                Utility U = new Utility(placement, Device);
                wirelength = U.getUnifiedWireLength();
                size = U.getMaxBBoxSize();

                pr.println(i + " " + wirelength + " " + size);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return wirelength < 4500;
        };


        /* Convergence checker: GIF data, called after each iteration */
        String gif_data_dir = System.getProperty("RAPIDWRIGHT_PATH") + "/result/CMA_gif_data/";
        File gif_dir = new File(gif_data_dir);
        if (collect_gif_data) {
            if (gif_dir.mkdirs())
                System.out.println("created dir " + gif_dir);
            else
                System.out.println("directory " + gif_dir + " exists");
        }
        ConvergenceChecker<PointValuePair> checker_for_gif_data = (i, previous, current) -> {


            Map<SiteTypeEnum, List<List<Site>>> allAvailSites = Opt.PlaceCreator.getAvailableSites(dev, x_min, x_max, y_min, y_max);
            // change the keys of selected sites to accustom cma decoder
            Map<SiteTypeEnum, List<List<Site>>> adapted = new HashMap<>();
            adapted.put(DSP_MAP, allAvailSites.get(DSP_SITE_TYPE));
            adapted.put(BRAM_MAP, allAvailSites.get(BRAM_SITE_TYPE));
            adapted.put(URAM_MAP, allAvailSites.get(URAM_SITE_TYPE));
            Map<SiteTypeEnum, List<Site[]>> selectedSites = cmaes.chooseSiteUniformly(adapted, block_num);
            Map<Integer, List<Site[]>> placement = PlaceDecoder.decode(current.getPoint(), selectedSites);
            Utility U = new Utility(placement, Device);
            double wirelength = U.getUnifiedWireLength();
            if (i > 30000 || i % 3 != 0) return wirelength < 4500;

            try {
                PrintWriter pr = new PrintWriter(new FileWriter(gif_data_dir + i + ".xdc"), true);
                Tool.write_XDC(placement, pr);
                pr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return wirelength < 4500;
        };

        CMAESOptimizer opt = new CMAESOptimizer(maxIteration, stopFitness, isActiveCMA, diagonalOnly,
                checkFeasibleCount, new JDKRandomGenerator(), generateStatistics, null);

        if (collect_gif_data) {
            opt = new CMAESOptimizer(maxIteration, stopFitness, isActiveCMA, diagonalOnly,
                    checkFeasibleCount, new JDKRandomGenerator(), generateStatistics, checker_for_gif_data);
        }

        if (collect_converge_data) {
            opt = new CMAESOptimizer(maxIteration, stopFitness, isActiveCMA, diagonalOnly,
                    checkFeasibleCount, new JDKRandomGenerator(), generateStatistics, checker_for_convergence_data);
        }


        /* find solution */
        long start_time = System.nanoTime();
        Map<Integer, List<Site[]>> solution = cmaes.Optimization(opt, dev, Device, x_min, x_max, y_min, y_max, block_num);
        long end_time = System.nanoTime();

        final String s = "CMA-ES Placement Solution Search Time = " + (end_time - start_time) / 1e9
                + " s, which is " + (end_time - start_time) / 1e9 / 60 + " min";
        System.out.println(s);

        Utility U = new Utility(solution, Device);
        double size = U.getMaxBBoxSize();
        double wireLength = U.getUnifiedWireLength();

        System.out.println("size = " + size);
        System.out.println("wirelength = " + wireLength);

        return solution;
    }

    public static double[] run() throws IOException {

        MinRect mr = new MinRect(device, 18, 8, 2);
        int blockn = mr.getBlocknum();
        int ymax = mr.getYmax();

        Map<Integer, List<Site[]>> result = opt_search(
                0, 6000, 0, ymax,
                device, blockn);

        Utility U = new Utility(result, device);
        double unitWireLength = U.getUnifiedWireLength();
        double size = U.getMaxBBoxSize();

        return new double[]{size, unitWireLength};
    }

    public static void collect_data() throws IOException {
        String perfFile = System.getenv("RAPIDWRIGHT_PATH") + "/result/CMA_perf.txt";

        PrintWriter pr = new PrintWriter(new FileWriter(perfFile, true), true);

        for (int i=0; i < iteration; i++) {

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

    /* mode = 0: does not write out convergence data or gif data
       mode = 1: write out convergence data
       mode = 2: write out gif data
    */
    public static void call(String dev, int mode, int it) throws IOException {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));

        iteration = it;

        switch (mode){
            case 1:
                collect_converge_data = true;
                collect_gif_data = false;
                break;
            case 2:
                collect_converge_data = false;
                collect_gif_data = true;
                break;
            default:
                collect_converge_data = false;
                collect_gif_data = false;
                break;
        }

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
        collect_gif_data = false;
        collect_converge_data = false;
        collect_data();
    }
}
