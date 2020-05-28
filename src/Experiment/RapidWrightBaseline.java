package Experiment;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.placer.blockplacer.BlockPlacer;
import main.Tool;
import main.Vivado;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class RapidWrightBaseline {

    static String checkpoint = System.getenv("RAPIDWRIGHT_PATH") + "/checkpoint/";
    static String tcl = System.getenv("RAPIDWRIGHT_PATH") + "/tcl/";

    public static Design implement_one_block(int block_num, String part, int depth, boolean verbose){
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
            printWriter.println("place_design; route_design;");
            printWriter.println("write_checkpoint -force -file " + output_path + ".dcp");
            printWriter.println("write_edif -force -file " + output_path + ".edf");
            printWriter.println("exit");
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        long start_time = System.nanoTime();
        Vivado.vivado_cmd("vivado -mode tcl -source " + tcl_path, verbose);
        long end_time = System.nanoTime();
        String s = "Synthesis - " + block_num + " conv blocks, time = " + (end_time-start_time)/1e9/60 + " min";
        System.out.println(">>>-----------------------------------------------");
        System.out.println(s);
        System.out.println(">>>-----------------------------------------------");

        return Design.readCheckpoint(output_path+".dcp");
    }


    public static void rwSAPlacer() throws IOException {

        // read config
        String device = "xcvu11p";
        String part = new Design("name", device).getPartName();

        // set up paths
        String root = System.getProperty("RAPIDWRIGHT_PATH") + "/";
        String checkpoint = root + "checkpoint/";

        // set up design parameters
        int blocknum = 1;
        int depth = 0;

        Design one = implement_one_block(blocknum, part, depth, true);

        Design d = Tool.replicateConvBlocks(one, 480);

        // Default parameters
        BlockPlacer blockPlacer = new BlockPlacer();

        System.out.println("number of hard macros: " + d.getModuleInsts().size());

        d = blockPlacer.placeDesign(d, true);

        d.writeCheckpoint(checkpoint + "rapidwright.dcp");
    }

    public static void main(String[] args) throws IOException {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));

        rwSAPlacer();
    }
}
