package main;

import Utils.Utils;
import cma.cmaes;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.Site;

import java.io.*;
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
            int blockn, boolean visual, String device,
            int population, int parents, int children, double crossoverR,
            int x_min, int x_max, int y_min, int y_max
    ) throws IOException {

        System.out.println("Searching for Placement Solution ...");

        // setup env
        String path = System.getProperty("RAPIDWRIGHT_PATH") + "/result/blockNum=" + blockn + ".xdc";
        FileWriter writer = new FileWriter(path);
        PrintWriter print_line = new PrintWriter(writer, true);
        Map<Integer, List<Site[]>> result = null;
        long start_time = System.nanoTime();

        switch (method.toUpperCase()) {

            case "SA":
            case "EA":

                Opt.StartOptimization opt = new Opt.StartOptimization();
                result = opt.main(
                        blockn, device, visual, method,
                        population, parents, children, crossoverR,
                        x_min, x_max, y_min, y_max);

                break;

            case "EA-REDUCED":
            case "SA-REDUCED":

                OptReduced.StartOptimization opt_r = new OptReduced.StartOptimization();
                result = opt_r.main(
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
        System.out.println("------- Replicate Placement " + num_repl + " times --------");

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
        System.out.println("------- Replicate Placement " + num_repl + " times --------");

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

    public static void flow_regular() throws IOException {
        // read config
        Properties prop = Tool.getProperties();
        String device = prop.getProperty("device");
        String part = new Design("name", device).getPartName();

        // Switches
        final boolean optimization = Boolean.parseBoolean(prop.getProperty("optimization"));
        final boolean rapidSynth = Boolean.parseBoolean(prop.getProperty("rapidSynth"));
        final boolean autoPipeline = Boolean.parseBoolean(prop.getProperty("autoPipeline"));
        final boolean matplotlib_visualize = Boolean.parseBoolean(prop.getProperty("matplotlib_visual"));
        final boolean vivado_verbose = Boolean.parseBoolean(prop.getProperty("vivado_verbose"));
        final boolean visualization = Boolean.parseBoolean(prop.getProperty("opt_visual"));

        // experiment config
        int blocknum = 80; // TODO: automatically determine blocknum
        String method = prop.getProperty("method");

        // optimization parameters
        int population = 5;
        int parents = 20;
        int children = 50;
        double crossoverR = 0.98;
        int x_min = 0;
        int x_max = 6000; // all columns
        int y_min = 0;
        int y_max = 240; // TODO: automatically determine min replicating rectangle

        // set up paths
        String root = System.getProperty("RAPIDWRIGHT_PATH") + "/";
        String checkpoint = root + "checkpoint/";
        String results = root + "result/";


        /*  --- find placement solution --- */
        Map<Integer, List<Site[]>> result = new HashMap<>();
        String xdc_result = results + "blockNum=" + blocknum + ".xdc";

        if (optimization)
        {
            FileWriter fw = new FileWriter(xdc_result);
            PrintWriter pw = new PrintWriter(fw, true);
            result = find_solution(
                    method, blocknum, visualization, device,
                    population, parents, children, crossoverR,
                    x_min, x_max, y_min, y_max);

            Tool.write_XDC(result, pw);
            pw.close();
        }
        else // use previous results
            result = Tool.getMapFromXDC(xdc_result, device);
        System.out.println("Found Placement Strategy for " + result.size() + " blocks of convolution units");


        /* generate full-chip placement */
        result = populate(result, device);
        blocknum = 480;


        /* placement result visualization */
        if (matplotlib_visualize) {
            Tool.matplot_visualize(xdc_result);
        }

        /* Synthesis */
        Design d;
        System.out.println("start synthesis...");
        if (rapidSynth)
            d = Vivado.synthesize_with_seed(blocknum, device, part, false, vivado_verbose);
        else
            d = Vivado.synthesize_vivado(blocknum, part, 0, vivado_verbose);
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

        /* Auto Pipeline */
        if (autoPipeline) {
            System.out.println(">>>------------ Auto-pipeline ----------<<<");
            AutoPipeline.rapidsynth_autopipeline(d, result);
        }

        // output placed DCP file
        String placedDCPPath = checkpoint + "blockNum=" + blocknum + "_placed.dcp";
        File file = new File(placedDCPPath);
        if (file.exists())
            file.delete();
        d.writeCheckpoint(placedDCPPath);

        // finish placement and routing with Vivado
        double freq = Vivado.finishPlacementNRoute(placedDCPPath, blocknum, vivado_verbose);
        System.out.println("$$$ TIMING RESULT : block num  = " + blocknum + "\t frequency = " + freq/1e6 + "MHz");

    }

    public static void flow_SLR() throws IOException {
        // read config
        Properties prop = Tool.getProperties();
        String device = prop.getProperty("device");
        String part = new Design("name", device).getPartName();

        // Switches
        //final boolean optimization = Boolean.parseBoolean(prop.getProperty("optimization"));
        boolean optimization = true;
        final boolean rapidSynth = Boolean.parseBoolean(prop.getProperty("rapidSynth"));
        final boolean autoPipeline = Boolean.parseBoolean(prop.getProperty("autoPipeline"));
        final boolean matplotlib_visualize = Boolean.parseBoolean(prop.getProperty("matplotlib_visual"));
        final boolean vivado_verbose = Boolean.parseBoolean(prop.getProperty("vivado_verbose"));
        final boolean visualization = Boolean.parseBoolean(prop.getProperty("opt_visual"));
        final int depth = Integer.parseInt(prop.getProperty("pipelineDepth"));

        // experiment config
        int blocknum = 1; // TODO: automatically determine blocknum
        String method = prop.getProperty("method");

        // optimization parameters
        int population = 5;
        int parents = 20;
        int children = 50;
        double crossoverR = 0.98;
        int x_min = 0;
        int x_max = 6000; // all columns
        int y_min = 0;
        int y_max = 240; // TODO: automatically determine min replicating rectangle

        // set up paths
        String root = System.getProperty("RAPIDWRIGHT_PATH") + "/";
        String checkpoint = root + "checkpoint/";
        String results = root + "result/";


        /*  --- find placement solution --- */
        Map<Integer, List<Site[]>> result = new HashMap<>();
        String xdc_result = results + "blockNum=" + blocknum + ".xdc";

        if (optimization)
        {
            FileWriter fw = new FileWriter(xdc_result);
            PrintWriter pw = new PrintWriter(fw, true);
            result = find_solution(
                    method, blocknum, visualization, device,
                    population, parents, children, crossoverR,
                    x_min, x_max, y_min, y_max);

            Tool.write_XDC(result, pw);
            pw.close();
        }
        else // use previous results
            result = Tool.getMapFromXDC(xdc_result, device);
        System.out.println("Found Placement Strategy for " + result.size() + " blocks of convolution units");

        if (matplotlib_visualize)
            Tool.matplot_visualize(xdc_result);

        /* replicate placement to one SLR */
//        result = AutoPlacement.populateFixed(result, device, 2);
//        blocknum *= 2;
//        xdc_result = results + "blockNum=" + blocknum + ".xdc";
//        PrintWriter pr = new PrintWriter(new FileWriter(xdc_result), true);
//        Tool.write_XDC(result, pr);
//        pr.close();

        /* synthesize one SLR */
        Design d = Vivado.synthesize_vivado(blocknum, part, 0, vivado_verbose);
        System.out.println("One SLR synthesis finished.");

        /* placement and site-routing */
        long start_time = System.nanoTime();
        System.out.println("Placement Start...");
        for (Integer index : result.keySet()) {
            List<Site[]> blockConfig = result.get(index);
            AutoPlacement.place_block(d, index, blockConfig);
        }
        System.out.println("Site-Routing ...");
        d.routeSites();
        long end_time = System.nanoTime();
        System.out.println(">>>-----------------------------------------------");
        String s = "RapidWright Hard Block Placement time = " + (end_time - start_time) / 1e9
                + " s, which is " + (end_time - start_time) / 1e9 / 60 + " min";
        System.out.println(s);
        System.out.println(">>>-----------------------------------------------");

        d = Vivado.legalize_process(d);

        /* pipelining */
        AutoPipeline.fixed_pipeline(d, depth, blocknum);

        /* write out checkpoint, finish routing with Vivado */
        String placedDCPPath = checkpoint + "blockNum=" + blocknum + "_placed.dcp";
        File file = new File(placedDCPPath);
        if (file.exists())
            file.delete();
        d.writeCheckpoint(placedDCPPath);
        double freq = Vivado.finishPlacementNRoute_2(placedDCPPath, blocknum, result, device, vivado_verbose);

        /* read in routed SLR and replicate */
        String routedSLR = checkpoint + "blockNum=" + blocknum + "_routed.dcp";
        start_time = System.nanoTime();
        Design full_chip_routed = Tool.replicateSLR(routedSLR);
        end_time = System.nanoTime();
        System.out.println(">>>-----------------------------------------------");
        s = "SLR Replication time = " + (end_time - start_time) / 1e9
                + " s, which is " + (end_time - start_time) / 1e9 / 60 + " min";
        System.out.println(s);
        System.out.println(">>>-----------------------------------------------");
        full_chip_routed.flattenDesign();
        full_chip_routed.writeCheckpoint(checkpoint + "full-chip_" + device + ".dcp");

        /* report clock frequency */
        System.out.println("$$$$ frequency =  " + freq/1e6 + " MHz");
    }

    public static void flow_SLR_debug() throws IOException {
        // read config
        Properties prop = Tool.getProperties();
        String device = prop.getProperty("device");
        String part = new Design("name", device).getPartName();
        int blocknum = 160;

        String checkpoint = System.getenv("RAPIDWRIGHT_PATH") + "/checkpoint/";

        /* read in routed SLR and replicate */
        String routedSLR = checkpoint + "blockNum=" + blocknum + "_routed.dcp";
        long start_time = System.nanoTime();
        Design full_chip_routed = Tool.replicateSLR(routedSLR);
        long end_time = System.nanoTime();
        System.out.println(">>>-----------------------------------------------");
        String s = "SLR Replication time = " + (end_time - start_time) / 1e9
                + " s, which is " + (end_time - start_time) / 1e9 / 60 + " min";
        System.out.println(s);
        System.out.println(">>>-----------------------------------------------");
        //full_chip_routed.flattenDesign();
        full_chip_routed.writeCheckpoint(checkpoint + "full-chip_" + device + ".dcp");

        /* report clock frequency */
        //System.out.println("$$$$ frequency =  " + freq/1e6 + " MHz");
        Vivado.post_impl_retiming(checkpoint + "full-chip_" + device + ".dcp");
    }


    public static void main(String[] args) throws IOException {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));

        File checkpoint = new File(System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint");
        File result = new File(System.getProperty("RAPIDWRIGHT_PATH") + "/result");
        if (checkpoint.mkdirs()) System.out.println("checkpoint folder created");
        if (result.mkdirs())     System.out.println("result folder created");


        Tool.printParameters();
        Properties prop = Tool.getProperties();
        final boolean useSLR = Boolean.parseBoolean(prop.getProperty("SLRCopy"));
        if (useSLR)
            flow_SLR();
        else
            flow_regular();
    }



}
