package main;

import Opt.StartOptimization;
import Utils.Utils;
import com.xilinx.rapidwright.design.*;
import com.xilinx.rapidwright.design.Module;
import com.xilinx.rapidwright.device.*;
import com.xilinx.rapidwright.edif.*;
import com.xilinx.rapidwright.util.RapidWright;
import cma.cmaes;

import java.io.*;
import java.nio.file.FileSystemNotFoundException;
import java.util.*;


public class AutoPlacement {

    /*
        Function: place_block
        places one convolutional block on device, without site routing
     */
    public static void place_block(Design d, int index, List<Site[]> placement) {

        // set up names
        String[] bramName = new String[8];
        for (int i = 1; i <= 4; i++)
            bramName[i - 1] = "name[" + index + "].dut/conv1/bram_inst_rdc" + i;
        for (int i = 1; i <= 4; i++)
            bramName[i + 3] = "name[" + index + "].dut/conv2/bram_inst_rdc" + i;
        String[] uramName = new String[]{
                "name[" + index + "].dut/uram_inst_wr", "name[" + index + "].dut/uram_inst_rd"};
        String[] dspName = new String[18];
        for (int i = 0; i < 8; i++)
            dspName[i] = "name[" + index + "].dut/conv2/dsp_chain" + i / 3 + "[" + i % 3 + "].dsp_inst";
        dspName[8] = "name[" + index + "].dut/conv2/dsp_inst8";
        for (int i = 0; i < 8; i++)
            dspName[i + 9] = "name[" + index + "].dut/conv1/dsp_chain" + i / 3 + "[" + i % 3 + "].dsp_inst";
        dspName[17] = "name[" + index + "].dut/conv1/dsp_inst8";

        // place everybody
        for (int i = 0; i < 18; i++) {
            PlaceHardBlock.placeDSP48E2(d, placement.get(0)[i].getName(), dspName[i]);
            System.out.println("[Placing] DSP Cell: " + dspName[i] + "\t-->\t" + placement.get(0)[i].getName());
        }
        for (int i = 0; i < 8; i++) {
            String name =  d.getNetlist().getCellInstFromHierName("name["+index+"].dut/conv"+(i/4+1)).getCellName();
            PlaceHardBlock.placeBRAM(d, placement.get(1)[i].getName(), bramName[i], name);
            System.out.println("[Placing] BRAM Cell: " + bramName[i] + "\t-->\t" + placement.get(1)[i].getName());
        }
        for (int i = 0; i < 2; i++) {
            String name =  d.getNetlist().getCellInstFromHierName("name["+index+"].dut").getCellName();
            PlaceHardBlock.placeURAM(d, placement.get(2)[i].getName(), uramName[i], name);
            System.out.println("[Placing] URAM Cell: " + uramName[i] + "\t-->\t" + placement.get(2)[i].getName());
        }

    }


    /*
    Function: find_solution
    Description: search placement solution for blockn within [min_blockn, max_blockn]
    */
    public static Map<Integer, List<Site[]>> find_solution(
            String method,
            int blockn, int iteration, boolean visual, String device,
            int population, int parents, int children, double crossoverR,
            int x_min, int x_max, int y_min, int y_max, PrintWriter log
    ) throws IOException {

        System.out.println("Searching for Placement Solution ...");

        // setup env
        String path = System.getProperty("RAPIDWRIGHT_PATH") + "/result/blockNum=" + blockn + ".xdc";
        FileWriter writer = new FileWriter(path);
        PrintWriter print_line = new PrintWriter(writer, true);
        Map<Integer, List<Site[]>> result = null;
        long start_time = System.nanoTime();

        final int iterations = iteration == -1 ? 300 + 20 * blockn : iteration;
        switch (method.toUpperCase()) {

            case "SA":
            case "EA":

                Opt.StartOptimization opt = new Opt.StartOptimization();
                result = opt.main(
                        iterations,
                        blockn, device, visual, method,
                        population, parents, children, crossoverR,
                        x_min, x_max, y_min, y_max);

                break;

            case "EA-REDUCED":
            case "SA-REDUCED":

                OptReduced.StartOptimization opt_r = new OptReduced.StartOptimization();
                result = opt_r.main(
                        iterations,
                        blockn, device, visual, method,
                        population, parents, children, crossoverR,
                        x_min, x_max, y_min, y_max);

                break;

            case "CMA":

                result = cmaes.opt_search(
                        x_min, x_max, y_min, y_max,
                        device,
                        blockn
                );

                break;

            default:
                System.out.println("[ERROR]: Unsupported Solver. in function List<> find_solution(...)");
                System.out.println("Choose between EA, SA, EA-reduced, SA-reduced, CMA");
                break;
        }

        // print timing information
        long end_time = System.nanoTime();
        String s = "Search for Placement Solution: " + (end_time-start_time)/1e9 + " s, which is " + (end_time-start_time)/1e9/60 + " min";
        System.out.println(">>>-----------------------------------------------");
        System.out.println(s);
        System.out.println(">>>-----------------------------------------------");
        log.println(">>>-----------------------------------------------");
        log.println(s);
        log.println(">>>-----------------------------------------------");

        // write xdc
        assert result != null;
        Tool.write_XDC(result, print_line);
        print_line.close();

        return result;

    }




    /* Function: populate
       this function takes in placement strategy searched in a small region
       and replicate it across the whole chip
     */
    public static Map<Integer, List<Site[]>> populate(Map<Integer, List<Site[]>> original, String dev){
        Map<Integer, List<Site[]>> replicated = new HashMap<>(original);

        Utils U = new Utils(original, dev);
        Device device = U.getDevice();
        int[] height = U.getHeight();
        int num_repl = U.getURAMColHeight() / height[2];
        System.out.println("------- Replicate " + num_repl + " times");

        String[] prefix = new String[]{"DSP48E2_", "RAMB18_", "URAM288_"};
        int key = original.size() - 1;
        for (int i = 1; i < num_repl; i++){
            for (int blk = 0; blk < original.keySet().size(); blk++){
                List<Site[]> thisBlock = new ArrayList<>();
                for (int type = 0; type < 3; type++){
                    List<Site> thisType = new ArrayList<>();
                    for (int idx = 0; idx < original.get(blk).get(type).length; idx++){
                        Site org = original.get(blk).get(type)[idx];
                        thisType.add(device.getSite(prefix[type] + "X" + org.getInstanceX() +
                                "Y" + (i * height[type] + org.getInstanceY())));
                    }
                    thisBlock.add(thisType.toArray(new Site[0]));
                }
                key++;
                replicated.put(key, thisBlock);
            }
        }
        System.out.println("replication finished");
        return replicated;
    }

    public static Map<Integer, List<Site[]>> populateFixed(Map<Integer, List<Site[]>> original, String dev, int num_repl){
        Map<Integer, List<Site[]>> replicated = new HashMap<>(original);

        Utils U = new Utils(original, dev);
        Device device = U.getDevice();
        int[] height = U.getHeight();
        System.out.println("------- Replicate " + num_repl + " times");

        String[] prefix = new String[]{"DSP48E2_", "RAMB18_", "URAM288_"};
        int key = original.size() - 1;
        for (int i = 1; i < num_repl; i++){
            for (int blk = 0; blk < original.keySet().size(); blk++){
                List<Site[]> thisBlock = new ArrayList<>();
                for (int type = 0; type < 3; type++){
                    List<Site> thisType = new ArrayList<>();
                    for (int idx = 0; idx < original.get(blk).get(type).length; idx++){
                        Site org = original.get(blk).get(type)[idx];
                        thisType.add(device.getSite(prefix[type] + "X" + org.getInstanceX() +
                                "Y" + (i * height[type] + org.getInstanceY())));
                    }
                    thisBlock.add(thisType.toArray(new Site[0]));
                }
                key++;
                replicated.put(key, thisBlock);
            }
        }
        System.out.println("replication finished");
        return replicated;
    }


   public static void main(String[] args) throws IOException {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));

        // set up output log
        String logPath = System.getProperty("RAPIDWRIGHT_PATH") + "/log.txt";
        FileWriter fileWriter = new FileWriter(logPath);
        PrintWriter log = new PrintWriter(fileWriter, true); // enable auto-flush
        String timing_path = System.getProperty("RAPIDWRIGHT_PATH") + "/result/vivado_timing.txt"; // timing information path

       // set up device information
       String device = "xcvu13p";
       //String device = "xcvu37p";
       //String part = "xcvu37p-fsvh2892-3-e-es1"; // with high-band-width memory
       //String part = "xcvu11p-fsgd2104-3-e"; // without high-band-width memory
       String part = new Design("name", device).getPartName();

       // Switches
       final boolean optimization = true;
       final boolean rapidSynth = false;
       final boolean autoPipeline = true;
       final boolean matplotlib_visualize = false;
       final boolean finish_route = true;
       final boolean full_chip = true;

       final boolean vivado_verbose = true;

       // experiment config
       int blocknum = 80;
       //final int iteration = 30000;
       final int iteration = 1000000;
       boolean visualization = false;

       // optimization config
       String method = "CMA"; // SA, EA, EA-reduced, CMA
       int population = 5;
       int parents = 20;
       int children = 50;
       double crossoverR = 0.98;
       int x_min = 0;
       int x_max = 6000; // all columns
       int y_min = 0;
       int y_max = 240;

       final int mode = 7; // mode = 0: optimization

        switch (mode){
            case 0:
                /*  --- find placement solution --- */
                Map<Integer, List<Site[]>> result = new HashMap<>();
                String xdc_result  = System.getProperty("RAPIDWRIGHT_PATH") + "/result/blockNum="+  blocknum +".xdc";

                if (optimization)
                {
                    FileWriter fw = new FileWriter(xdc_result);
                    PrintWriter pw = new PrintWriter(fw, true);
                    result = find_solution(
                            method, blocknum, iteration, visualization, device,
                            population, parents, children, crossoverR,
                            x_min, x_max, y_min, y_max,
                            log);

                    Tool.write_XDC(result, pw);
                    pw.close();
                }
                else // use previous results
                    result = Tool.getMapFromXDC(xdc_result, device);
                System.out.println("Found Placement Strategy for " + result.size() + " blocks of convolution units");


                /* whole-chip placement */
                if (full_chip) {
                    result = populate(result, device);
                    blocknum = 480;
                }

                /* placement result visualization */
                if (matplotlib_visualize) {
                    Tool.matplot_visualize(xdc_result);
                }

                /* Synthesis */
                Design d;
                System.out.println("start synthesis...");
                if (rapidSynth)
                    d = Vivado.synthesize_with_seed(blocknum, device, part, false, vivado_verbose, log);
                else
                    d = Vivado.synthesize_vivado(blocknum, part, vivado_verbose, log);
                d.setAutoIOBuffers(false); // out of context mode
                System.out.println("synthesis finished");


                /* Placement */
                long start_time = System.nanoTime();
                System.out.println("Placement Start...");
                for (Integer index : result.keySet()) {
                    List<Site[]> blockConfig = result.get(index);
                    place_block(d, index, blockConfig);
                }
                System.out.println("Site-Routing ...");
                d.routeSites();
                long end_time = System.nanoTime();

                System.out.println(">>>-----------------------------------------------");
                final String s = "RapidWright Hard Block Placement time = " + (end_time - start_time) / 1e9
                        + " s, which is " + (end_time - start_time) / 1e9 / 60 + " min";
                System.out.println(s);
                System.out.println(">>>-----------------------------------------------");
                log.println(">>>-----------------------------------------------");
                log.println(s);
                log.println(">>>-----------------------------------------------");

                /* Auto Pipeline */
                if (autoPipeline) {
                    System.out.println(">>>------------ Auto-pipeline ----------<<<");
                    AutoPipeline.rapidsynth_autopipeline(d, result);
                }

                // output placed DCP file
                String placedDCPPath = System.getProperty("user.home") + "/RapidWright" + "/checkpoint/blockNum=" + blocknum + "_placed.dcp";
                File file = new File(placedDCPPath);
                if (file.exists())
                    file.delete();
                d.writeCheckpoint(placedDCPPath);

                // finish placement and routing with Vivado
                if (finish_route) {
                    double freq = Vivado.finishPlacementNRoute(placedDCPPath, blocknum, vivado_verbose, log);
                    System.out.println("$$$ TIMING RESULT : block num  = " + blocknum + "\t frequency = " + freq/1e6 + "MHz");
                    log.println("$$$ TIMING RESULT : block num  = " + blocknum + "\t frequency = " + freq/1e6 + " MHz");
                }

                break;


            case 1:
                /* this case is for auto-pipeline dev */

                // pre-synthesized design
                Design design = Vivado.synthesize_vivado(blocknum, part, false, log);
                // pre-searched regional solution
                Map<Integer, List<Site[]>> placement_partial = Tool.getMapFromXDC(System.getProperty("RAPIDWRIGHT_PATH") +
                        "/result/blockNum=80.xdc", "xcvu11p");
                // get whole-chip placement from regional solution
                //Map<Integer, List<Site[]>> placement = populate(placement_partial, device);
                Map <Integer, List<Site[]>> placement = placement_partial;

                // place design
                for (int i = 0; i < blocknum; i++) {
                    place_block(design, i, placement.get(i));
                    System.out.println("placed block = " + i);
                }
                design.routeSites();

                // auto pipeline after placement, let's see if it works
                AutoPipeline.AutoPipeline(design, placement);

                // write out pipelined design for debugging
                design.writeCheckpoint(System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/blockNum=480_pipelined.dcp");

                //design.writeCheckpoint(System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/blockNum=480_placed.dcp");

                break;

            case 2:

                System.out.println("--- In Case 5 --- ");

                /* Vivado experiment - no placement constraint */
                //Vivado.vivadoBaselineNoConstraints(480, part, log);

                /* Vivado experiment - manual floorplanning */
                Vivado.vivadoBaselineWithConstraints(480, part, log);

                break;

            case 3:
                String xdc_result2  = System.getProperty("RAPIDWRIGHT_PATH") + "/result/blockNum="+  blocknum +".xdc";
                FileWriter fw2 = new FileWriter(xdc_result2);
                PrintWriter pw2 = new PrintWriter(fw2);

                Map<Integer, List<Site[]>>    result2 = find_solution(
                            method, blocknum, iteration, visualization, device,
                            population, parents, children, crossoverR,
                            x_min, x_max, y_min, y_max,
                            log);

                Tool.write_XDC(result2, pw2);

                Tool.matplot_visualize(xdc_result2);

                break;
            case 4:

                blocknum = 80;
                population = 5;
                parents = 20;
                children = 40;

                Map<Integer, List<Site[]>> result4 = new HashMap<>();
                String xdc_result4  = System.getProperty("RAPIDWRIGHT_PATH") + "/result/blockNum="+  blocknum +".xdc";
                FileWriter fw4 = new FileWriter(xdc_result4);
                PrintWriter pw4 = new PrintWriter(fw4);

                result4 = find_solution(
                            "SA", blocknum, 1000000, true, device,
                            population, parents, children, crossoverR,
                            x_min, x_max, y_min, y_max,
                            log);
                Tool.write_XDC(result4, pw4);

                Tool.matplot_visualize(xdc_result4);


                break;

            case 5:

                blocknum = 80;
                population = 5;
                parents = 20;
                children = 40;
                visualization = false;
                int repeat = 100;

                /*// Annealing
                FileWriter sa_fw = new FileWriter(System.getProperty("RAPIDWRIGHT_PATH") + "/result/sa.txt");
                PrintWriter sa_pw = new PrintWriter(sa_fw, true);

                for (int i = 0; i < repeat; i++) {
                    long start = System.nanoTime();
                    Map<Integer, List<Site[]>> tmp_result =
                            find_solution(
                                    "SA", blocknum, 1000000, visualization, device,
                                    population, parents, children, crossoverR,
                                    x_min, x_max, y_min, y_max,
                                    log);
                    long end = System.nanoTime();
                    double seconds = (end - start) / 1e9;
                    Utils U = new Utils(tmp_result, device);
                    double spread = U.getMaxSpread();
                    double wirelength = U.getUnifiedWireLength();

                    sa_pw.println(seconds + " " + spread + " " + wirelength);
                    System.out.println("Annealing Result: time = " + seconds + " s, spread = " + spread + " , wirelength = " + wirelength);
                }

                sa_pw.close();
                sa_fw.close();

                // NSGA-II
                FileWriter ea_fw = new FileWriter(System.getProperty("RAPIDWRIGHT_PATH") + "/result/ea.txt");
                PrintWriter ea_pw = new PrintWriter(ea_fw, true);

                for (int i = 0; i < repeat; i++) {
                    long start = System.nanoTime();
                    Map<Integer, List<Site[]>> tmp_result =
                            find_solution(
                                    "EA", blocknum, 1000000, visualization, device,
                                    population, parents, children, crossoverR,
                                    x_min, x_max, y_min, y_max,
                                    log);
                    long end = System.nanoTime();
                    double seconds = (end - start) / 1e9;
                    Utils U = new Utils(tmp_result, device);
                    double spread = U.getMaxSpread();
                    double wirelength = U.getUnifiedWireLength();

                    ea_pw.println(seconds + " " + spread + " " + wirelength);
                    System.out.println("NSGA-II Result: time = " + seconds + " s, spread = " + spread + " , wirelength = " + wirelength);
                }

                ea_pw.close();
                ea_fw.close();*/

                // CMA-ES
                FileWriter cma_fw = new FileWriter(System.getProperty("RAPIDWRIGHT_PATH") + "/result/cma.txt");
                PrintWriter cma_pw = new PrintWriter(cma_fw, true);

                for (int i = 0; i < repeat; i++) {
                    long start = System.nanoTime();
                    Map<Integer, List<Site[]>> tmp_result =
                            find_solution(
                                    "CMA", blocknum, 1000000, visualization, device,
                                    population, parents, children, crossoverR,
                                    x_min, x_max, y_min, y_max,
                                    log);
                    long end = System.nanoTime();
                    double seconds = (end - start) / 1e9;
                    Utils U = new Utils(tmp_result, device);
                    double spread = U.getMaxSpread();
                    double wirelength = U.getUnifiedWireLength();

                    cma_pw.println(seconds + " " + spread + " " + wirelength);
                    System.out.println("CMA-ES Result: time = " + seconds + " s, spread = " + spread + " , wirelength = " + wirelength);
                }

                cma_pw.close();
                cma_fw.close();


            case 6:

                // plot-2 and evolution process visualization
                /*find_solution(
                        "SA", 80, 1000000, true, device,
                        population, parents, children, crossoverR,
                        x_min, x_max, y_min, y_max,
                        log);*/


                /*population = 5;
                parents = 20;
                children = 40;
                find_solution(
                        "EA", 80, 1000000, true, device,
                        population, parents, children, crossoverR,
                        x_min, x_max, y_min, y_max,
                        log);*/

                find_solution(
                        "CMA", 80, 100000000, true, device,
                        population, parents, children, crossoverR,
                        x_min, x_max, y_min, y_max, log
                );


                break;

            case 7:

                String path = System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/blockNum=480_pipelined.dcp";
                Vivado.finishPlacementNRoute(path , 480, true, log);

                break;

            default:
                break;

        }

        log.close();

    }



}
