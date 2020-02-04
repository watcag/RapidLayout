package main;

import com.xilinx.rapidwright.design.*;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.edif.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AutoPipeline {

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
    public static double[] report_wireLengths(Map<Integer, List<Site[]>> placement) throws IOException {
        int blockn = placement.size();
        String folder = System.getProperty("user.home") + "/RapidWright/result/report_wireLengths/";
        String report_name = folder + blockn + "_report.txt";
        FileWriter fileWriter = new FileWriter(report_name);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);

        /* A list<double[]> is used to store the result of wire length evaluation,
        *  Each double[] has 5 elements, represents uram_wr -> bram_rdc1, bram_1 -> dsp1, bram_2 -> dsp4, bram_3 -> bram7,
        *  bram_4 -> dsp1 respectively
        */

        double max_1 = 0;
        double max_2 = 0;

        printWriter.println("# wire length report from placement solution, block number = " + blockn);
        for (Integer key : placement.keySet()){

            List<Site[]> thisBlock = placement.get(key);
            Site[] dspSites = thisBlock.get(0);
            Site[] bramSites = thisBlock.get(1);
            Site[] uramSites = thisBlock.get(2);
            printWriter.println(" convolutional block # " + key);

            // evaluate wire length from URAMs to BRAMs
            double ManhattanD = uramSites[0].getTile().getManhattanDistance(bramSites[0].getTile());
            double ManhattanD2 = uramSites[1].getTile().getManhattanDistance(dspSites[8].getTile());
            //printWriter.println("uram_wr ---> bram_rdc1   =   " + ManhattanD);
            //printWriter.println("dsp9 --> uram_rd = " + ManhattanD2);
            if (ManhattanD > max_1)
                max_1 = ManhattanD;
            if (ManhattanD2 > max_2)
                max_2 = ManhattanD2;

            //wireLengths.add(new double[]{ManhattanD, ManhattanD2});
        }

        printWriter.close();
        fileWriter.close();

        return new double[]{max_1, max_2};

    }


    /*
        The current problem is that src and dst must be in the same parent cell, which is not usually the case
     */

    public static void insert_reg(Design d, String SRCEDIFCell, String SRCPortInst, String DSTEDIFCell, String DSTPortInst, String name, int nReg) {

        Device dev = d.getDevice();

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
        EDIFNet output_net = dstPortInst.getNet();
        if (input_net != null){
            EDIFPortInst[] old_ports = input_net.getPortInsts().toArray(new EDIFPortInst[0]);
            input_net.removePortInst(old_ports[0]);
            input_net.removePortInst(old_ports[1]);
        }

        EDIFCell parent = srcEdifCell.getParentCell();
        String parentName = parent.getLegalEDIFName();
        //System.out.println(parentName); // all the same fucking parent EDIFCell

        // add pipeline registers
        EDIFNet clk = parent.getNet("clk");
        EDIFNet const0 = EDIFTools.getStaticNet(NetType.GND, parent, d.getNetlist());  // get global nets
        EDIFNet rst = parent.getNet("rst");
        List<Cell> pipelineCells = new ArrayList<>();
        for (int i = 0; i < nReg; i++){
            Site s = dev.getSite("SLICE_X43Y"+i); // random site, we'll unplace later
            //System.out.println(name + "_pipeline_cell_"+ i);
            //EDIFCell fdre = d.getNetlist().getHDIPrimitive(Unisim.FDRE);
            //EDIFCellInst fdreInst = new EDIFCellInst( name + "_pipeline_cell_"+ i , fdre, parent);
            //Cell pipelineCell = d.createCell(name + "_pipeline_cell_"+ i, fdreInst);
            //Cell pipelineCell = d.createCell(name + "_pipeline_cell_" + i, fdre);

            Cell pipelineCell = d.createAndPlaceCell(parent, name + "_pipeline_cell_"+ i, Unisim.FDRE, s, s.getBEL("HFF2"));
            //EDIFCellInst pipelineCellInst = pipelineCell.getEDIFCellInst();
            pipelineCell.unplace();
            pipelineCells.add(pipelineCell); // collect the new pipeline cell
            // connect global nets
            clk.createPortInst("C", pipelineCell);
            const0.createPortInst("CE", pipelineCell);
            rst.createPortInst("R", pipelineCell);
        }

        // connect new cells if we have more than one new cell
        if (nReg > 1){
            for  (int i = 0; i < nReg - 1 ; i ++){
                Net net = d.createNet(name + "_inter_connect_net_"+i);
                net.getLogicalNet().createPortInst("Q", pipelineCells.get(i));
                net.getLogicalNet().createPortInst("D", pipelineCells.get(i+1));
            }
        }

        // TODO: should discriminate between bus and wire
        Net in_net = d.createNet(name + "_ppln_start");
        in_net.getLogicalNet().createPortInst("D", pipelineCells.get(0));
        if (srcPort.isBus()){
            int index = srcPortInst.getIndex();
            in_net.getLogicalNet().createPortInst(srcPort, index, srcEdifCell);
        } else {
            in_net.getLogicalNet().createPortInst(SRCPortInst, srcEdifCell);
        }

        //EDIFNet new_net = new EDIFNet(name +"_ppln_final", net_parent);
        Net out_net = d.createNet(name +"_ppln_end");
        out_net.getLogicalNet().createPortInst("Q", pipelineCells.get(nReg-1));
        out_net.getLogicalNet().createPortInst(DSTPortInst, dstEdifCell);

    }

    public static void insert_reg_2(Design d, String SRCEDIFCell, String SRCPortInst, String DSTEDIFCell, String DSTPortInst, String name, int nReg, String UniqueName) {
        /*
           this function try to fix the issue that we can't insert register inside name[i].dut/conv1
           I need to figure out why
         */
        Device dev = d.getDevice();

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
        if (input_net != null){
            EDIFPortInst[] old_ports = input_net.getPortInsts().toArray(new EDIFPortInst[0]);
            input_net.removePortInst(old_ports[0]);
            input_net.removePortInst(old_ports[1]);
        }

        EDIFCell parent = srcEdifCell.getParentCell();
        String parentName = parent.getLegalEDIFName();
        //System.out.println(parentName); // all the same fucking parent EDIFCell

        // add pipeline registers
        EDIFNet clk = parent.getNet("clk");
        EDIFNet const0 = EDIFTools.getStaticNet(NetType.GND, parent, d.getNetlist());  // get global nets
        EDIFNet const1 = EDIFTools.getStaticNet(NetType.VCC, parent, d.getNetlist());
        EDIFNet rst = parent.getNet("rst");
        List<Cell> pipelineCells = new ArrayList<>();

        for (int i = 0; i < nReg; i++){
            EDIFCellInst pipelineCellInst = Design.createUnisimInst(parent, UniqueName + "pipeline_cell_" + i, Unisim.FDRE);
            Cell pipelineCell = d.createCell(UniqueName + "pipeline_cell_" + i, pipelineCellInst);
            pipelineCells.add(pipelineCell); // collect the new pipeline cell
            // connect global nets
            clk.createPortInst("C", pipelineCell);
            const1.createPortInst("CE", pipelineCell);
            const0.createPortInst("R", pipelineCell);
        }

        // connect new cells if we have more than one new cell
        if (nReg > 1){
            for  (int i = 0; i < nReg - 1 ; i ++){
                Net net = d.createNet(name + "_inter_connect_net_"+i);
                net.getLogicalNet().createPortInst("Q", pipelineCells.get(i));
                net.getLogicalNet().createPortInst("D", pipelineCells.get(i+1));
            }
        }
        // create the beginning and the ending Nets
        Net in_net = d.createNet(name + "_ppln_start");
        in_net.getLogicalNet().createPortInst("D", pipelineCells.get(0));
        if (srcPort.isBus()){
            int index = srcPortInst.getIndex();
            in_net.getLogicalNet().createPortInst(srcPort, index, srcEdifCell);
        } else {
            in_net.getLogicalNet().createPortInst(SRCPortInst, srcEdifCell);
        }

        Net out_net = d.createNet(name +"_ppln_end");
        out_net.getLogicalNet().createPortInst("Q", pipelineCells.get(nReg-1));
        out_net.getLogicalNet().createPortInst(DSTPortInst, dstEdifCell);

    }




    public static void AutoPipeline(Design d, Map<Integer, List<Site[]>> placement) throws IOException {

        /*List<double[]> wireLength = report_wireLengths(placement);
        for (int i = 0; i < wireLength.size(); i++){
            double uram_length = wireLength.get(i)[0];
            if (uram_length > 0){
                //int reg_num = uram_length > 150 ? 2 : 1;
                // add excessive registers
                int reg_num = 4;
                for (int pin = 0; pin < 32; pin++){
                   insert_reg(d, "name["+i+"].dut/uram_inst_wr", "DOUT_B[" + pin + "]",
                           "name["+i+"].dut/option_3.uram_rd_data_r2_reg[" + pin + "]_srl2", "D",
                           "name["+i+"].dut/" + i + "_" + pin, reg_num);
                }

            }
        }*/

    }

    public static void rapidsynth_autopipeline(Design d, Map<Integer, List<Site[]>> placement) throws  IOException {
        String[] srcEDIFCells = new String[]{
                "name[block].dut/uram_inst_wr", "name[block].dut/uram_inst_rd",
                "name[block].dut/conv1/bram_inst_rdc1","name[block].dut/conv2/bram_inst_rdc1",
                "name[block].dut/conv1/bram_inst_rdc2", "name[block].dut/conv2/bram_inst_rdc2",
                "name[block].dut/conv1/bram_inst_rdc3", "name[block].dut/conv2/bram_inst_rdc3",
                "name[block].dut/conv1/bram_inst_rdc4", "name[block].dut/conv2/bram_inst_rdc4"};
        String[] srcPortInsts = new String[] {
                "DOUT_B[pin]", "DOUT_B[pin]",
                "DOUTADOUT[pin]", "DOUTADOUT[pin]","DOUTBDOUT[pin]","DOUTBDOUT[pin]",
                "DOUTADOUT[pin]","DOUTADOUT[pin]","DOUTADOUT[pin]","DOUTADOUT[pin]"
        };
        String[] dstEDIFCells = new String[] {
                "name[block].dut/option_3.uram_rd_data_r2_reg[pin]_srl2", "name[block].dut/uram2_rd_data_r_reg[pin]",
                "name[block].dut/conv1/a0k0_3.dsp_a0_1_reg[pin]", "name[block].dut/conv2/a0k0_3.dsp_a0_1_reg[pin]",
                "name[block].dut/conv1/rd_data_b2_r1_reg[pin]","name[block].dut/conv2/rd_data_b2_r1_reg[pin]",
                "name[block].dut/conv1/rd_data_b3_r1_reg[pin]","name[block].dut/conv2/rd_data_b3_r1_reg[pin]",
                "name[block].dut/conv1/a0k0_3.dsp_k0_1_reg[pin]","name[block].dut/conv2/a0k0_3.dsp_k0_1_reg[pin]"
        };
        String[] namePrefix = new String[] {
                "name[block].dut/", "name[block].dut/",
                "name[block].dut/conv1/", "name[block].dut/conv2/",
                "name[block].dut/conv1/", "name[block].dut/conv2/",
                "name[block].dut/conv1/", "name[block].dut/conv2/",
                "name[block].dut/conv1/", "name[block].dut/conv2/"
        };

        int nreg = 3;

        Integer[] numberofPin = new Integer[] {32,72,
                16,16, 16,16, 16,16, 8,8};
        for (int block = 0; block < placement.size(); block++) {
            for (int i = 0; i < srcEDIFCells.length; i++) {
                if (i > 1) continue; // only pipeline URAMs
                for (int pin = 0; pin < numberofPin[i]; pin++) {
                    String srcEDIFCell = srcEDIFCells[i].replaceAll("block", Integer.toString(block));
                    String srcPortInst = srcPortInsts[i].replaceAll("pin", Integer.toString(pin));
                    String dstEDIFCell = dstEDIFCells[i].replaceAll("pin", Integer.toString(pin)).replaceAll("block", Integer.toString(block));
                    String dstPortInst = "D";
                    String name = namePrefix[i] + "pipeline_" + i + "_" + pin;
                    name = name.replaceAll("block", Integer.toString(block));
                    System.out.println("Inserting: " + srcEDIFCell + " -> " + dstEDIFCell + " : " + name);
                    //insert_reg(d, srcEDIFCell, srcPortInst, dstEDIFCell, dstPortInst, name, 4); // we used this for full-chip
                    insert_reg_2(d, srcEDIFCell, srcPortInst, dstEDIFCell, dstPortInst, name, nreg, "pipeline_"+i+"_"+pin);
                    System.out.println("pipeline depth = " + nreg);
                }
            }
        }
    }

    public static void fixed_pipeline(Design d, Integer depth, Integer blockn) throws  IOException {
        String[] srcEDIFCells = new String[]{
                "name[block].dut/uram_inst_wr", "name[block].dut/uram_inst_rd",
                "name[block].dut/conv1/bram_inst_rdc1","name[block].dut/conv2/bram_inst_rdc1",
                "name[block].dut/conv1/bram_inst_rdc2", "name[block].dut/conv2/bram_inst_rdc2",
                "name[block].dut/conv1/bram_inst_rdc3", "name[block].dut/conv2/bram_inst_rdc3",
                "name[block].dut/conv1/bram_inst_rdc4", "name[block].dut/conv2/bram_inst_rdc4"};
        String[] srcPortInsts = new String[] {
                "DOUT_B[pin]", "DOUT_B[pin]",
                "DOUTADOUT[pin]", "DOUTADOUT[pin]","DOUTBDOUT[pin]","DOUTBDOUT[pin]",
                "DOUTADOUT[pin]","DOUTADOUT[pin]","DOUTADOUT[pin]","DOUTADOUT[pin]"
        };
        String[] dstEDIFCells = new String[] {
                "name[block].dut/option_0.uram_rd_data_r_reg[pin]", "name[block].dut/uram2_rd_data_r_reg[pin]",
                "name[block].dut/conv1/a0k0_3.dsp_a0_1_reg[pin]", "name[block].dut/conv2/a0k0_3.dsp_a0_1_reg[pin]",
                "name[block].dut/conv1/rd_data_b2_r1_reg[pin]","name[block].dut/conv2/rd_data_b2_r1_reg[pin]",
                "name[block].dut/conv1/rd_data_b3_r1_reg[pin]","name[block].dut/conv2/rd_data_b3_r1_reg[pin]",
                "name[block].dut/conv1/a0k0_3.dsp_k0_1_reg[pin]","name[block].dut/conv2/a0k0_3.dsp_k0_1_reg[pin]"
        };
        String[] namePrefix = new String[] {
                "name[block].dut/", "name[block].dut/",
                "name[block].dut/conv1/", "name[block].dut/conv2/",
                "name[block].dut/conv1/", "name[block].dut/conv2/",
                "name[block].dut/conv1/", "name[block].dut/conv2/",
                "name[block].dut/conv1/", "name[block].dut/conv2/"
        };

        int nreg = depth;

        Integer[] numberofPin = new Integer[] {32,72,
                16,16, 16,16, 16,16, 8,8};
        for (int block = 0; block < blockn; block++) {
            for (int i = 0; i < srcEDIFCells.length; i++) {
                if (i > 1) continue; // only pipeline URAMs
                for (int pin = 0; pin < numberofPin[i]; pin++) {
                    String srcEDIFCell = srcEDIFCells[i].replaceAll("block", Integer.toString(block));
                    String srcPortInst = srcPortInsts[i].replaceAll("pin", Integer.toString(pin));
                    String dstEDIFCell = dstEDIFCells[i].replaceAll("pin", Integer.toString(pin)).replaceAll("block", Integer.toString(block));
                    String dstPortInst = "D";
                    String name = namePrefix[i] + "pipeline_" + i + "_" + pin;
                    name = name.replaceAll("block", Integer.toString(block));
                    System.out.println("Inserting: " + srcEDIFCell + " -> " + dstEDIFCell + " : " + name);
                    //insert_reg(d, srcEDIFCell, srcPortInst, dstEDIFCell, dstPortInst, name, 4); // we used this for full-chip
                    insert_reg_2(d, srcEDIFCell, srcPortInst, dstEDIFCell, dstPortInst, name, nreg, "pipeline_"+i+"_"+pin);
                    System.out.println("pipeline depth = " + nreg);
                }
            }
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
    *    Special Note: all BRAMs should be pipelined with the same amount of registers
    */


    public static void main(String[] args) throws IOException {
        // this is a test function for register insertion inside name[i].dut/conv1
        // I have experience some bizarre behaviour of Vivado: I can see that I have successfully inserted registers inside
        // but the placer would just quite without any obvious reason, only "Abnormal Program Termination"
        // set up env variable

        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));

        String logPath = System.getProperty("RAPIDWRIGHT_PATH") + "/log.txt";
        FileWriter fileWriter = new FileWriter(logPath);
        PrintWriter log = new PrintWriter(fileWriter, true); // enable auto-flush

        String device = "xcvu11p";
        String part = "xcvu11p-fsgd2104-3-e"; // without high-band-width memory

        int blockNum = 2;

        Design d = Vivado.synthesize_vivado(blockNum, part, 0, true);

        String[] srcEDIFCells = new String[]{
                "name[block].dut/conv1/bram_inst_rdc1","name[block].dut/conv2/bram_inst_rdc1",
        };
        String[] srcPortInsts = new String[] {
                "DOUTADOUT[pin]", "DOUTADOUT[pin]"
        };
        String[] dstEDIFCells = new String[] {
                "name[block].dut/conv1/a0k0_3.dsp_a0_1_reg[pin]", "name[block].dut/conv2/a0k0_3.dsp_a0_1_reg[pin]"
        };
        String[] namePrefix = new String[] {
                "name[block].dut/conv1/", "name[block].dut/conv2/"
        };

        Integer[] numberofPin = new Integer[] {16,16};
        for (int block = 0; block < blockNum; block++) {
            for (int i = 0; i < srcEDIFCells.length; i++) {
                for (int pin = 0; pin < numberofPin[i]; pin++) {
                    String srcEDIFCell = srcEDIFCells[i].replaceAll("block", Integer.toString(block));
                    String srcPortInst = srcPortInsts[i].replaceAll("pin", Integer.toString(pin));
                    String dstEDIFCell = dstEDIFCells[i].replaceAll("pin", Integer.toString(pin)).replaceAll("block", Integer.toString(block));
                    String dstPortInst = "D";
                    String name = namePrefix[i] + "pipeline_" + i + "_" + pin;
                    name = name.replaceAll("block", Integer.toString(block));
                    System.out.println("Inserting: " + srcEDIFCell + " -> " + dstEDIFCell + " : " + name);
                    insert_reg_2(d, srcEDIFCell, srcPortInst, dstEDIFCell, dstPortInst, name, 1, "pipeline_"+i+"_"+pin);
                }
            }
        }

        String pipelined = System.getProperty("RAPIDWRIGHT_PATH") + "/checkpoint/debug_pipelined.dcp";
        File p = new File(pipelined);
        if (p.exists())
            p.delete();
        d.writeCheckpoint(pipelined);

        Vivado.finishPlacementNRoute(pipelined, blockNum, true);

    }


}
