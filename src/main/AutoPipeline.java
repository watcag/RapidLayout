package main;

import com.xilinx.rapidwright.design.*;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.edif.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class AutoPipeline {

    private static double distance(Site a, Site b) {
        return a.getTile().getManhattanDistance(b.getTile());
    }

    private static double[] getMaxDistances(List<double[]> wirelengths) {
        List<List<Double>> matrix = new ArrayList<>();
        List<Double> max = new ArrayList<>();
        for (int i = 0; i < wirelengths.get(0).length; i++)
            matrix.add(new ArrayList<>()); // add a column
        for (double[] wirelength : wirelengths) {
            for (int j = 0; j < wirelength.length; j++) {
                matrix.get(j).add(wirelength[j]);
            }
        }

        for (List<Double> col : matrix) {
            max.add(Collections.max(col));
        }

        return new double[]{max.get(0), max.get(1), max.get(2), max.get(3), max.get(4)};
    }


    /*
     * Insertion Positions:
     * 1. uram_inst_wr[URAM] -> option_3.uram_rd_data_r2_reg[31:0]_srl2 -> option_3.uram_rd_data_r_reg[31:0]
     * 2. RAMB_rdc1 -> dsp1   RAMB_rdc2 -> dsp4   RAMB_rdc3 -> dsp7
     * 3. RAMB_rdc4 -> dsp1
     *
     * Let's try to do auto-pipelining for URAM-BRAM first. And because uram_rd (the output uram) has so many registers
     * already, I'll just insert registers between uram_wr and block rams, which is exactly what we have done in the
     * hand-made pipeline.
     */

    /*
     * Function: report_wireLengths, return which name[i].dut needs to be pipelined. And I need to know which location
     * needs to be pipelined
     */
    public static List<double[]> report_wireLengths(Map<Integer, List<Site[]>> placement) throws IOException {

        int blockn = placement.size();

        /* debug: write out wirelengths */
        String folder = System.getenv("RAPIDWRIGHT_PATH") + "/result/report_wirelengths/";
        File f = new File(folder);
        if (f.mkdirs())
            System.out.println("folder " + folder + " is created.");
        String report_name = folder + blockn + "_report.txt";

        FileWriter fileWriter = new FileWriter(report_name);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);

        /* A list<double[]> is used to store the result of wire length evaluation,
         *  Each double[] has 5 elements, represents uram_wr -> bram_rdc1, bram_1 -> dsp1, bram_2 -> dsp4, bram_3 -> bram7,
         *  bram_4 -> dsp1 respectively
         */

        List<double[]> wirelengths = new ArrayList<>();

        printWriter.println("# wire length report from placement solution, block number = " + blockn);
        for (Integer key : placement.keySet()) {

            printWriter.println("conv block #" + key);

            List<Site[]> thisBlock = placement.get(key);
            Site[] dspSites = thisBlock.get(0);
            Site[] bramSites = thisBlock.get(1);
            Site[] uramSites = thisBlock.get(2);
            printWriter.println(" convolutional block # " + key);

            double uram1_bram1 = distance(uramSites[0], bramSites[0]);
            double bram1_dsp1 = distance(bramSites[0], dspSites[0]);
            double bram2_dsp4 = distance(bramSites[1], dspSites[3]);
            double bram3_dsp7 = distance(bramSites[2], dspSites[6]);
            double bram4_dsp1 = distance(bramSites[3], dspSites[0]);

            printWriter.println("uram1 -> bram1 = " + uram1_bram1);
            printWriter.println("bram1 -> dsp1 = "  + bram1_dsp1);
            printWriter.println("bram2 -> dsp4 = "  + bram2_dsp4);
            printWriter.println("bram3 -> dsp7 = "  + bram3_dsp7);
            printWriter.println("bram4 -> dsp1 = "  + bram4_dsp1);

            wirelengths.add(new double[]{uram1_bram1, bram1_dsp1, bram2_dsp4, bram3_dsp7, bram4_dsp1});
        }

        printWriter.close();
        fileWriter.close();

        return wirelengths;
    }

    public static void insert_reg(Design d, String SRCEDIFCell, String SRCPortInst, String DSTEDIFCell, String DSTPortInst, String name, int nReg, String UniqueName) {

        // get input net
        Map<String, EDIFCellInst> edifCells = d.getNetlistInstMap();
        EDIFCellInst srcEdifCell = edifCells.get(SRCEDIFCell);
        Map<String, EDIFPortInst> Ports = srcEdifCell.getPortInstMap();
        EDIFPortInst srcPortInst = Ports.get(SRCPortInst);
        EDIFNet input_net = srcPortInst.getNet();
        EDIFPort srcPort = srcPortInst.getPort();

        // get output net
        EDIFCellInst dstEdifCell = edifCells.get(DSTEDIFCell);
        Map<String, EDIFPortInst> dstPorts = dstEdifCell.getPortInstMap();
        EDIFPortInst dstPortInst = dstPorts.get(DSTPortInst);
        if (input_net != null) {
            EDIFPortInst[] old_ports = input_net.getPortInsts().toArray(new EDIFPortInst[0]);
            input_net.removePortInst(old_ports[0]);
            input_net.removePortInst(old_ports[1]);
        }

        EDIFCell parent = srcEdifCell.getParentCell();
        //String parentName = parent.getLegalEDIFName();
        //System.out.println(parentName);

        // add pipeline registers
        EDIFNet clk = parent.getNet("clk");
        EDIFNet const0 = EDIFTools.getStaticNet(NetType.GND, parent, d.getNetlist());  // get global nets
        EDIFNet const1 = EDIFTools.getStaticNet(NetType.VCC, parent, d.getNetlist());
        EDIFNet rst = parent.getNet("rst");
        List<Cell> pipelineCells = new ArrayList<>();

        for (int i = 0; i < nReg; i++) {
            EDIFCellInst pipelineCellInst = Design.createUnisimInst(parent, UniqueName + "pipeline_cell_" + i, Unisim.FDRE);
            Cell pipelineCell = d.createCell(UniqueName + "pipeline_cell_" + i, pipelineCellInst);
            pipelineCells.add(pipelineCell); // collect the new pipeline cell
            // connect global nets
            clk.createPortInst("C", pipelineCell);
            const1.createPortInst("CE", pipelineCell);
            const0.createPortInst("R", pipelineCell);
        }

        // connect new cells if we have more than one new cell
        if (nReg > 1) {
            for (int i = 0; i < nReg - 1; i++) {
                Net net = d.createNet(name + "_inter_connect_net_" + i);
                net.getLogicalNet().createPortInst("Q", pipelineCells.get(i));
                net.getLogicalNet().createPortInst("D", pipelineCells.get(i + 1));
            }
        }
        // create the beginning and the ending Nets
        Net in_net = d.createNet(name + "_ppln_start");
        in_net.getLogicalNet().createPortInst("D", pipelineCells.get(0));
        if (srcPort.isBus()) {
            int index = srcPortInst.getIndex();
            in_net.getLogicalNet().createPortInst(srcPort, index, srcEdifCell);
        } else {
            in_net.getLogicalNet().createPortInst(SRCPortInst, srcEdifCell);
        }

        Net out_net = d.createNet(name + "_ppln_end");
        out_net.getLogicalNet().createPortInst("Q", pipelineCells.get(nReg - 1));
        out_net.getLogicalNet().createPortInst(DSTPortInst, dstEdifCell);

    }


    public static int get_depth(double distance) {
        return (int)(Math.min(distance / 20, 4));
    }

    public static void auto_pipeline(Design d, Integer blockn, Map<Integer, List<Site[]>> placement) throws IOException {
        String[] srcEDIFCells = new String[]{
                "name[block].dut/uram_inst_wr", "name[block].dut/uram_inst_rd",
                "name[block].dut/conv1/bram_inst_rdc1", "name[block].dut/conv2/bram_inst_rdc1",
                "name[block].dut/conv1/bram_inst_rdc2", "name[block].dut/conv2/bram_inst_rdc2",
                "name[block].dut/conv1/bram_inst_rdc3", "name[block].dut/conv2/bram_inst_rdc3",
                "name[block].dut/conv1/bram_inst_rdc4", "name[block].dut/conv2/bram_inst_rdc4"};
        String[] srcPortInsts = new String[]{
                "DOUT_B[pin]", "DOUT_B[pin]",
                "DOUTADOUT[pin]", "DOUTADOUT[pin]", "DOUTBDOUT[pin]", "DOUTBDOUT[pin]",
                "DOUTADOUT[pin]", "DOUTADOUT[pin]", "DOUTADOUT[pin]", "DOUTADOUT[pin]"
        };
        String[] dstEDIFCells = new String[]{
                "name[block].dut/option_0.uram_rd_data_r_reg[pin]", "name[block].dut/uram2_rd_data_r_reg[pin]",
                "name[block].dut/conv1/a0k0_0.dsp_a0_r_reg[pin]", "name[block].dut/conv2/a0k0_0.dsp_a0_r_reg[pin]",
                "name[block].dut/conv1/rd_data_b2_r1_reg[pin]", "name[block].dut/conv2/rd_data_b2_r1_reg[pin]",
                "name[block].dut/conv1/rd_data_b3_r1_reg[pin]", "name[block].dut/conv2/rd_data_b3_r1_reg[pin]",
                "name[block].dut/conv1/a0k0_0.dsp_k0_r_reg[pin]", "name[block].dut/conv2/a0k0_0.dsp_k0_r_reg[pin]"
        };
        String[] namePrefix = new String[]{
                "name[block].dut/", "name[block].dut/",
                "name[block].dut/conv1/", "name[block].dut/conv2/",
                "name[block].dut/conv1/", "name[block].dut/conv2/",
                "name[block].dut/conv1/", "name[block].dut/conv2/",
                "name[block].dut/conv1/", "name[block].dut/conv2/"
        };

        double[] wl = getMaxDistances(report_wireLengths(placement));

        Properties prop = Tool.getProperties();
        boolean rapidSynth = Boolean.parseBoolean(prop.getProperty("rapidSynth"));

        Integer[] numberofPin = new Integer[]{32, 72,
                16, 16, 16, 16, 16, 16, 8, 8};
        for (int block = 0; block < blockn; block++) {
            for (int i = 0; i < srcEDIFCells.length; i++) {
                /* we only pipeline URAMs for now, BRAM pipelining is not fully supported yet. */
                if (i > 1) continue; // only pipeline URAMs
                for (int pin = 0; pin < numberofPin[i]; pin++) {
                    String srcEDIFCell = srcEDIFCells[i].replaceAll("block", Integer.toString(block));
                    String srcPortInst = srcPortInsts[i].replaceAll("pin", Integer.toString(pin));
                    String dstEDIFCell = dstEDIFCells[i].replaceAll("pin", Integer.toString(pin)).replaceAll("block", Integer.toString(block));
                    String dstPortInst = "D";
                    String name = namePrefix[i] + "pipeline_" + i + "_" + pin;
                    name = name.replaceAll("block", Integer.toString(block));
                    System.out.println("Inserting: " + srcEDIFCell + " -> " + dstEDIFCell + " : " + name);

                    int nreg = get_depth(wl[i/2]);

                    insert_reg(d, srcEDIFCell, srcPortInst, dstEDIFCell, dstPortInst, name, nreg, "pipeline_" + i + "_" + pin);
                    System.out.println("pipeline depth = " + nreg);
                }
            }
            if (rapidSynth) break;
        }
    }

    public static void fixed_pipeline(Design d, Integer depth, Integer blockn) throws IOException {
        String[] srcEDIFCells = new String[]{
                "name[block].dut/uram_inst_wr", "name[block].dut/uram_inst_rd",
                "name[block].dut/conv1/bram_inst_rdc1", "name[block].dut/conv2/bram_inst_rdc1",
                "name[block].dut/conv1/bram_inst_rdc2", "name[block].dut/conv2/bram_inst_rdc2",
                "name[block].dut/conv1/bram_inst_rdc3", "name[block].dut/conv2/bram_inst_rdc3",
                "name[block].dut/conv1/bram_inst_rdc4", "name[block].dut/conv2/bram_inst_rdc4"};
        String[] srcPortInsts = new String[]{
                "DOUT_B[pin]", "DOUT_B[pin]",
                "DOUTADOUT[pin]", "DOUTADOUT[pin]", "DOUTBDOUT[pin]", "DOUTBDOUT[pin]",
                "DOUTADOUT[pin]", "DOUTADOUT[pin]", "DOUTADOUT[pin]", "DOUTADOUT[pin]"
        };
        String[] dstEDIFCells = new String[]{
                "name[block].dut/option_0.uram_rd_data_r_reg[pin]", "name[block].dut/uram2_rd_data_r_reg[pin]",
                "name[block].dut/conv1/a0k0_0.dsp_a0_r_reg[pin]", "name[block].dut/conv2/a0k0_0.dsp_a0_r_reg[pin]",
                "name[block].dut/conv1/rd_data_b2_r1_reg[pin]", "name[block].dut/conv2/rd_data_b2_r1_reg[pin]",
                "name[block].dut/conv1/rd_data_b3_r1_reg[pin]", "name[block].dut/conv2/rd_data_b3_r1_reg[pin]",
                "name[block].dut/conv1/a0k0_0.dsp_k0_r_reg[pin]", "name[block].dut/conv2/a0k0_0.dsp_k0_r_reg[pin]"
        };
        String[] namePrefix = new String[]{
                "name[block].dut/", "name[block].dut/",
                "name[block].dut/conv1/", "name[block].dut/conv2/",
                "name[block].dut/conv1/", "name[block].dut/conv2/",
                "name[block].dut/conv1/", "name[block].dut/conv2/",
                "name[block].dut/conv1/", "name[block].dut/conv2/"
        };

        int nreg = depth;

        Properties prop = Tool.getProperties();
        boolean rapidSynth = Boolean.parseBoolean(prop.getProperty("rapidSynth"));

        Integer[] numberofPin = new Integer[]{32, 72,
                16, 16, 16, 16, 16, 16, 8, 8};
        for (int block = 0; block < blockn; block++) {
            for (int i = 0; i < srcEDIFCells.length; i++) {
                /* we only pipeline URAMs for now, BRAM pipelining is not fully supported yet. */
                //if (i > 1) continue; // only pipeline URAMs
                for (int pin = 0; pin < numberofPin[i]; pin++) {
                    String srcEDIFCell = srcEDIFCells[i].replaceAll("block", Integer.toString(block));
                    String srcPortInst = srcPortInsts[i].replaceAll("pin", Integer.toString(pin));
                    String dstEDIFCell = dstEDIFCells[i].replaceAll("pin", Integer.toString(pin)).replaceAll("block", Integer.toString(block));
                    String dstPortInst = "D";
                    String name = namePrefix[i] + "pipeline_" + i + "_" + pin;
                    name = name.replaceAll("block", Integer.toString(block));
                    System.out.println("Inserting: " + srcEDIFCell + " -> " + dstEDIFCell + " : " + name);
                    insert_reg(d, srcEDIFCell, srcPortInst, dstEDIFCell, dstPortInst, name, nreg, "pipeline_" + i + "_" + pin);
                    System.out.println("pipeline depth = " + nreg);
                }
            }
            if (rapidSynth) break;
        }
    }


    /*
     *        ----   Pipeline Register Insertion Locations  ----
     *    | source EDIFCell                     |   source PortInst   |                 destination EDIFCell                | destination PortInst |  number of pin (width)  |
     *    |-------------------------------------|---------------------|-----------------------------------------------------|----------------------|-------------------------|
     *    | name[i].dut/uram_inst_wr            |     DOUT_B[pin]     |  name[i].dut/option_3.uram_rd_data_r2_reg[pin]_srl2 |           D          |           32            |
     *    | name[i].dut/uram_inst_rd            |     DOUT_B[pin]     |  name[i].dut/uram2_rd_data_r_reg[pin]               |           D          |           72            |
     *    | name[i].dut/conv1(2)/bram_inst_rdc1 |     DOUTADOUT[pin]  |  name[i].dut/conv1(2)/a0k0_3.dsp_a0_1_reg[pin]      |           D          |           16            |
     *    | name[i].dut/conv1(2)/bram_inst_rdc2 |     DOUTBDOUT[pin]  |  name[i].dut/conv1(2)/rd_data_b2_r1_reg[pin]        |           D          |           16            |
     *    | name[i].dut/conv1(2)/bram_inst_rdc3 |     DOUTADOUT[pin]  |  name[i].dut/conv1(2)/rd_data_b3_r1_reg[pin]        |           D          |           16            |
     *    | name[i].dut/conv1(2)/bram_inst_rdc4 |     DOUTADOUT[pin]  |  name[i].dut/conv1(2)/a0k0_3.dsp_k0_1_reg[pin]      |           D          |           8             |
     * */

    /*
     *    Special Note: all BRAMs should be pipelined with the same amount of registers so that the outputs are aligned
     */


    /* debug autopipeline */
//    public static void main(String[] args) throws IOException {
//        String input_dcp = System.getenv("RAPIDWRIGHT_PATH") + "/checkpoint/replicated.dcp";
//        String output_dcp = System.getenv("RAPIDWRIGHT_PATH") + "/checkpoint/bram_pipelined.dcp";
//        Design d = Design.readCheckpoint(input_dcp);
//        String xdc_result = System.getenv("RAPIDWRIGHT_PATH") + "/result/blockNum=80.xdc";
//        String device = "vu11p";
//        Map<Integer, List<Site[]>> result = Tool.getMapFromXDC(xdc_result, device);
//        for (int i = 0; i < 2; i++) {
//            AutoPlacement.place_block(d, i, result.get(i));
//        }
//        d.routeSites();
//        AutoPipeline.fixed_pipeline(d, 4, 2);
//        d.writeCheckpoint(output_dcp);
//
//        // vivado part
//        String tcl = System.getenv("RAPIDWRIGHT_PATH") + "/tcl/min_prob.tcl";
//        PrintWriter pr = new PrintWriter(new FileWriter(tcl), true);
//        pr.println("open_checkpoint " + output_dcp);
//        pr.println("write_checkpoint -force " + output_dcp);
//        pr.println("open_checkpoint " + output_dcp);
//        pr.println("startgroup");
//        pr.println("create_pblock {pblock_name.dut}");
//        pr.println("resize_pblock {pblock_name.dut} -add CLOCKREGION_X0Y0:CLOCKREGION_X7Y3");
//        pr.println("add_cells_to_pblock {pblock_name.dut} -top");
//        pr.println("endgroup");
//        pr.println("set_property CONTAIN_ROUTING true [get_pblocks pblock_name.dut]");
//        pr.println("create_clock -period 1.000 -waveform {0.000 0.500} [get_nets clk];");
//        pr.println("place_design; route_design;");
//        pr.close();
//
//        Tool.execute_cmd("vivado -mode tcl -source " + tcl);
//    }

    public static void main(String[] args) throws IOException {
        String xdc_result = System.getenv("RAPIDWRIGHT_PATH") + "/result/blockNum=80.xdc";
        String device = "vu11p";
        Map<Integer, List<Site[]>> result = Tool.getMapFromXDC(xdc_result, device);

        report_wireLengths(result);
    }

}
