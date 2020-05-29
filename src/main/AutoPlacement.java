package main;

import Utils.Utility;
import cma.cmaes;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.Site;

import java.io.*;
import java.util.*;


public class AutoPlacement {

    // directories
    private static String root;
    private static String checkpoint;
    private static String results;


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
            case "GA":
            case "EA":

                Opt.StartOptimization opt = new Opt.StartOptimization();
                result = opt.main(
                        blockn, device, visual, method,
                        population, parents, children, crossoverR,
                        x_min, x_max, y_min, y_max);

                break;

            case "EA-REDUCED":

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
                System.out.println("Choose between EA, SA, EA-reduced, GA, CMA");
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

        Utility U = new Utility(original, dev);
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

    /* Function: populateFixed
       this function takes in placement solution searched in a small region and
       replicate it a fixed number of times, the number is specified with "num_repl"
    */
    public static Map<Integer, List<Site[]>> populateFixed(Map<Integer, List<Site[]>> original, String dev, int num_repl){
        Map<Integer, List<Site[]>> replicated = new HashMap<>(original);

        Utility U = new Utility(original, dev);
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

    /*
        Function: toolflow
        This is the entire toolflow as a demo. The configurations are loaded
        from ./config.properties
    */
    public static double[] toolflow() throws IOException {

        // read in configuration
        Properties prop = Tool.getProperties();
        String device = prop.getProperty("device");
        String part = new Design("name", device).getPartName();
        String placement = prop.getProperty("placement");

        // set up flags
        final boolean optimization = Boolean.parseBoolean(prop.getProperty("optimization"));
        final boolean rapidSynth = Boolean.parseBoolean(prop.getProperty("rapidSynth"));
        final boolean autoPipeline = Boolean.parseBoolean(prop.getProperty("autoPipeline"));
        final boolean vivado_verbose = Boolean.parseBoolean(prop.getProperty("vivado_verbose"));
        final boolean visualization = Boolean.parseBoolean(prop.getProperty("opt_visual"));
        final boolean generate_gif = Boolean.parseBoolean(prop.getProperty("generate_gif"));
        final int depth = Integer.parseInt(prop.getProperty("pipelineDepth"));
        boolean placement_exists = new File(placement).exists();

        // automatically calc block number, replication times, minimum replicate rectangle
        MinRect mr = new MinRect(device, 18, 8, 2); // hard block number in 1 computation unit
        int blocknum = mr.getBlocknum();
        int replication = mr.getReplication();
        int x_min = 0;    // these coordinates are RPM coordinates
        int x_max = 6000; // use all columns
        int y_min = 0;
        int y_max = mr.getYmax();

        // optimization parameters
        String method = prop.getProperty("method");
        int population = 5;
        int parents = 20;
        int children = 50;
        double crossoverR = 0.98;

        // set up gif data folder
        if (generate_gif) {
            String gif_folder = results + "demo_gif_data/";
            File gif_dir = new File(gif_folder);
            if (gif_dir.exists()) if (gif_dir.delete()) System.out.println("deleted old demo gif folder");
            if (gif_dir.mkdirs()) System.out.println("created dir " + gif_dir);
        }

        /* --- find placement solution --- */
        Map<Integer, List<Site[]>> result;
        String xdc_result = results + "blockNum=" + blocknum + ".xdc";
        boolean result_exists = new File(xdc_result).exists();
        double opt_secs = 0;

        if (optimization || (!result_exists && !placement_exists)) {
            // if we want to rerun optimization or there's no existing xdc files
            FileWriter fw = new FileWriter(xdc_result);
            PrintWriter pw = new PrintWriter(fw, true);
            long start = System.nanoTime();
            result = find_solution(
                    method, blocknum, visualization, device,
                    population, parents, children, crossoverR,
                    x_min, x_max, y_min, y_max);
            long end = System.nanoTime();
            opt_secs =  (end - start) / 1e9;
            Tool.write_XDC(result, pw);
            pw.close();
        } else if (placement_exists) {
            // else we use previous results stored in the xdc file
            result = Tool.getMapFromXDC(placement, device);
        } else {
            result = Tool.getMapFromXDC(xdc_result, device);
        }
        Utility U = new Utility(result, device);
        double bbox_size = U.getMaxBBoxSize();
        double wirelength = U.getUnifiedWireLength();
        System.out.println("[RapidLayout] Found Placement Strategy for " + result.size() + " blocks of convolution units");

        /* --- replicate placement to one SLR --- */
        result = AutoPlacement.populateFixed(result, device, replication);
        blocknum *= mr.getReplication();
        // generate a placement file for one SLR
        xdc_result = results + "blocknum=" + blocknum + ".xdc";
        PrintWriter pr = new PrintWriter(new FileWriter(xdc_result), true);
        Tool.write_XDC(result, pr);
        pr.close();

        /* --- synthesize one SLR --- */
        Design d = rapidSynth
                ? Vivado.synthesize_with_seed(blocknum, device, 0,  part, vivado_verbose)
                : Vivado.synthesize_vivado(blocknum, part, 0, vivado_verbose);
        System.out.println("[RapidLayout] One SLR synthesis finished.");

        /* --- placement and site-routing */
        long start_time = System.nanoTime();
        System.out.println("[RapidLayout] Placement Start...");
        for (Integer index : result.keySet()) {
            List<Site[]> blockConfig = result.get(index);
            AutoPlacement.place_block(d, index, blockConfig);
        }
        System.out.println("[RapidLayout] Site-Routing ...");
        d.routeSites();
        long end_time = System.nanoTime();

        System.out.println(">>>-----------------------------------------------<<<");
        String s = "RapidWright Hard Block Placement time = " + (end_time - start_time) / 1e9
                + " s, which is " + (end_time - start_time) / 1e9 / 60 + " min";
        System.out.println(s);
        System.out.println(">>>-----------------------------------------------<<<");

        /* --- pipeline --- */
        if (autoPipeline)
            AutoPipeline.auto_pipeline(d, blocknum, result);
        else
            AutoPipeline.fixed_pipeline(d, depth, blocknum);

        System.out.println("[RapidLayout] Pipelining finished.");

        /* --- finish SLR routing with Vivado --- */
        double impl_runtime = 0;
        String placedDCP = checkpoint + "blockNum=" + blocknum + "_placed.dcp";
        if (new File(placedDCP).delete()) System.out.println("[RapidLayout] deleted old placed dcp file.");
        d.writeCheckpoint(placedDCP);
        start_time = System.nanoTime();
        double freq = Vivado.finishPlacementNRoutePBlock(placedDCP, blocknum, result, device, vivado_verbose);
        end_time = System.nanoTime();
        impl_runtime += (end_time - start_time) / 1e9 / 60;
        System.out.println("[RapidLayout] clock frequency = " + freq / 1e6 + " MHz");

        /* --- SLR Replication --- */
        String routedSLR = checkpoint + "blockNum=" + blocknum + "_routed.dcp";
        start_time = System.nanoTime();
        Design full_chip_routed = Tool.replicateSLR(routedSLR);
        end_time = System.nanoTime();
        System.out.println(">>>-----------------------------------------------");
        s = "[RapidLayout] SLR Replication time = " + (end_time - start_time) / 1e9
                + " s, which is " + (end_time - start_time) / 1e9 / 60 + " min";
        System.out.println(s);
        System.out.println(">>>-----------------------------------------------");
        full_chip_routed.writeCheckpoint(checkpoint + "full-chip_" + device + ".dcp");
        System.out.println("$$$$ frequency =  " + freq/1e6 + " MHz");
        impl_runtime += (end_time - start_time) / 1e9 / 60;

        /* -- Generate GIF --- */
        if (generate_gif){
            String script = root + "src/visualize/draw_demo.py";
            Tool.execute_cmd("python3 " + script);
        }

        return new double[]{freq / 1e6, impl_runtime, opt_secs, bbox_size, wirelength};

    }


    public static void main(String[] args) throws IOException {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));

        root = System.getProperty("RAPIDWRIGHT_PATH") + "/";
        checkpoint = root + "checkpoint/";
        results = root + "result/";

        File ckpt = new File(checkpoint);
        File rslt = new File(results);
        if (ckpt.mkdirs()) System.out.println("checkpoint folder created");
        if (rslt.mkdirs())     System.out.println("result folder created");

        Tool.printParameters();
        toolflow();
    }



}
