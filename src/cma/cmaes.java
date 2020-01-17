package cma;

import Utils.Utils;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.device.SiteTypeEnum;
import com.xilinx.rapidwright.util.Pair;
import main.Tool;
import org.apache.commons.math3.optim.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.random.JDKRandomGenerator;

import java.io.*;
import java.util.*;

public class cmaes {
    private static int n_dsp = 9;
    private static int n_bram = 4;
    private static int n_uram = 2;
    private static final SiteTypeEnum DSP_SITE_TYPE = SiteTypeEnum.DSP48E2;
    private static final SiteTypeEnum BRAM_SITE_TYPE = SiteTypeEnum.RAMB180;
    private static final SiteTypeEnum URAM_SITE_TYPE = SiteTypeEnum.URAM288;
    private static final SiteTypeEnum DSP_MAP = SiteTypeEnum.SLICEL;
    private static final SiteTypeEnum BRAM_MAP = SiteTypeEnum.BUFG;
    private static final SiteTypeEnum URAM_MAP = SiteTypeEnum.LAGUNA;

    public static double OptimSensAnalysis(CMAESOptimizer opt, List<Double> sigma_list, int population,
                                                               Device dev, String device,
                                                               int x_min, int x_max, int y_min, int y_max, int block_num){

        Map<SiteTypeEnum, List<List<Site>>> allAvailSites = Opt.PlaceCreator.getAvailableSites(dev, x_min, x_max, y_min, y_max);
        Map<SiteTypeEnum, List<List<Site>>> adapted = new HashMap<>();
        adapted.put(DSP_MAP, allAvailSites.get(DSP_SITE_TYPE));
        adapted.put(BRAM_MAP, allAvailSites.get(BRAM_SITE_TYPE));
        adapted.put(URAM_MAP, allAvailSites.get(URAM_SITE_TYPE));

        Map<SiteTypeEnum, List<Site[]>> selectedSites = chooseSiteUniform(adapted, block_num);

        PlaceEvaluator placeEvaluator = new PlaceEvaluator(selectedSites, device);

        final ObjectiveFunction objective = new ObjectiveFunction(placeEvaluator.getFitnessFunction());
        final CMAESOptimizer.PopulationSize populationSize = new CMAESOptimizer.PopulationSize(population);
        final GoalType goalType = GoalType.MINIMIZE;
        final InitialGuess initialGuess = new InitialGuess(PlaceCreator.getInitial(block_num));
        final CMAESOptimizer.Sigma sigma = new CMAESOptimizer.Sigma(sigma_list.stream().mapToDouble(Double::doubleValue).toArray());
        final MaxEval maxEval = new MaxEval((int)1e9);
        final MaxIter maxIter = new MaxIter((int)1e9);
        final SimpleBounds unbounded = SimpleBounds.unbounded(block_num * 5);

        PointValuePair result =
                opt.optimize(goalType, objective, populationSize, initialGuess, sigma, maxEval, maxIter, unbounded);

        Map<Integer, List<Site[]>> phenotype = PlaceDecoder.decode(result.getPoint(), selectedSites);
        Utils U = new Utils(phenotype, device);

        return U.getUnifiedWireLength();
    }

    public static Map<Integer, List<Site[]>> Optimization_param_fixed(CMAESOptimizer opt, Device dev, String device, int x_min, int x_max, int y_min, int y_max, int block_num){

        Map<SiteTypeEnum, List<List<Site>>> allAvailSites = Opt.PlaceCreator.getAvailableSites(dev, x_min, x_max, y_min, y_max);
        Map<SiteTypeEnum, List<List<Site>>> adapted = new HashMap<>();
        adapted.put(DSP_MAP, allAvailSites.get(DSP_SITE_TYPE));
        adapted.put(BRAM_MAP, allAvailSites.get(BRAM_SITE_TYPE));
        adapted.put(URAM_MAP, allAvailSites.get(URAM_SITE_TYPE));

        Map<SiteTypeEnum, List<Site[]>> selectedSites = chooseSiteUniform(adapted, block_num);

        PlaceEvaluator placeEvaluator = new PlaceEvaluator(selectedSites, device);
        List<Double> sigma_list = new ArrayList<>();
        for (int i = 0; i < 5 * block_num; i++) {
            sigma_list.add(0.5D);
        }

        final ObjectiveFunction objective = new ObjectiveFunction(placeEvaluator.getFitnessFunction());
        final CMAESOptimizer.PopulationSize populationSize = new CMAESOptimizer.PopulationSize(100);
        final GoalType goalType = GoalType.MINIMIZE;
        // can we add multiple genotype at initial guess?
        final InitialGuess initialGuess = new InitialGuess(PlaceCreator.getInitial(block_num));
        final CMAESOptimizer.Sigma sigma = new CMAESOptimizer.Sigma(sigma_list.stream().mapToDouble(Double::doubleValue).toArray());
        final MaxEval maxEval = new MaxEval((int)1e9);
        final MaxIter maxIter = new MaxIter((int)1e9);
        final SimpleBounds unbounded = SimpleBounds.unbounded(block_num * 5);

        PointValuePair result =
                opt.optimize(goalType, objective, populationSize, initialGuess, sigma, maxEval, maxIter, unbounded);


        System.out.println("result value = " + result.getValue());

        return PlaceDecoder.decode(result.getPoint(), selectedSites);
    }

    /*
        analyze sensitivity:
        population, sigma
     */
    public static void sensitivity_analysis(String out_path) throws IOException {
        FileWriter fw = new FileWriter(out_path);
        PrintWriter printWriter = new PrintWriter(fw, true);
        /* optimizer setup */
        int maxIteration = 100000;
        double stopFitness = 4500;
        boolean isActiveCMA = false;
        int diagonalOnly = 1000000;
        int checkFeasibleCount = 0;
        boolean generateStatistics = true;
        SimpleValueChecker simpleValueChecker = new SimpleValueChecker(-1, stopFitness);

        CMAESOptimizer opt = new CMAESOptimizer(maxIteration, stopFitness, isActiveCMA, diagonalOnly,
                checkFeasibleCount, new JDKRandomGenerator(), generateStatistics, null);

        /* optimization setup */
        String Device = "xcvu11p";
        int x_min = 0; int x_max = 6000; int y_min = 0; int y_max = 240;
        Device dev = (new Design("new Design", Device)).getDevice();
        int block_num = 80;

        /* analysis bound and step */
        int pop_min = 10, pop_max = 200, pop_step = 10;
        double sig_min = 0.1, sig_max = 1.5, sig_step = 0.1;

        for (int pop = pop_min; pop <= pop_max; pop+= pop_step) {
            for (double sig = sig_min; sig <= sig_max; sig+= sig_step) {
                List<Double> sigma = new ArrayList<>();
                for (int i = 0; i < 5 * block_num; i++)
                    sigma.add(sig);
                /* best performance of 10 experiments */
                double result = 1e10;
                for (int exp = 0; exp < 3; exp++) {
                    double tmp = OptimSensAnalysis(opt, sigma, pop,
                            dev, Device, x_min, x_max, y_min, y_max, block_num);
                    if (tmp < result)
                        result = tmp;
                }
                System.out.println("population = " + pop + " sigma = " + sig + " result = " + result);
                printWriter.println("population = " + pop + " sigma = " + sig + " result = " + result);
            }
        }

        printWriter.close();
        fw.close();
    }

    public static Map<Integer, List<Site[]>> opt_search(
            int x_min, int x_max, int y_min, int y_max,
            String Device,
            int block_num
    ) throws IOException {
        int maxIteration = 100000;
        double stopFitness = 4500;
        boolean isActiveCMA = false;
        int diagonalOnly = 1000000;
        int checkFeasibleCount = 0;
        boolean generateStatistics = true;

        /* optimization setup */
        Device dev = (new Design("new Design", Device)).getDevice();


        ConvergenceChecker<PointValuePair> checker = new ConvergenceChecker<PointValuePair>() {
            @Override
            public boolean converged(int i, PointValuePair previous, PointValuePair current) {

                /* this convergence checker is for convergence visualization, making gifs */
                if (i % 10 != 0) return i > 100000;

                System.out.println("i = " + i);
                String path = System.getProperty("RAPIDWRIGHT_PATH") + "/result/CMA/" + i + ".xdc";

                Map<SiteTypeEnum, List<List<Site>>> allAvailSites = Opt.PlaceCreator.getAvailableSites(dev, x_min, x_max, y_min, y_max);
                Map<SiteTypeEnum, List<List<Site>>> adapted = new HashMap<>();
                adapted.put(DSP_MAP, allAvailSites.get(DSP_SITE_TYPE));
                adapted.put(BRAM_MAP, allAvailSites.get(BRAM_SITE_TYPE));
                adapted.put(URAM_MAP, allAvailSites.get(URAM_SITE_TYPE));

                Map<SiteTypeEnum, List<Site[]>> selectedSites = chooseSiteUniform(adapted, block_num);
                try {
                    PrintWriter pr = new PrintWriter(new FileWriter(path));
                    Map<Integer, List<Site[]>> placement = PlaceDecoder.decode(current.getPoint(), selectedSites);
                    Tool.write_XDC(placement, pr);
                    pr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return i > 1000000;
            }
        };

        /* this part and the checker is used for collecting convergen plot's data */
        String data_dir = System.getProperty("RAPIDWRIGHT_PATH") + "/result/cma_converge_data/";
        File dir = new File(data_dir);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("created dir " + dir);
        } else {
            System.out.println("directory " + dir + "exists");
        }
        // everytime I call opt_search I create a new file whose name has the current time
        // add that is okay because we do this outside of the convergence checker.
        String converge_data = data_dir + "run_at_" + System.currentTimeMillis() + ".txt";

        ConvergenceChecker<PointValuePair> checker_for_convergence_data = new ConvergenceChecker<PointValuePair>() {
            @Override
            public boolean converged(int i, PointValuePair previous, PointValuePair current) {
                Map<SiteTypeEnum, List<List<Site>>> allAvailSites = Opt.PlaceCreator.getAvailableSites(dev, x_min, x_max, y_min, y_max);
                Map<SiteTypeEnum, List<List<Site>>> adapted = new HashMap<>();
                adapted.put(DSP_MAP, allAvailSites.get(DSP_SITE_TYPE));
                adapted.put(BRAM_MAP, allAvailSites.get(BRAM_SITE_TYPE));
                adapted.put(URAM_MAP, allAvailSites.get(URAM_SITE_TYPE));

                Map<SiteTypeEnum, List<Site[]>> selectedSites = chooseSiteUniform(adapted, block_num);
                try {
                    PrintWriter pr = new PrintWriter(new FileWriter(converge_data, true), true);
                    Map<Integer, List<Site[]>> placement = PlaceDecoder.decode(current.getPoint(), selectedSites);
                    Utils U = new Utils(placement, "xcvu11p");
                    double wirelength = U.getUnifiedWireLength();
                    double width = U.getMaxSpread();

                    pr.println(i + " " + wirelength + " " + width);

                } catch (IOException e) {
                    e.printStackTrace();
                }


                return false;
            }
        };

        CMAESOptimizer opt = new CMAESOptimizer(maxIteration, stopFitness, isActiveCMA, diagonalOnly,
                checkFeasibleCount, new JDKRandomGenerator(), generateStatistics, null);


        /* find solution */
        long start_time = System.nanoTime();
        Map<Integer, List<Site[]>> solution = Optimization_param_fixed(opt, dev, Device, x_min, x_max, y_min, y_max, block_num);
        long end_time = System.nanoTime();

        final String s = "CMA-ES Placement Solution Search Time = " + (end_time - start_time) / 1e9
                + " s, which is " + (end_time - start_time) / 1e9 / 60 + " min";
        System.out.println(s);

        Utils U = new Utils(solution, Device);
        double spread = U.getMaxSpread();
        double wireLength = U.getUnifiedWireLength();

        System.out.println("Spread = " + spread);
        System.out.println("wire length = " + wireLength);


        List<Double> fitnessHistory = opt.getStatisticsFitnessHistory();
        System.out.println("--------------------");
        System.out.println("cma search steps = " + fitnessHistory.size());
        /*for(double d : fitnessHistory)
            System.out.println(d);*/

        /*String xdc = System.getProperty("RAPIDWRIGHT_PATH")  +
                "/result/cma_blockNum=" + block_num + ".xdc";*/
        String xdc = System.getProperty("RAPIDWRIGHT_PATH") + "/result/blockNum=" + block_num + ".xdc";
        FileWriter fw = new FileWriter(xdc);
        PrintWriter printWriter = new PrintWriter(fw, true);
        Tool.write_XDC(solution, printWriter);
        printWriter.close();

        return solution;
    }


    public static void main(String[] args) throws IOException {

        String sens = "/home/niansong/RapidWright/result/cma_sensitivity.txt";
        sensitivity_analysis(sens);
    }

    public static Map<SiteTypeEnum, List<Site[]>> chooseSiteUniform(Map<SiteTypeEnum, List<List<Site>>> allAvailSites, int block_num) {


        // List<Site[]> each Site[] is a group of physical sites for one group of logical sites
        Map<SiteTypeEnum, List<Site[]>> selected_sites = new HashMap<>();
        // how many groups of hard blocks you need
        Integer[] numberOfSites = new Integer[]{2 * block_num, 2 * block_num, block_num};
        // how many hard blocks in each group
        Integer[] inGroup = new Integer[]{n_dsp, n_bram, n_uram};
        SiteTypeEnum[] siteTypes = new SiteTypeEnum[]{DSP_MAP, BRAM_MAP, URAM_MAP};

        for (SiteTypeEnum siteType : siteTypes) {
            // available sites for current site type
            List<List<Site>> availSites = allAvailSites.get(siteType);
            List<Site[]> selected_sites_for = new ArrayList<>(); // to store chosen sites for this type
            // calculate how many you should choose on each column
            int index = Arrays.asList(siteTypes).indexOf(siteType);
            int perCol = numberOfSites[index] / (availSites.size());
            int mod = numberOfSites[index] % (availSites.size());

            List<Integer> group_num_per_col = new ArrayList<>();
            for (int i = 0; i < availSites.size(); i++) {
                int num = i > mod - 1 ? perCol : perCol + 1;
                group_num_per_col.add(num);
            }

            if (siteType == BRAM_MAP) {
                // because you have to interleave BRAMs so it is kinda special
                for (List<Site> thisColSite : availSites) {
                    int col_idx = availSites.indexOf(thisColSite);
                    int num = group_num_per_col.get(col_idx);
                    for (int i = 0; i < num; i++) {
                        Site[] group = i % 2 == 0
                                ? new Site[]{thisColSite.get(4 * i), thisColSite.get(4 * i + 2), thisColSite.get(4 * i + 4), thisColSite.get(4 * i + 6)} // even
                                : new Site[]{thisColSite.get(4 * i - 3), thisColSite.get(4 * i - 1), thisColSite.get(4 * i + 1), thisColSite.get(4 * i + 3)};
                        selected_sites_for.add(group);
                    }
                }

            } else {
                // URAM and DSP you can just choose them continuously
                for (List<Site> thisColSite : availSites) {
                    int col_idx = availSites.indexOf(thisColSite);
                    int num = group_num_per_col.get(col_idx);
                    for (int i = 0; i < num; i++) { // group index
                        List<Site> group = new ArrayList<>();
                        for (int j = 0; j < inGroup[index]; j++) {
                            group.add(thisColSite.get(i * inGroup[index] + j));
                        }
                        // add this group to save list
                        selected_sites_for.add(group.toArray(new Site[0]));
                    }

                }
            }

            // collect selected groups of sites for each type
            selected_sites.put(siteType, selected_sites_for);
        }

        return selected_sites;
    }
}
