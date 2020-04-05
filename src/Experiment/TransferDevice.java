package Experiment;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.SLR;
import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.device.Tile;
import main.Tool;
import main.Vivado;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransferDevice {

    private static String tcl = System.getenv("RAPIDWRIGHT_PATH") + "/tcl/";
    private static String checkpoint = System.getenv("RAPIDWRIGHT_PATH") + "/checkpoint/";
    private static String root = System.getenv("RAPIDWRIGHT_PATH") + "/";


    public static boolean inSLR0(List<Site[]> ConvUnit, Device dev) {
        boolean isTrue = true;
        SLR slr0 = dev.getSLR(0);
        for(Site[] sites : ConvUnit) {
            for (Site s : sites) {
                Tile t = s.getTile();
                if (!slr0.containsTile(t))
                    isTrue = false;
            }
        }
        return isTrue;
    }

    public static double place_it(int block_num, String xdc_path,
                                                     Map<Integer, List<Site[]>> placement, String device,
                                                     boolean verbose) throws IOException {
        String tcl_path = tcl + "transfer_finish.tcl";
        String output_path = checkpoint + "blockNum=" + block_num + "_routed.dcp";
        String output_edif = checkpoint + "blockNum=" + block_num + "_routed.edf";
        String verilog_path = root + "src/verilog/";

        String part = new Design("name", device).getPartName();

        // write tcl script
        try (FileWriter write = new FileWriter(tcl_path)) {
            PrintWriter printWriter = new PrintWriter(write, true);
            printWriter.println("read_verilog " + verilog_path +"addr_gen.v " +
                    verilog_path + "dsp_conv.v "  + verilog_path + "dsp_conv_top.v " +
                    verilog_path + "dsp_conv_chip.sv");
            printWriter.println("set_property generic {NUMBER_OF_REG=4" + " Y=" + block_num + "} [current_fileset]");
            printWriter.println("synth_design -mode out_of_context -part "+ part +" -top dsp_conv_chip;");
            main.Vivado.PBlockConstraint(printWriter, placement, device);
            printWriter.println("create_clock -period 1.000 -waveform {0.000 0.500} [get_nets clk];");
            printWriter.println("read_xdc " + xdc_path);
            printWriter.println("place_design;");
            printWriter.println("route_design");
            printWriter.println("report_timing;");
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

        double frequency = 1e9 / clk_period;

        System.out.println("$$$$ frequency =  " + frequency / 1e6 + " MHz");


        return 1e9 / clk_period;
    }


    public static void transferLearning() throws IOException {

        String root = System.getenv("RAPIDWRIGHT_PATH") + "/";

        String device = "vu11p";
        Device dev = new Design("name", device).getDevice();
        String orig_xdc = root + "src/verilog/dsp_conv_chip.xdc";
        String orig_xdc_part = root + "result/opt.xdc";
        String adapted = root + "result/adapted.xdc";

        Map<Integer, List<Site[]>> orig_place = Tool.getMapFromXDCRobust(orig_xdc, device, 480);

        Map<Integer, List<Site[]>> inSLR = new HashMap<>();
        int i = 0;
        for (Integer key : orig_place.keySet()) {
            List<Site[]> convUnit = orig_place.get(key);
            boolean isInSLR = inSLR0(convUnit, dev);
            if (isInSLR){
                inSLR.put(i, convUnit);
                i += 1;
            }
        }

        System.out.println("num of blocks = " + inSLR.keySet().size());

        PrintWriter pw = new PrintWriter(new FileWriter(orig_xdc_part), true);
        Tool.write_XDC(inSLR, pw);
        pw.close();

        String visual2 = "python3 " + root + "src/visualize/overall_visual.py " + orig_xdc_part + " " + root + "result/visual/";

        Tool.execute_cmd(visual2);

        /* adapt manual placement with ours */


        int blockn = 160;
        int x_min=0; int y_min=0; int x_max=6000; int y_max=500;

        boolean visual = true;
        String method = "EA";
        int population = 5;
        int parents = 20;
        int children = 50;
        double crossoverR = 0.98;

        Tool.changeProperty("transfer", "true");
        Tool.changeProperty("initial_xdc", orig_xdc_part);

        Map<Integer, List<Site[]>> optPlace = Tool.getMapFromXDC(adapted, device);


        place_it(blockn, adapted, optPlace, device, true);

    }


    public static void main(String[] args) throws IOException {
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));
        transferLearning();

    }
}
