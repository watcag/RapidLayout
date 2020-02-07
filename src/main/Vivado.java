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

    static String checkpoint = System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/";
    static String tcl = System.getProperty("RAPIDWRIGHT_PATH") + "/tcl/";
    static String result = System.getProperty("RAPIDWRIGHT_PATH") + "/result/";
    static String root = System.getProperty("RAPIDWRIGHT_PATH") + "/";

    public static Design synthesize_with_seed(int block_num, String device, String part, boolean save, boolean verbose) {
        long start_time = System.nanoTime();

        // synthesize seed first, if seed is not available
        String seed_path = checkpoint + "seed.dcp";
        File seed_file = new File(seed_path);
        if (!seed_file.exists())
            synthesize_seed(part, verbose);
        else{
            Design d = Design.readCheckpoint(seed_path);
            Design temp_d = new Design("temp", device);
            if (!d.getPartName().equals(temp_d.getPartName())) { // if the seed is not compatible
                System.out.println(">>>>WARNING<<<<  -- Seed's Device Part Name is different from requested, redo seed synthesis......");
                synthesize_seed(part, verbose);
            }
        }

        Design seed = Design.readCheckpoint(seed_path);

        // Replicate
        Design design = Tool.replicateConvBlocks(seed, block_num);

        long end_time = System.nanoTime();
        String s = "Synthesis - " + block_num + " conv blocks, time = " + (end_time-start_time)/1e9 + " s";
        System.out.println(">>>-----------------------------------------------");
        System.out.println(s);
        System.out.println(">>>-----------------------------------------------");

        if (save)
            design.writeCheckpoint(checkpoint + "blockNum="+block_num+"_synth.dcp");

        return design;
    }

    public static Design synthesize_vivado(int block_num, String part, int depth, boolean verbose){
        String tcl_path = tcl + "synth.tcl";
        String output_path = checkpoint + "blockNum=" + block_num;
        File checkpoint = new File(output_path+".dcp");

        if (checkpoint.exists())
            return Design.readCheckpoint(output_path+".dcp");

        // write tcl script
        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("read_verilog ../verilog/addr_gen.v ../verilog/dsp_conv.v ../verilog/dsp_conv_top.v ../verilog/dsp_conv_chip.sv");
            printWriter.println("set_property generic {NUMBER_OF_REG=" + depth + " Y="+block_num+"} [current_fileset]");
            printWriter.println("synth_design -mode out_of_context -part "+ part +" -top dsp_conv_chip;");
            printWriter.println("write_checkpoint -force -file " + output_path + ".dcp");
            printWriter.println("write_edif -force -file " + output_path + ".edf");
            printWriter.println("exit");
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        long start_time = System.nanoTime();
        vivado_cmd("vivado -mode tcl -source " + tcl_path, verbose);
        long end_time = System.nanoTime();
        String s = "Synthesis - " + block_num + " conv blocks, time = " + (end_time-start_time)/1e9/60 + " min";
        System.out.println(">>>-----------------------------------------------");
        System.out.println(s);
        System.out.println(">>>-----------------------------------------------");

        return Design.readCheckpoint(output_path+".dcp");
    }

    public static void synthesize_seed(String part, boolean verbose){
        String tcl_path = tcl + "synth_seed.tcl";
        String output_path = checkpoint + "seed";

        // write tcl script
        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("read_verilog ../src/verilog/addr_gen.v ../src/verilog/dsp_conv.v ../src/verilog/dsp_conv_top.v ../src/verilog/dsp_conv_chip.sv");
            printWriter.println("set_property generic {Y=" + 1 + "} [current_fileset]");
            //printWriter.println("set_property generic {NUMBER_OF_REG=" + depth + "} [current_fileset]");
            printWriter.println("synth_design -mode out_of_context -part "+ part +" -top dsp_conv_chip;");
            printWriter.println("write_checkpoint -force -file " + output_path + ".dcp");
            printWriter.println("write_edif -force -file " + output_path + ".edf");
            printWriter.println("exit");
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        long start_time = System.nanoTime();
        vivado_cmd("vivado -mode tcl -source " + tcl_path, verbose);
        long end_time = System.nanoTime();
        String s = "Synthesis - " + 1 + " conv blocks, time = " + (end_time-start_time)/1e9/60 + " min";
        System.out.println(">>>-----------------------------------------------");
        System.out.println(s);
        System.out.println(">>>-----------------------------------------------");

    }

    // Vivado-only implementation, given placement and block number
    public static double implementation(int block_num, String part, String device, Map<Integer, List<Site[]>> placement,
                                        boolean save, boolean verbose) throws IOException {
        String xdc_path = result + "blockNum=" + block_num + ".xdc";
        String tcl_path = tcl + "full_implementation.tcl";
        String output_path = checkpoint + "Vivado_routed_" + block_num + ".dcp";
        String verilog_path = root + "src/verilog/";
        // write tcl script
        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("read_verilog " + verilog_path +"addr_gen.v " +
                    verilog_path + "dsp_conv.v "  + verilog_path + "dsp_conv_top.v " +
                    verilog_path + "dsp_conv_chip.sv");
            printWriter.println("set_property generic Y=" + block_num + " [current_fileset]");
            File synth = new File(checkpoint + "blockNum=" + block_num + "_synth.dcp");
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
        String slack = vivado_cmd("vivado -mode tcl -source " + tcl_path, verbose);
        long end_time = System.nanoTime();
        System.out.println(">>>-----------------------------------------------");
        System.out.println("Full Vivado Implementation time = " + (end_time-start_time)/1e9/60 + " min");
        System.out.println(">>>-----------------------------------------------");

        double violation = Double.parseDouble(slack.substring(slack.indexOf("-"), slack.indexOf("ns")));
        double clk_period = 1 - violation;
        double frequency = 1e9 / clk_period;

        return frequency;
    }


    public static String vivado_cmd(String cmd, boolean verbose) {
        String command = "export PATH=$PATH:~/Vivado/2018.3/bin/;" + cmd;
        String vivado_path = System.getenv("VIVADO_PATH");
        if (vivado_path != null) {
            command = "export PATH:$PATH:" + vivado_path + ";" + cmd;
        }

        String slack = "";

        try {
            ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", command);
            builder.redirectErrorStream(true);
            builder.directory(new File(root + "src/main"));
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
                }
            }
            p.waitFor();
            r.close();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return slack;
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

    public static double finishPlacementNRoute(String placedDCP, int block_num, boolean verbose) throws IOException {
        String tcl_path = tcl + "finish_placement.tcl";
        String output_path = checkpoint + "blockNum=" + block_num + "_routed";

        // write tcl script
        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("open_checkpoint " + placedDCP);
            printWriter.println("create_clock -period 1.000 -waveform {0.000 0.500} [get_nets clk];");
            printWriter.println("place_design; route_design; report_timing");
            printWriter.println("write_checkpoint -force -file " + output_path + ".dcp");
            printWriter.println("write_edif -force -file " + output_path + ".edf");
            printWriter.println("exit");
            printWriter.close();
        }

        long start_time = System.nanoTime();
        String slack = Vivado.vivado_cmd("vivado -mode tcl -source " + tcl_path, verbose);
        long end_time = System.nanoTime();
        System.out.println(">>>-----------------------------------------------");
        System.out.println("Continued Placement and Route (Vivado) time = " + (end_time-start_time)/1e9/60 + " min");
        System.out.println(">>>-----------------------------------------------");


        double violation = Double.parseDouble(slack.substring(slack.indexOf("-"), slack.indexOf("ns")));
        double clk_period = 1 - violation;
        double frequency = 1e9 / clk_period;

        return frequency;
    }

    public static double finishPlacementNRoute_2(String placedDCP, int block_num,
                                                 Map<Integer, List<Site[]>> placement, String device,
                                                 boolean verbose) throws IOException {
        String tcl_path = tcl + "finish_placement.tcl";
        String output_path = checkpoint + "blockNum=" + block_num + "_routed.dcp";
        String output_edif = checkpoint + "blockNum=" + block_num + "_routed.edf";
        // write tcl script
        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("open_checkpoint " + placedDCP);
            //printWriter.println("create_clock -period 1.000 -waveform {0.000 0.500} [get_nets clk];");
            PBlockConstraint(printWriter, placement, device);
            printWriter.println("place_design; route_design; report_timing");
            printWriter.println("write_checkpoint -force -file " + output_path);
            printWriter.println("write_edf -force -file " + output_edif);
            printWriter.println("exit");
            printWriter.close();
        }

        long start_time = System.nanoTime();
        String slack = Vivado.vivado_cmd("vivado -mode tcl -source " + tcl_path, verbose);
        long end_time = System.nanoTime();
        System.out.println(">>>-----------------------------------------------");
        System.out.println("Continued Placement and Route (Vivado) time = " + (end_time-start_time)/1e9/60 + " min");
        System.out.println(">>>-----------------------------------------------");

        double violation = Double.parseDouble(slack.substring(slack.indexOf("-"), slack.indexOf("ns")));
        double clk_period = 1 - violation;

        return 1e9 / clk_period;
    }

    public static double implement_pipelined_design(String pipelinedDCP, String xdc_path, int block_num, boolean verbose) throws IOException {
        String tcl_path = tcl + "finish_placement.tcl";
        String output_path = checkpoint + "blockNum=" + block_num + "_routed";
        // write tcl script
        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("open_checkpoint " + pipelinedDCP);
            printWriter.println("read_xdc " + xdc_path);
            printWriter.println("create_clock -period 1.000 -waveform {0.000 0.500} [get_nets clk];");
            printWriter.println("place_design; route_design; report_timing");
            printWriter.println("write_checkpoint -force -file " + output_path + ".dcp");
            printWriter.println("write_edif -force -file " + output_path + ".edf");
            printWriter.println("exit");
            printWriter.close();
        }

        long start_time = System.nanoTime();
        String slack = Vivado.vivado_cmd("vivado -mode tcl -source " + tcl_path, verbose);
        long end_time = System.nanoTime();
        System.out.println(">>>-----------------------------------------------");
        System.out.println("Continued Placement and Route (Vivado) time = " + (end_time-start_time)/1e9/60 + " min");
        System.out.println(">>>-----------------------------------------------");

        double violation = Double.parseDouble(slack.substring(slack.indexOf("-"), slack.indexOf("ns")));
        double clk_period = 1 - violation;
        double frequency = 1e9 / clk_period;

        System.out.println("$$$$ frequency =  " + frequency/1e6 + " MHz");

        return frequency;
    }

    public static double post_impl_retiming(String implementedDesign) throws IOException {
        String tcl_path = tcl + "retiming.tcl";
        // write tcl script
        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("open_checkpoint " + implementedDesign);
            printWriter.println("create_clock -period 1.000 -waveform {0.000 0.500} [get_nets clk];");
            printWriter.println("report_timing");
            printWriter.println("exit");
            printWriter.close();
        }

        String slack = Vivado.vivado_cmd("vivado -mode tcl -source " + tcl_path, true);

        double violation = Double.parseDouble(slack.substring(slack.indexOf("-"), slack.indexOf("ns")));
        double clk_period = 1 - violation;
        double frequency = 1e9 / clk_period;

        System.out.println("$$$$ frequency =  " + frequency/1e6 + " MHz");

        return frequency;
    }

    public static Design legalize_process(Design d) throws IOException {
        String temp_dcp = System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/temp.dcp";
        String new_dcp  = System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/temp2.dcp";
        String new_edif = System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/temp2.edf";
        d.writeCheckpoint(temp_dcp);

        // vivado: read in and write out the temp dcp
        String tclFile = System.getProperty("RAPIDWRIGHT_PATH") + "/tcl/temp.tcl";
        PrintWriter tcl = new PrintWriter(new FileWriter(tclFile), true);
        tcl.println("open_checkpoint " + temp_dcp);
        tcl.println("write_checkpoint -force -file " + new_dcp);
        tcl.println("write_edif -force -file " + new_edif);
        tcl.println("exit");
        tcl.close();

        Vivado.vivado_cmd("vivado -mode tcl -source " + tclFile, true);

        d = Design.readCheckpoint(new_dcp);
        return d;
    }
}
