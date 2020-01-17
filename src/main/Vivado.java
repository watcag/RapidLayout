package main;

import Utils.Utils;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.device.ClockRegion;
import com.xilinx.rapidwright.device.Site;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Vivado {
    public static Design synthesize_with_seed(int block_num, String device, String part, boolean save, boolean verbose, PrintWriter log) {
        long start_time = System.nanoTime();

        // synthesize seed first, if seed is not available
        String seed_path = System.getProperty("user.home") + "/RapidWright/checkpoint/seed.dcp";
        File seed_file = new File(seed_path);
        if (!seed_file.exists())
            synthesize_seed(part, verbose, log);
        else{
            Design d = Design.readCheckpoint(seed_path);
            Design temp_d = new Design("temp", device);
            if (!d.getPartName().equals(temp_d.getPartName())) { // if the seed is not compatible
                System.out.println(">>>>WARNING<<<<  -- Seed's Device Part Name is different from requested, redo seed synthesis......");
                synthesize_seed(part, verbose, log);
            }
        }

        Design seed = Design.readCheckpoint(seed_path);
        Design design = Tool.replicateConvBlocks(seed, block_num);


        long end_time = System.nanoTime();
        String s = "Synthesis - " + block_num + " conv blocks, time = " + (end_time-start_time)/1e9 + " s";
        System.out.println(">>>-----------------------------------------------");
        System.out.println(s);
        System.out.println(">>>-----------------------------------------------");
        log.println(">>>-----------------------------------------------");
        log.println(s);
        log.println(">>>-----------------------------------------------");

        if (save)
            design.writeCheckpoint(System.getProperty("user.home")+"/RapidWright/checkpoint/blockNum="+block_num+"_synth.dcp");

        return design;
    }

    public static Design synthesize_vivado(int block_num, String part, boolean verbose, PrintWriter log){
        String tcl_path = System.getProperty("user.home") + "/RapidWright/tcl/synth.tcl";
        String output_path = System.getProperty("user.home") + "/RapidWright/checkpoint/blockNum=" + block_num;
        File checkpoint = new File(output_path+".dcp");

        if (checkpoint.exists())
            return Design.readCheckpoint(output_path+".dcp");

        // write tcl script
        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("read_verilog ../verilog/addr_gen.v ../verilog/dsp_conv.v ../verilog/dsp_conv_top.v ../verilog/dsp_conv_chip.sv");
            printWriter.println("set_property generic Y=" + block_num + " [current_fileset]");
            printWriter.println("synth_design -mode out_of_context -part "+ part +" -top dsp_conv_chip;");
            printWriter.println("write_checkpoint -force -file " + output_path + ".dcp");
            printWriter.println("write_edif -force -file " + output_path + ".edf");
            printWriter.println("exit");
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        long start_time = System.nanoTime();
        vivado_cmd("vivado -mode tcl -source " + tcl_path, verbose, log);
        long end_time = System.nanoTime();
        String s = "Synthesis - " + block_num + " conv blocks, time = " + (end_time-start_time)/1e9/60 + " min";
        System.out.println(">>>-----------------------------------------------");
        System.out.println(s);
        System.out.println(">>>-----------------------------------------------");
        log.println(">>>-----------------------------------------------");
        log.println(s);
        log.println(">>>-----------------------------------------------");

        return Design.readCheckpoint(output_path+".dcp");
    }

    public static void synthesize_seed(String part, boolean verbose, PrintWriter log){
        String tcl_path = System.getProperty("user.home") + "/RapidWright" + "/tcl/synth_seed.tcl";
        String output_path = System.getProperty("user.home") + "/RapidWright/checkpoint/seed.dcp";

        // write tcl script
        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("read_verilog ../src/verilog/addr_gen.v ../src/verilog/dsp_conv.v ../src/verilog/dsp_conv_top.v ../src/verilog/dsp_conv_chip.sv");
            printWriter.println("set_property generic Y=" + 1 + " [current_fileset]");
            printWriter.println("synth_design -mode out_of_context -part "+ part +" -top dsp_conv_chip;");
            printWriter.println("write_checkpoint -force -file " + output_path);
            printWriter.println("exit");
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        long start_time = System.nanoTime();
        vivado_cmd("vivado -mode tcl -source " + tcl_path, verbose, log);
        long end_time = System.nanoTime();
        String s = "Synthesis - " + 1 + " conv blocks, time = " + (end_time-start_time)/1e9/60 + " min";
        System.out.println(">>>-----------------------------------------------");
        System.out.println(s);
        System.out.println(">>>-----------------------------------------------");
        log.println(">>>-----------------------------------------------");
        log.println(s);
        log.println(">>>-----------------------------------------------");

    }

    public static double implementation(int block_num, String part, String device, Map<Integer, List<Site[]>> placement,
                                        boolean save, boolean verbose, PrintWriter log) throws IOException {
        String xdc_path = System.getProperty("RAPIDWRIGHT_PATH") + "/result/blockNum=" + block_num + ".xdc";
        String tcl_path = System.getProperty("RAPIDWRIGHT_PATH") + "/tcl/full_implementation.tcl";
        String output_path = System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/Vivado_routed_" + block_num + ".dcp";
        String verilog_path = System.getProperty("RAPIDWRIGHT_PATH") + "/src/verilog/";
        // write tcl script
        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("read_verilog " + verilog_path +"addr_gen.v " +
                    verilog_path + "dsp_conv.v "  + verilog_path + "dsp_conv_top.v " +
                    verilog_path + "dsp_conv_chip.sv");
            printWriter.println("set_property generic Y=" + block_num + " [current_fileset]");
            File synth = new File(System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/blockNum=" + block_num + "_synth.dcp");
            if (synth.exists())
                printWriter.println("open_checkpoint " + synth.getAbsolutePath());
            else
                printWriter.println("synth_design -mode out_of_context -part "+ part +" -top dsp_conv_chip;");
            printWriter.println("create_clock -period 1.000 -waveform {0.000 0.500} [get_nets clk];");
            PBlockConstraint(printWriter, placement, device);
            printWriter.println("read_xdc " + xdc_path);
            printWriter.println("place_design; route_design; report_utilization; report_timing;");
            if (save)
                printWriter.println("write_checkpoint -force -file " + output_path);
            printWriter.println("exit");
            printWriter.close();
        }

        long start_time = System.nanoTime();
        String slack = vivado_cmd("vivado -mode tcl -source " + tcl_path, verbose, log);
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

        return frequency;
    }


    public static String vivado_cmd(String cmd, boolean verbose, PrintWriter log) {
        String command = "" +
                "export PATH=$PATH:~/Vivado/2018.3/bin/;" +
                cmd;

        String slack = "";

        try {
            ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", command);
            Map<String, String> environment = builder.environment();
            //environment.forEach((key, value) -> System.out.println(key + value));
            //environment.put("","");
            builder.redirectErrorStream(true);
            builder.directory(new File(System.getProperty("user.home") + "/RapidWright" + "/src/main"));
            Process p = builder.start();

            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while (true) {
                buffer.append(line).append("\n");
                line = r.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith("Slack")) slack = line;
                if (verbose){
                    System.out.println(line);
                    log.println(line);
                }
            }
            p.waitFor();
            r.close();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return slack;
    }

    /* vivadoTiming(): special timing function for all .xdc result file in RapidWright/result folder
    *  if blockn == -1, it times all results in the result folder
    *  or else it only times the selected one
    * */
    public static void vivadoTiming(String timing_path, String part, String device, int blockn,
                                    boolean saveDCP, boolean verbose, PrintWriter log) throws IOException {
        String path = System.getProperty("user.home") + "/RapidWright/result";
        File dir = new File(path);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".xdc"));
        List<Integer> block_nums = new ArrayList<>();
        for (File f : files)
            block_nums.add(Integer.parseInt(f.getName().substring(f.getName().indexOf('=') + 1, f.getName().indexOf('.'))));
        Collections.sort(block_nums);

        try (FileWriter write = new FileWriter(timing_path)) {
            PrintWriter printWriter = new PrintWriter(write, true); // enable auto flush
            for (Integer n : block_nums) {
                if (blockn == -1){ // set block n to -1 to time all results, or else only time block number = blockn
                    Map<Integer, List<Site[]>> placement = Tool.getMapFromXDC(path + "/blockNum="+n+".xdc", device);
                    double freq = implementation(n, part, device, placement, saveDCP, verbose, log);
                    printWriter.println(freq);
                    System.out.println("$$$ TIMING RESULT : block num  = " + n + "\tfrequency = " + freq);
                    log.println("$$$ TIMING RESULT : block num  = " + n + "\tfrequency = " + freq);
                } else {
                    if (n != blockn)
                        continue;
                    Map<Integer, List<Site[]>> placement = Tool.getMapFromXDC(path + "/blockNum="+n+".xdc", device);
                    double freq = implementation(n, part, device, placement, saveDCP, verbose, log);
                    printWriter.println(freq);
                    System.out.println("$$$ TIMING RESULT : block num  = " + n + "\tfrequency = " + freq);
                    log.println("$$$ TIMING RESULT : block num  = " + n + "\tfrequency = " + freq);
                }

            }
            printWriter.close();
        }
    }

    public static void PBlockConstraint(PrintWriter tcl, Map<Integer, List<Site[]>> placement, String dev) {
        tcl.println("startgroup");
        tcl.println("create_pblock {pblock_name.dut}");
        Utils utils = new Utils(placement, dev);
        ClockRegion[] r = utils.getClockRegionRange();
        tcl.println("resize_pblock {pblock_name.dut} -add " + "CLOCKREGION_" + r[0].getName() + ":" + "CLOCKREGION_" +  r[1].getName());
        tcl.println("add_cells_to_pblock {pblock_name.dut} -top");
        tcl.println("endgroup");
        tcl.println("set_property CONTAIN_ROUTING true [get_pblocks pblock_name.dut]");
    }

    public static double finishPlacementNRoute(String placedDCP, int block_num, boolean verbose, PrintWriter log) throws IOException {
        String tcl_path = System.getProperty("RAPIDWRIGHT_PATH") + "/tcl/finish_placement.tcl";
        String output_path = System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/blockNum=" + block_num + "_routed.dcp";
        // write tcl script
        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("open_checkpoint " + placedDCP);
            printWriter.println("create_clock -period 1.000 -waveform {0.000 0.500} [get_nets clk];");
            printWriter.println("place_design; route_design; report_timing");
            printWriter.println("write_checkpoint -force -file " + output_path);
            printWriter.println("exit");
            printWriter.close();
        }

        long start_time = System.nanoTime();
        String slack = Vivado.vivado_cmd("vivado -mode tcl -source " + tcl_path, verbose, log);
        long end_time = System.nanoTime();
        System.out.println(">>>-----------------------------------------------");
        System.out.println("Continued Placement and Route (Vivado) time = " + (end_time-start_time)/1e9/60 + " min");
        System.out.println(">>>-----------------------------------------------");
        log.println(">>>-----------------------------------------------");
        log.println("Continued Placement and Route (Vivado) time = " + (end_time-start_time)/1e9/60 + " min");
        log.println(">>>-----------------------------------------------");


        double violation = Double.parseDouble(slack.substring(slack.indexOf("-"), slack.indexOf("ns")));
        double clk_period = 1 - violation;
        double frequency = 1e9 / clk_period;

        return frequency;
    }

    public static double finishPlacementNRoute_2(String placedDCP, int block_num,
                                                 Map<Integer, List<Site[]>> placement, String device,
                                                 boolean verbose, PrintWriter log) throws IOException {
        String tcl_path = System.getProperty("RAPIDWRIGHT_PATH") + "/tcl/finish_placement.tcl";
        String output_path = System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/blockNum=" + block_num + "_routed.dcp";
        String output_edif = System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/blockNum=" + block_num + "_routed.edf";
        // write tcl script
        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("open_checkpoint " + placedDCP);
            printWriter.println("create_clock -period 1.000 -waveform {0.000 0.500} [get_nets clk];");
            PBlockConstraint(printWriter, placement, device);
            printWriter.println("place_design; route_design; report_timing");
            printWriter.println("write_checkpoint -force -file " + output_path);
            printWriter.println("write_edf " + output_edif);
            printWriter.println("exit");
            printWriter.close();
        }

        long start_time = System.nanoTime();
        String slack = Vivado.vivado_cmd("vivado -mode tcl -source " + tcl_path, verbose, log);
        long end_time = System.nanoTime();
        System.out.println(">>>-----------------------------------------------");
        System.out.println("Continued Placement and Route (Vivado) time = " + (end_time-start_time)/1e9/60 + " min");
        System.out.println(">>>-----------------------------------------------");
        log.println(">>>-----------------------------------------------");
        log.println("Continued Placement and Route (Vivado) time = " + (end_time-start_time)/1e9/60 + " min");
        log.println(">>>-----------------------------------------------");


        double violation = Double.parseDouble(slack.substring(slack.indexOf("-"), slack.indexOf("ns")));
        double clk_period = 1 - violation;
        double frequency = 1e9 / clk_period;

        return frequency;
    }

    public static void vivadoBaselineNoConstraints(int block_num, String part, PrintWriter log) throws IOException {
        String tcl_path = System.getProperty("RAPIDWRIGHT_PATH") + "/tcl/vivado_baseline_no_constraints.tcl";
        String dcp_path = System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/blockNum=" + block_num + "_vrouted_baseline.dcp";
        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("read_verilog ../verilog/addr_gen.v ../verilog/dsp_conv.v ../verilog/dsp_conv_top.v ../verilog/dsp_conv_chip.sv");
            printWriter.println("set_property generic Y=" + block_num + " [current_fileset]");
            printWriter.println("synth_design -mode out_of_context -part "+ part +" -top dsp_conv_chip;");
            //printWriter.println("create_clock -period 1.000 -waveform {0.000 0.500} [get_nets clk];");
            printWriter.println("place_design; opt_design; route_design;");
            printWriter.println("write_checkpoint -force -file " + dcp_path);
            printWriter.println("exit");
            printWriter.close();
        }

        long start_time = System.nanoTime();
        Vivado.vivado_cmd("vivado -mode tcl -source " + tcl_path, true, log);
        long end_time = System.nanoTime();
        System.out.println(">>>-----------------------------------------------");
        System.out.println("Vivado baseline (no constraints) time = " + (end_time-start_time)/1e9/60 + " min");
        System.out.println(">>>-----------------------------------------------");
        log.println(">>>-----------------------------------------------");
        log.println("Vivado baseline (no constraints) time =" + (end_time-start_time)/1e9/60 + " min");
        log.println(">>>-----------------------------------------------");
    }

    public static void vivadoBaselineSynth(int block_num, String part, PrintWriter log) throws IOException {
        String tcl_path = System.getProperty("RAPIDWRIGHT_PATH") + "/tcl/blockNum=" + block_num + "_synth.tcl";
        String dcp_path = System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/blockNum=" + block_num + "_synth.dcp";

        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("read_verilog ../verilog/addr_gen.v ../verilog/dsp_conv.v ../verilog/dsp_conv_top.v ../verilog/dsp_conv_chip.sv");
            printWriter.println("set_property generic Y=" + block_num + " [current_fileset]");
            printWriter.println("synth_design -mode out_of_context -part "+ part +" -top dsp_conv_chip;");
            printWriter.println("write_checkpoint -force -file " + dcp_path);
            printWriter.println("exit");
            printWriter.close();
        }

        Vivado.vivado_cmd("vivado -mode tcl -source " + tcl_path, true, log);
    }


    public static void vivadoBaselineWithConstraints(int block_num, String part, PrintWriter log) throws IOException {

        System.out.println("--- Vivado Baseline, With Constraints --- ");

        String tcl_path = System.getProperty("RAPIDWRIGHT_PATH") + "/tcl/vivado_baseline_with_constraints.tcl";
        String dcp_path = System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/blockNum=" + block_num + "_constr_baseline.dcp";
        String synthed_design = System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/blockNum=" + block_num + "_synth.dcp";

        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("read_verilog ../verilog/addr_gen.v ../verilog/dsp_conv.v ../verilog/dsp_conv_top.v ../verilog/dsp_conv_chip.sv");
            printWriter.println("set_property generic Y=" + block_num + " [current_fileset]");
            printWriter.println("synth_design -mode out_of_context -part "+ part +" -top dsp_conv_chip;");
            printWriter.println("create_clock -period 1.000 -waveform {0.000 0.500} [get_nets clk];");
            printWriter.println("read_xdc " + System.getProperty("RAPIDWRIGHT_PATH") + "/src/verilog/dsp_conv_chip.xdc");
            printWriter.println("opt_design; place_design; route_design;");
            printWriter.println("write_checkpoint -force -file " + dcp_path);
            printWriter.println("exit");
            printWriter.close();
        }

        long start_time = System.nanoTime();
        System.out.println("Starting Vivado, execute script: " + tcl_path );
        Vivado.vivado_cmd("vivado -mode tcl -source " + tcl_path, true, log);
        long end_time = System.nanoTime();
        System.out.println(">>>-----------------------------------------------");
        System.out.println("Vivado baseline (with constraints) time = " + (end_time-start_time)/1e9/60 + " min");
        System.out.println(">>>-----------------------------------------------");
        log.println(">>>-----------------------------------------------");
        log.println("Vivado baseline (with constraints) time =" + (end_time-start_time)/1e9/60 + " min");
        log.println(">>>-----------------------------------------------");
    }

    public static double implement_pipelined_design(String pipelinedDCP, String xdc_path, int block_num, boolean verbose, PrintWriter log) throws IOException {
        String tcl_path = System.getProperty("RAPIDWRIGHT_PATH") + "/tcl/finish_placement.tcl";
        String output_path = System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/blockNum=" + block_num + "_routed.dcp";
        // write tcl script
        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("open_checkpoint " + pipelinedDCP);
            printWriter.println("read_xdc " + xdc_path);
            printWriter.println("create_clock -period 1.000 -waveform {0.000 0.500} [get_nets clk];");
            printWriter.println("place_design; route_design; report_timing");
            printWriter.println("write_checkpoint -force -file " + output_path);
            printWriter.println("exit");
            printWriter.close();
        }

        long start_time = System.nanoTime();
        String slack = Vivado.vivado_cmd("vivado -mode tcl -source " + tcl_path, verbose, log);
        long end_time = System.nanoTime();
        System.out.println(">>>-----------------------------------------------");
        System.out.println("Continued Placement and Route (Vivado) time = " + (end_time-start_time)/1e9/60 + " min");
        System.out.println(">>>-----------------------------------------------");
        log.println(">>>-----------------------------------------------");
        log.println("Continued Placement and Route (Vivado) time = " + (end_time-start_time)/1e9/60 + " min");
        log.println(">>>-----------------------------------------------");


        double violation = Double.parseDouble(slack.substring(slack.indexOf("-"), slack.indexOf("ns")));
        double clk_period = 1 - violation;
        double frequency = 1e9 / clk_period;

        System.out.println("$$$$ frequency =  " + frequency/1e6 + " MHz");

        return frequency;
    }
}
