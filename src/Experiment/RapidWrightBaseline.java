package Experiment;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Module;
import com.xilinx.rapidwright.design.ModuleInst;
import com.xilinx.rapidwright.design.Net;
import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.edif.*;
import com.xilinx.rapidwright.placer.blockplacer.BlockPlacer;
import main.Tool;
import main.Vivado;

import java.io.IOException;
import java.util.*;

public class RapidWrightBaseline {

    public static Design replicateConvBlocks(Design d, int replicate_num){

        Design bebes = new Design(d.getName()+"_bebes", d.getPartName());
        bebes.setAutoIOBuffers(false);
        // add basic cells to new working library
        for (EDIFCell cell : d.getNetlist().getWorkLibrary().getCells())
            bebes.getNetlist().getWorkLibrary().addCell(cell);
        EDIFLibrary hdi = bebes.getNetlist().getHDIPrimitivesLibrary();
        for (EDIFCell cell : d.getNetlist().getHDIPrimitivesLibrary().getCells()){
            if (!hdi.containsCell(cell))
                hdi.addCell(cell);
        }

        Net clk = bebes.createNet("clk");
        Net rst = bebes.createNet("rst");
        Net ce = bebes.createNet("ce");

        // Replicate bebe Cells
        String[] dontConnect = new String[]{"CAS_OUT_ADDR", "CAS_OUT_BWE", "CAS_OUT_DBITERR", "clk", "rst", "ce",
                "CAS_OUT_DIN", "CAS_OUT_DOUT", "CAS_OUT_EN", "CAS_OUT_RDACCESS","CAS_OUT_RDB_WR", "CAS_OUT_SBITERR"};
        EDIFCell template_old = d.getTopEDIFCell().getCellInst("name[0].dut").getCellType();

        for (int i = 0; i < replicate_num; i++){
            EDIFCell template = template_old;

            Module module = new Module(d);
            ModuleInst moduleInst = bebes.createModuleInst("name["+i+"].module", module);

            EDIFCellInst cellInst = moduleInst.getCellInst();

            EDIFLibrary library = bebes.getNetlist().getWorkLibrary();
            library.addCell(template);

            for(EDIFPort port : cellInst.getCellPorts()){
                String portName = port.isBus() ? port.getBusName() : port.getName();
                if (Arrays.stream(dontConnect).anyMatch(str -> portName.contains(str)))
                    continue;
                String ext_name = port.isBus() ?
                        port.getBusName() + "[" + i + "][" + (port.getWidth()-1) + ":0]"
                        : port.getBusName() + "_"+ i +"_";
                EDIFPort top_port = bebes.getNetlist().getTopCell().createPort(ext_name, port.getDirection(), port.getWidth());

                for (int index = 0 ; index < top_port.getWidth(); index++){
                    String netName = port.isBus() ? port.getBusName() + "[" + i + "]" + "[" + index + "]" :  port.getBusName() + "_" + i + "_";
                    Net net = bebes.createNet(netName);
                    if (port.isBus()){
                        net.getLogicalNet().createPortInst(top_port, index);
                        net.getLogicalNet().createPortInst(port, index, cellInst);
                    } else {
                        net.getLogicalNet().createPortInst(top_port);
                        net.getLogicalNet().createPortInst(port, cellInst);
                    }
                }
            }

            // connect global nets
            clk.getLogicalNet().createPortInst("clk", moduleInst.getCellInst());
            rst.getLogicalNet().createPortInst("rst",moduleInst.getCellInst());
            ce.getLogicalNet().createPortInst("ce", moduleInst.getCellInst());
        }

        EDIFPort clkPort = bebes.getTopEDIFCell().createPort("clk", EDIFDirection.INPUT, 1);
        EDIFPort rstPort = bebes.getTopEDIFCell().createPort("rst", EDIFDirection.INPUT, 1);
        EDIFPort cePort = bebes.getTopEDIFCell().createPort("ce", EDIFDirection.INPUT, 1);
        clk.getLogicalNet().createPortInst(clkPort);
        rst.getLogicalNet().createPortInst(rstPort);
        ce.getLogicalNet().createPortInst(cePort);

        return bebes;

    }

    public static void rwSAPlacer() throws IOException {

        // read config
        String device = "xcvu11p";
        String part = new Design("name", device).getPartName();

        // set up paths
        String root = System.getProperty("RAPIDWRIGHT_PATH") + "/";
        String checkpoint = root + "checkpoint/";
        String results = root + "result/";

        // set up design parameters
        int blocknum = 1;
        int depth = 0;

        Design one = Vivado.synthesize_vivado(blocknum, part, depth, true);

        Design d = replicateConvBlocks(one, 480);
        Collection<ModuleInst> moduleInsts = d.getModuleInsts();
        for (ModuleInst moduleInst : moduleInsts) {
            ArrayList<Site> avail = moduleInst.getAllValidPlacements();
            System.out.println("the number of available placement for module: " + moduleInst.getName() + " is " + avail.size());
        }

        int movesPerTemperature = 200;
        long seed = 1;
        double tempReduceRate = 10;
        double startTempFactor = 2;
        boolean verbose = true;

        //BlockPlacer blockPlacer = new BlockPlacer(movesPerTemperature, seed, tempReduceRate, startTempFactor, verbose);

        // Default parameters
        BlockPlacer blockPlacer = new BlockPlacer();

        System.out.println("number of hard macros: " + d.getModuleInsts().size());

        d = blockPlacer.placeDesign(d, true);

        d.writeCheckpoint(checkpoint + "rapidwright.dcp");
    }

    public static void rwModulePlacer() {
        // read config
        String device = "xcvu11p";
        String part = new Design("name", device).getPartName();

        // set up paths
        String root = System.getProperty("RAPIDWRIGHT_PATH") + "/";
        String checkpoint = root + "checkpoint/";
        String results = root + "result/";

        // synthesize
        Design one = Vivado.synthesize_vivado(1, part, 0, true);

        Design d = replicateConvBlocks(one, 480);
        Collection<ModuleInst> moduleInsts = d.getModuleInsts();
        List<ModuleInst> moduleInsts1 = new ArrayList<>(moduleInsts);
        ArrayList<Site> avail = moduleInsts1.get(0).getAllValidPlacements();

        for (int i=0; i < avail.size(); i++) {
            moduleInsts1.get(i).place(avail.get(i));
        }

        System.out.println("placed cells: " + avail.size());

        d.writeCheckpoint(checkpoint + "/rapidWright_baseline.dcp");
    }


    public static void main(String[] args) throws IOException {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));
        // default xdc path
        String xdc = System.getenv("RAPIDWRIGHT_PATH") + "/src/verilog/dsp_conv_chip.xdc";


        rwModulePlacer();


    }
}
