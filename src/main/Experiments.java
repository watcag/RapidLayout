package main;

import Utils.Utils;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.ModuleInst;
import com.xilinx.rapidwright.design.Net;
import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.edif.EDIFCell;
import com.xilinx.rapidwright.edif.EDIFCellInst;
import com.xilinx.rapidwright.edif.EDIFLibrary;
import com.xilinx.rapidwright.design.Module;

import java.io.*;
import java.util.*;

import static main.Tool.*;

public class Experiments {


    public static double[] evaluate(String xdcPath, int blockNum) throws IOException {

        String device = "xcvu11p";

        Map<Integer, List<Site[]>> placement =  getMapFromXDCRobust(xdcPath, device, blockNum);
        Utils U = new Utils(placement, device);

        double[] report_wirelength = AutoPipeline.report_wireLengths(placement);

        double wirelength = U.getUnifiedWireLength();
        double size = U.getMaxBBoxSize();

        return new double[]{wirelength, size, report_wirelength[0], report_wirelength[1]};
    }

    public static void test_performance(String method) throws IOException {

        int repeat = 100;
        int blocknum = 80;
        int population = 5;
        int parents = 20;
        int children = 40;
        boolean visualization = false;

        String device = "xcvu11p";
        double crossoverR = 0.98;
        int x_min = 0;
        int x_max = 6000; // all columns
        int y_min = 0;
        int y_max = 240;

        String logPath = System.getProperty("RAPIDWRIGHT_PATH") + "/log" + method + ".txt";
        FileWriter fileWriter = new FileWriter(logPath);
        PrintWriter log = new PrintWriter(fileWriter, true); // enable auto-flush

        FileWriter ea_fw = new FileWriter(System.getProperty("RAPIDWRIGHT_PATH") + "/result/" + method + ".txt", true);
        PrintWriter ea_pw = new PrintWriter(ea_fw, true);

        for (int i = 0; i < repeat; i++) {
            String dirPath = System.getProperty("RAPIDWRIGHT_PATH") + "/result/" + method + "_performance";
            File xdcfile = new File(dirPath + "/" + i + ".xdc");

            if (xdcfile.exists()) continue;

            System.out.println("i = " + i);

            long start = System.nanoTime();
            Map<Integer, List<Site[]>> tmp_result =
                    AutoPlacement.find_solution(
                            method, blocknum, visualization, device,
                            population, parents, children, crossoverR,
                            x_min, x_max, y_min, y_max);
            long end = System.nanoTime();
            double seconds = (end - start) / 1e9;
            Utils U = new Utils(tmp_result, device);
            double size = U.getMaxBBoxSize();
            double wirelength = U.getUnifiedWireLength();

            ea_pw.println(seconds + " " + size + " " + wirelength);
            System.out.println( method + " Result: time = " + seconds + " s, size = " + size + " , wirelength = " + wirelength);

            // write XDC file to preserve all solution information
            File dir = new File(dirPath);
            if (!dir.exists())
                dir.mkdir();
            PrintWriter pw = new PrintWriter(new FileWriter(dirPath + "/" + i + ".xdc"), true);
            Tool.write_XDC(tmp_result, pw);
            pw.close();
        }

        ea_pw.close();
        ea_fw.close();
    }

    public static void transfer(String device) throws IOException {
        // idk why placement has something wrong
        String logPath = System.getProperty("RAPIDWRIGHT_PATH") + "/log.txt";
        FileWriter fileWriter = new FileWriter(logPath);
        PrintWriter log = new PrintWriter(fileWriter, true); // enable auto-flush
        String part = new Design("name", device).getPartName();

        // Make the right placement
        Map<Integer, List<Site[]>> placement = new HashMap<>();
        String cma = System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/pipeline_experiments/CMA-ES.xdc";
        Map<Integer, List<Site[]>> partial = Tool.getMapFromXDC(cma, device);
        placement = AutoPlacement.populate(partial, "xcvu11p");
        String xdc = System.getProperty("RAPIDWRIGHT_PATH") + "/result/blockNum=480.xdc";
        PrintWriter xdc_writer = new PrintWriter(new FileWriter(xdc));
        Tool.write_XDC(placement, xdc_writer);
        System.out.println("Printed xdc file: " + xdc);

        // Synthesis
        Design design = Vivado.synthesize_vivado(480, part, true);

        // Pipeline
        AutoPipeline.rapidsynth_autopipeline(design, placement);
        String pipelined = System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/blockNum=480_pipelined.dcp";
        design.writeCheckpoint(pipelined);

        // finish
        Vivado.implement_pipelined_design(pipelined, xdc, 480, true);
    }


    public static void transfer_13p() throws IOException {
        String device = "xcvu13p";
        String part = new Design("name", device).getPartName();

        // set up output log
        String logPath = System.getProperty("RAPIDWRIGHT_PATH") + "/log.txt";
        FileWriter fileWriter = new FileWriter(logPath);
        PrintWriter log = new PrintWriter(fileWriter, true);

        // optimization parameters, we shall use CMA-ES
        final boolean optimize = true;
        int blocknum = 80;
        double crossoverR = 0.98;
        int x_min = 0;
        int x_max = 6000; // all columns
        int y_min = 0;
        int y_max = 240;

        // optimization
        Map<Integer, List<Site[]>> result = new HashMap<>();
        String xdc_result = System.getProperty("RAPIDWRIGHT_PATH") + "/result/blockNum=" + blocknum + ".xdc";

        if (optimize) {
            FileWriter fw = new FileWriter(xdc_result);
            PrintWriter pw = new PrintWriter(fw, true);
            result = AutoPlacement.find_solution(
                    "CMA", blocknum, false, device,
                    0, 0, 0, crossoverR,
                    x_min, x_max, y_min, y_max);
            Tool.write_XDC(result, pw);
            pw.close();
        }
        else
            result = Tool.getMapFromXDC(xdc_result, device);
        result = AutoPlacement.populate(result, device);
        blocknum *= 8;

        // write populated XDC
        xdc_result = System.getProperty("RAPIDWRIGHT_PATH") + "/result/blockNum=" + blocknum + ".xdc";
        PrintWriter pr = new PrintWriter(new FileWriter(xdc_result), true);
        Tool.write_XDC(result, pr);
        pr.close();

        // synth
        Design d = Vivado.synthesize_vivado(blocknum, part, true);
        System.out.println("synthesis finished");

        // placement & site-routing
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
        final String s = "RapidWright Hard Block Placement time = " + (end_time - start_time) / 1e9
                + " s, which is " + (end_time - start_time) / 1e9 / 60 + " min";
        System.out.println(s);
        System.out.println(">>>-----------------------------------------------");
        log.println(">>>-----------------------------------------------");
        log.println(s);
        log.println(">>>-----------------------------------------------");

        /* Auto Pipeline */
        System.out.println(">>>------------ Auto-pipeline ----------<<<");
        AutoPipeline.rapidsynth_autopipeline(d, result);


        // output placed DCP file
        String placedDCPPath = System.getProperty("user.home") + "/RapidWright" + "/checkpoint/blockNum=" + blocknum + "_placed.dcp";
        File file = new File(placedDCPPath);
        if (file.exists())
            file.delete();
        d.writeCheckpoint(placedDCPPath);

        // finish route
        double freq = Vivado.finishPlacementNRoute(placedDCPPath, blocknum, true);
        System.out.println("$$$ TIMING RESULT : block num  = " + blocknum + "\t frequency = " + freq/1e6 + "MHz");
        log.println("$$$ TIMING RESULT : block num  = " + blocknum + "\t frequency = " + freq/1e6 + " MHz");


    }

    public static void collect_convergence_data() throws IOException {
        String logPath = System.getProperty("RAPIDWRIGHT_PATH") + "/log.txt";
        FileWriter fileWriter = new FileWriter(logPath);
        PrintWriter log = new PrintWriter(fileWriter, true); // enable auto-flush
        String device = "xcvu11p";
        // experiment config
        int blocknum = 80;
        boolean visualization = false;
        int population = 5;
        int parents = 20;
        int children = 50;
        double crossoverR = 0.98;
        int x_min = 0;
        int x_max = 6000; // all columns
        int y_min = 0;
        int y_max = 240;

        String[] methods = new String[]{"CMA", "EA", "SA", "EA-reduced"};
        for (String method : methods)
            for (int i = 0 ; i < 10; i++)
                main.AutoPlacement.find_solution(method,blocknum,
                        visualization, device, population, parents, children, crossoverR,
                        x_min, x_max, y_min, y_max);

    }

    public static void collect_gif_data() throws IOException {
        String logPath = System.getProperty("RAPIDWRIGHT_PATH") + "/log.txt";
        FileWriter fileWriter = new FileWriter(logPath);
        PrintWriter log = new PrintWriter(fileWriter, true); // enable auto-flush
        String device = "xcvu11p";
        // experiment config
        int blocknum = 80;
        boolean visualization = false;
        int population = 5;
        int parents = 20;
        int children = 50;
        double crossoverR = 0.98;
        int x_min = 0;
        int x_max = 6000; // all columns
        int y_min = 0;
        int y_max = 240;

        String[] methods = new String[]{"CMA"};
        for (String method : methods)
            main.AutoPlacement.find_solution(method,blocknum,
                        visualization, device, population, parents, children, crossoverR,
                        x_min, x_max, y_min, y_max);

    }

    public static void evaluate_placement_from_xdc() throws IOException {
        // evaluate placement quality
        String[] XDCpaths = {
                //"result/blockNum=1.xdc"
                //"result/CMA-ES.xdc",
                //"result/Annealing.xdc",
                //"result/NSGA-II.xdc",
                System.getProperty("RAPIDWRIGHT_PATH") + "/src/verilog/dsp_conv_chip.xdc"
        };

        for ( int i = 0; i < XDCpaths.length; i++) {
            String path = XDCpaths[i];
            double[] result = evaluate(path, 480);
            double wirelength = result[0];
            double size = result[1];

            double max_0 = result[2];
            double max_1 = result[3];

            System.out.println(path + " wirelength = " + wirelength + " size = " + size);
            System.out.println(path + " connection 1 = " + max_0 + " connection 2 = " + max_1);

        }
    }


    public static Design replicate(){


        long start_time = System.nanoTime();
        Design d = Design.readCheckpoint(System.getProperty("RAPIDWRIGHT_PATH")
                + "/checkpoint/blockNum=160_routed.dcp");

        Design design = new Design("new", d.getPartName());
        design.setAutoIOBuffers(false);
        //Net clk = design.createNet("clk");
        //EDIFCell top = design.getTopEDIFCell();

        Module module = new Module(d);
        //module.setNetlist(d.getNetlist());

        for (EDIFCell cell : d.getNetlist().getWorkLibrary().getCells()){
            //cell.rename("new_" + cell.getName());
            design.getNetlist().getWorkLibrary().addCell(cell);
        }

        EDIFLibrary hdi = design.getNetlist().getHDIPrimitivesLibrary();
        for (EDIFCell cell : d.getNetlist().getHDIPrimitivesLibrary().getCells()){
            if (!hdi.containsCell(cell))
                hdi.addCell(cell);
        }

        ArrayList<Site> allValidPlacement = module.calculateAllValidPlacements(d.getDevice());

        for(Site anchor : allValidPlacement){
            //EDIFCellInst ci  = top.createChildCellInst(allValidPlacement.indexOf(anchor) + "_module", module.getNetlist().getTopCell());
            ModuleInst mi = design.createModuleInst(allValidPlacement.indexOf(anchor) + "_moduleInst", module);
            design.addModuleInstNetlist(mi, module.getNetlist());
            //clk.getLogicalNet().createPortInst("clk", mi.getCellInst());
            //mi.setCellInst(ci);
            mi.place(anchor);
        }

        long end_time = System.nanoTime();

        System.out.println(">>>-----------------------------------------------");
        final String s = "SLR Replication time = " + (end_time - start_time) / 1e9
                + " s, which is " + (end_time - start_time) / 1e9 / 60 + " min";
        System.out.println(s);
        System.out.println(">>>-----------------------------------------------");

        design.writeCheckpoint(System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/replicated4SLR.dcp");

        return design;
    }





    public static void replicate_SLR() throws IOException {

        System.out.println("We do'in SLR replication");

        String device = "xcvu13p";
        String part = new Design("name", device).getPartName();

        // set up output log
        String logPath = System.getProperty("RAPIDWRIGHT_PATH") + "/log.txt";
        FileWriter fileWriter = new FileWriter(logPath);
        PrintWriter log = new PrintWriter(fileWriter, true);

        // optimization parameters, we shall use CMA-ES
        final boolean optimize = true;
        int blocknum = 80;
        double crossoverR = 0.98;
        int x_min = 0;
        int x_max = 6000; // all columns
        int y_min = 0;
        int y_max = 240;

        // optimization
        Map<Integer, List<Site[]>> result = new HashMap<>();
        String xdc_result = System.getProperty("RAPIDWRIGHT_PATH") + "/result/blockNum=" + blocknum + ".xdc";

        if (optimize) {
            FileWriter fw = new FileWriter(xdc_result);
            PrintWriter pw = new PrintWriter(fw, true);
            result = AutoPlacement.find_solution(
                    "CMA", blocknum,  false, device,
                    0, 0, 0, crossoverR,
                    x_min, x_max, y_min, y_max);
            Tool.write_XDC(result, pw);
            pw.close();
        }
        else
            result = Tool.getMapFromXDC(xdc_result, device);

        result = AutoPlacement.populateFixed(result, device, 2);
        blocknum *= 2;

        // write populated XDC
        xdc_result = System.getProperty("RAPIDWRIGHT_PATH") + "/result/blockNum=" + blocknum + ".xdc";
        PrintWriter pr = new PrintWriter(new FileWriter(xdc_result), true);
        Tool.write_XDC(result, pr);
        pr.close();

        // synth
        Design d = Vivado.synthesize_vivado(blocknum, part, true);
        System.out.println("synthesis finished");

        // placement & site-routing
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
        final String s = "RapidWright Hard Block Placement time = " + (end_time - start_time) / 1e9
                + " s, which is " + (end_time - start_time) / 1e9 / 60 + " min";
        System.out.println(s);
        System.out.println(">>>-----------------------------------------------");


        // output placed DCP file
        String placedDCPPath = System.getProperty("user.home") + "/RapidWright" + "/checkpoint/blockNum=" + blocknum + "_placed_13p.dcp";
        File file = new File(placedDCPPath);
        if (file.exists())
            file.delete();
        d.writeCheckpoint(placedDCPPath);

        // finish route
        double freq = Vivado.finishPlacementNRoute_2(placedDCPPath, blocknum, result, device,  true);
        System.out.println("$$$ TIMING RESULT : block num  = " + blocknum + "\t frequency = " + freq/1e6 + "MHz");
        log.println("$$$ TIMING RESULT : block num  = " + blocknum + "\t frequency = " + freq/1e6 + " MHz");

    }

    public static void clean_pipeline() throws IOException {
        String device = "xcvu11p";
        String part = new Design("name", device).getPartName();
        int blockn = 1;

        // set up output log
        String logPath = System.getProperty("RAPIDWRIGHT_PATH") + "/log.txt";
        FileWriter fileWriter = new FileWriter(logPath);
        PrintWriter log = new PrintWriter(fileWriter, true);

        Vivado.synthesize_vivado(blockn, part, true);
    }


    public static void manual_placement_timing() throws IOException {

        int pipeline_depth = 1;
        String device = "xcvu37p";
        //String part = new Design("name", device).getPartName();
        String part = "xcvu37p-fsvh2892-3-e-es1";
        int block_num = 480;

        PrintWriter log = new PrintWriter(new FileWriter(System.getProperty("RAPIDWRIGHT_PATH")
                + "/log_ppld" + pipeline_depth + ".txt"), true);

        String tcl_path = System.getProperty("RAPIDWRIGHT_PATH") + "/tcl/manual_" + pipeline_depth +  ".tcl";
        String output_path = System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/manual_pipeline_" + pipeline_depth + ".dcp";
        String verilog_path = System.getProperty("RAPIDWRIGHT_PATH") + "/src/verilog/";
        // write tcl script
        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("read_verilog " + verilog_path +"addr_gen.v " +
                    verilog_path + "dsp_conv.v "  + verilog_path + "dsp_conv_top.v " +
                    verilog_path + "dsp_conv_chip.sv");
            //printWriter.println("set_property generic Y=" + block_num + " [current_fileset]");
            //printWriter.println("set_property generic NUMBER_OF_REGISTER=" + pipeline_depth + " [current_fileset]");
            printWriter.println("synth_design -mode out_of_context -part "+ part +" -top dsp_conv_chip;");
            printWriter.println("create_clock -period 1.000 -waveform {0.000 0.500} [get_nets clk];");
            printWriter.println("read_xdc " + verilog_path + "dsp_conv_chip.xdc");
            printWriter.println("place_design; route_design; report_utilization; report_timing;");
            printWriter.println("write_checkpoint -force -file " + output_path);
            printWriter.println("exit");
            printWriter.close();
        }

        long start_time = System.nanoTime();
        String slack = Vivado.vivado_cmd("vivado -mode tcl -source " + tcl_path, true);
        long end_time = System.nanoTime();
        System.out.println(">>>-----------------------------------------------");
        System.out.println("Full Vivado Implementation time = " + (end_time-start_time)/1e9/60 + " min");
        System.out.println(">>>-----------------------------------------------");
        log.println(">>>-----------------------------------------------");
        log.println("Full Vivado Implementation time = " + (end_time-start_time)/1e9/60 + " min");
        log.println(">>>-----------------------------------------------");

        double violation = Double.parseDouble(slack.substring(slack.indexOf("-"), slack.indexOf("ns")));
        double clk_period = 1 - violation;
        double frequency = 1e9 / clk_period;

        System.out.println(">>>-----------------------------------------------");
        System.out.println("Pipeline Depth = " + pipeline_depth + ", Frequency = " + frequency / 1e6 + " MHz");
        System.out.println(">>>-----------------------------------------------");
    }

    public static void count_register() throws IOException {

        String dir = System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/FF/";
        File Dir = new File(dir);
        String[] checkpoints = Dir.list();
//        String[] checkpoints = new String[]{
//                System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/manual_pipeline_3.dcp"
//        };

        // write EDIF
        assert checkpoints != null;
//        for (String checkpoint : checkpoints) {
//            String name = checkpoint.split("\\.")[0];
//            if (name.startsWith("Annealing")) continue;
//            FileWriter fw = new FileWriter("/data/zhangniansong/RapidWright/tmp.tcl");
//            PrintWriter pw = new PrintWriter(fw);
//            pw.println("open_checkpoint " + dir + checkpoint + ";");
//            pw.println("write_edf " + dir + name + ".edf;");
//            pw.println("exit");
//            pw.close();
//            fw.close();
//            String cmd = "vivado -mode tcl -source /data/zhangniansong/RapidWright/tmp.tcl";
//            Vivado.vivado_cmd(cmd, true, new PrintWriter(new FileWriter("tmp.txt")));
//        }

        for (String checkpoint : checkpoints) {
            Design design = Design.readCheckpoint(dir + checkpoint);

            List<EDIFCellInst> cellInsts = design.getNetlist().getAllLeafCellInstances();

            int reg_num = 0;

            for (EDIFCellInst cellInst : cellInsts) {
                EDIFCell cellType = cellInst.getCellType();
                if (cellType.isPrimitive()) {
                    String name = cellType.getName();
                    if (name.equals("FDRE"))
                        reg_num++;
                }
            }

            System.out.println(checkpoint);
            System.out.println(" #FF  = " + reg_num);
        }
    }

    public static void readConfig() throws  IOException {
        // try to read config
        Properties prop = new Properties();
        String propFileName = System.getProperty("RAPIDWRIGHT_PATH") + "/config.properties";
        InputStream inputStream = new FileInputStream(propFileName);
        prop.load(inputStream);

        String xmin = prop.getProperty("xmin");
        String device = prop.getProperty("device");

        System.out.println("xmin = " + xmin + " device = " + device);
    }


    public static void main(String[] args) throws IOException {

        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));



        //test_performance("EA");

        //transfer("xcvu27p");

        //transfer_13p();

        //collect_convergence_data();

        //collect_gif_data();

        //replicate_SLR();

        //clean_pipeline();

        //manual_placement_timing();

        //replicate_SLR();

        //evaluate_placement_from_xdc();

        //replicate();

        //count_register();

//        String xdc_result = System.getProperty("RAPIDWRIGHT_PATH") + "/result/blockNum=80.xdc";
//        Map<Integer, List<Site[]>> result = Tool.getMapFromXDC(xdc_result, "xcvu11p");
//        result = AutoPlacement.populate(result, "xcvu11p");
//
//        FileWriter fw = new FileWriter(System.getProperty("RAPIDWRIGHT_PATH") + "/result/blockNum=480.xdc");
//        PrintWriter pw = new PrintWriter(fw, true);
//        Tool.write_XDC(result, pw);



        readConfig();

    }


}
