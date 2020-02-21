package Experiment;

import com.xilinx.rapidwright.design.Design;
import main.MinRect;
import main.Vivado;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Scanner;

public class ManualPlacement {

    public static void manual_placement_timing(String xdc_path, int depth, String device) throws IOException {
        System.out.println("--- Vivado manual placement ---");
        String part = new Design("name", device).getPartName();
        // automatically decide number of blocks
        MinRect mr = new MinRect(device, 18, 8, 2);
        int block_num = mr.getBlocknum();
        String tcl_path = System.getProperty("RAPIDWRIGHT_PATH") + "/tcl/manual_" + depth + ".tcl";
        String output_path = System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/manual_pipeline_" + depth + ".dcp";
        String verilog_path = System.getProperty("RAPIDWRIGHT_PATH") + "/src/verilog/";
        // write tcl script
        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("read_verilog " + verilog_path + "addr_gen.v " +
                    verilog_path + "dsp_conv.v " + verilog_path + "dsp_conv_top.v " +
                    verilog_path + "dsp_conv_chip.sv");
            printWriter.println("set_property generic {NUMBER_OF_REG=" + depth + " Y=" + block_num + "} [current_fileset]");
            printWriter.println("synth_design -mode out_of_context -part " + part + " -top dsp_conv_chip;");
            printWriter.println("create_clock -period 1.000 -waveform {0.000 0.500} [get_nets clk];");
            printWriter.println("read_xdc " + xdc_path);
            printWriter.println("place_design; route_design; report_timing;");
            printWriter.println("write_checkpoint -force -file " + output_path);
            printWriter.println("exit");
            printWriter.close();
        }

        long start_time = System.nanoTime();
        String slack = Vivado.vivado_cmd("vivado -mode tcl -source " + tcl_path, true);
        long end_time = System.nanoTime();
        System.out.println(">>>-----------------------------------------------");
        System.out.println("Full Vivado Implementation time = " + (end_time - start_time) / 1e9 / 60 + " min");
        System.out.println(">>>-----------------------------------------------");

        double violation = Double.parseDouble(slack.substring(slack.indexOf("-"), slack.indexOf("ns")));
        double clk_period = 1 - violation;
        double frequency = 1e9 / clk_period;

        System.out.println(">>>-----------------------------------------------");
        System.out.println("Pipeline Depth = " + depth + ", Frequency = " + frequency / 1e6 + " MHz");
        System.out.println(">>>-----------------------------------------------");
    }


    public static void main(String[] args) throws IOException {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));


        Scanner input = new Scanner(System.in);

        System.out.println("Please input device: (e.g. xcvu11p, xcvu13p, xcvu37p, etc.)");
        String device = input.next();
        // check if device is legal
        Design checkLegal = new Design("temp", device);

        int depth;
        do {
            System.out.println("Please input pipeline depth: (e.g. 1)");
            while (!input.hasNextInt()) {
                System.out.println("That's not a number!");
                input.next(); // this is important!
            }
            depth = input.nextInt();
        } while (depth <0 || depth > 3);

        // default xdc path
        String xdc = System.getenv("RAPIDWRIGHT_PATH") + "/src/verilog/dsp_conv_chip.xdc";

        manual_placement_timing(xdc, depth, device);

    }
}
