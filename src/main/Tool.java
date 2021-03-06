package main;

import Utils.Utility;
import com.xilinx.rapidwright.design.*;
import com.xilinx.rapidwright.design.Module;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.edif.*;
import com.esotericsoftware.kryo.Kryo;

import java.io.*;
import java.util.*;

public class Tool {
    // Utility Tools

    public static void execute_cmd(String command) {

        System.out.println("Executing Command: " + command);

        try {
            ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", command);
            builder.redirectErrorStream(true);
            builder.directory(new File(System.getProperty("user.home") + "/RapidWright/"));
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
                System.out.println(line);

            }
            p.waitFor();
            r.close();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static void write_XDC(Map<Integer, List<Site[]>> map, PrintWriter print_line) {
        Set<Integer> keys = map.keySet();
        for (Integer index : keys) {
            print_line.println("# Block " + index + ": ");

            List<Site[]> placement = map.get(index);

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
            for (int i = 0; i < 18; i++)
                print_line.println("set_property LOC " + placement.get(0)[i].getName() + "\t" + "[get_cells {" + dspName[i] + "}];");

            for (int i = 0; i < 8; i++)
                print_line.println("set_property LOC " + placement.get(1)[i].getName() + "\t" + "[get_cells {" + bramName[i] + "}];");

            for (int i = 0; i < 2; i++)
                print_line.println("set_property LOC " + placement.get(2)[i].getName() + "\t" + "[get_cells {" + uramName[i] + "}];");

        }
        print_line.println("# end");
    }

    public static Map<Integer, List<Site[]>> getMapFromXDC(String xdcFile, String device) throws IOException{
        System.out.println("Reading Placement Solution from XDC file : " + xdcFile);
        int ele_index = 0;
        Design design = new Design("name", device);
        Device dev = design.getDevice();
        Map<Integer, List<Site[]>> phenotype = new HashMap<>();
        int block_index = 0;

        // read line by line
        List<Site[]> oneBlock = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(xdcFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("#")) {
                    String siteName = line.split("\t")[0].split(" ")[2];
                    Site site = dev.getSite(siteName);
                    block_index = Integer.parseInt(line.split("\\{",2)[1].split("\\[",2)[1].split("]",2)[0]);
                    if (ele_index < 18)
                        oneBlock.get(0)[ele_index] = site;
                    else if (ele_index < 26)
                        oneBlock.get(1)[ele_index - 18] = site;
                    else
                        oneBlock.get(2)[ele_index - 26] = site;
                    ele_index += 1;
                } else {
                    if (!oneBlock.isEmpty())
                        phenotype.put(block_index, new ArrayList<>(oneBlock));
                    oneBlock.clear();
                    oneBlock.add(new Site[18]);
                    oneBlock.add(new Site[8]);
                    oneBlock.add(new Site[2]);
                    ele_index = 0;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return phenotype;
    }

    public static Map<Integer, List<Site[]>> getMapFromXDCRobust(String xdcFile, String device, int blockNum) throws IOException{
        System.out.println("Reading Placement Solution from XDC file : " + xdcFile);
        Design design = new Design("name", device);
        Device dev = design.getDevice();
        Map<Integer, List<Site[]>> phenotype = new HashMap<>();
        Map<Integer, List<List<Site>>> pocket = new HashMap<>();
        for (int i = 0; i < blockNum; i++){
            List<Site> dsps = new ArrayList<>();
            List<Site> brams = new ArrayList<>();
            List<Site> urams = new ArrayList<>();
            List<List<Site>> convUnit = new ArrayList<>();
            convUnit.add(dsps);
            convUnit.add(brams);
            convUnit.add(urams);
            pocket.put(i, convUnit);
        }

        // read line by line
        List<Site[]> oneBlock = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(xdcFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("set_property")) {
                    String siteName = line.split("\t")[0].split(" ")[2];
                    Site site = dev.getSite(siteName);
                    String siteType = siteName.split("_")[0];
                    int block_index = Integer.parseInt(line.split("\\{",2)[1].split("\\[",2)[1].split("]",2)[0]);
                    List<List<Site>> convUnit = pocket.get(block_index);
                    if (siteType.equals("DSP48E2"))
                        convUnit.get(0).add(site);
                    else if (siteType.equals("RAMB18"))
                        convUnit.get(1).add(site);
                    else
                        convUnit.get(2).add(site);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // convert to our phenotype
        for (int i = 0; i < blockNum; i++) {
            List<List<Site>> convUnit = pocket.get(i);
            Site[] dsps = convUnit.get(0).toArray(new Site[0]);
            Site[] brams = convUnit.get(1).toArray(new Site[0]);
            Site[] urams = convUnit.get(2).toArray(new Site[0]);
            List<Site[]> real_convUnit = new ArrayList<>();
            real_convUnit.add(dsps);
            real_convUnit.add(brams);
            real_convUnit.add(urams);
            phenotype.put(i, real_convUnit);
        }

        return phenotype;
    }


    public static Design replicateConvBlocks(Design d, int replicate_num){

        Design babies = new Design(d.getName()+"_repl", d.getPartName());
        babies.setAutoIOBuffers(false);
        // add basic cells to new working library
        for (EDIFCell cell : d.getNetlist().getWorkLibrary().getCells())
            babies.getNetlist().getWorkLibrary().addCell(cell);
        EDIFLibrary hdi = babies.getNetlist().getHDIPrimitivesLibrary();
        for (EDIFCell cell : d.getNetlist().getHDIPrimitivesLibrary().getCells()){
            if (!hdi.containsCell(cell))
                hdi.addCell(cell);
        }

        Net clk = babies.createNet("clk");
        Net rst = babies.createNet("rst");
        Net ce = babies.createNet("ce");

        // Replicate bebe Cells
        String[] dontConnect = new String[]{"CAS_OUT_ADDR", "CAS_OUT_BWE", "CAS_OUT_DBITERR", "clk", "rst", "ce",
                "CAS_OUT_DIN", "CAS_OUT_DOUT", "CAS_OUT_EN", "CAS_OUT_RDACCESS","CAS_OUT_RDB_WR", "CAS_OUT_SBITERR"};
        EDIFCell template = d.getTopEDIFCell().getCellInst("name[0].dut").getCellType();

        for (int i = 0; i < replicate_num; i++){

            Cell new_baby = babies.createCell("name["+i+"].dut", template);

            EDIFLibrary library = babies.getNetlist().getWorkLibrary();
            library.addCell(template);

            for(EDIFPort port : new_baby.getEDIFCellInst().getCellPorts()){
                String portName = port.isBus() ? port.getBusName() : port.getName();
                if (Arrays.stream(dontConnect).anyMatch(portName::contains))
                    continue;
                String ext_name = port.isBus()
                        ? port.getBusName() + "[" + i + "][" + (port.getWidth()-1) + ":0]"
                        : port.getName() + "_"+ i +"_";
                EDIFPort top_port = babies.getNetlist().getTopCell().createPort(ext_name, port.getDirection(), port.getWidth());

                for (int index = 0 ; index < top_port.getWidth(); index++){
                    String netName = port.isBus() ? port.getBusName() + "[" + i + "]" + "[" + index + "]" :  port.getBusName() + "_" + i + "_";
                    Net net = babies.createNet(netName);
                    if (port.isBus()){
                        net.getLogicalNet().createPortInst(top_port, index);
                        net.getLogicalNet().createPortInst(port, index, new_baby.getEDIFCellInst());
                    } else {
                        net.getLogicalNet().createPortInst(top_port);
                        net.getLogicalNet().createPortInst(port, new_baby.getEDIFCellInst());
                    }
                }
            }

            // connect global nets
            clk.getLogicalNet().createPortInst("clk", new_baby);
            rst.getLogicalNet().createPortInst("rst",new_baby);
            ce.getLogicalNet().createPortInst("ce", new_baby);
        }

        EDIFPort clkPort = babies.getTopEDIFCell().createPort("clk", EDIFDirection.INPUT, 1);
        EDIFPort rstPort = babies.getTopEDIFCell().createPort("rst", EDIFDirection.INPUT, 1);
        EDIFPort cePort = babies.getTopEDIFCell().createPort("ce", EDIFDirection.INPUT, 1);
        clk.getLogicalNet().createPortInst(clkPort);
        rst.getLogicalNet().createPortInst(rstPort);
        ce.getLogicalNet().createPortInst(cePort);

        return babies;
    }



    public static EDIFCell cloneEDIFCell (EDIFCell src, String suffix) {
        //EDIFCell clone = new EDIFCell(src.getLibrary(), src.getName() + suffix);
        Kryo kryo = new Kryo();
        EDIFCell clone = kryo.copy(src);
        clone.rename(src.getName() + suffix);

        return clone;
    }

    public static Properties getProperties() throws IOException {
        String config = System.getenv("RAPIDWRIGHT_PATH") + "/config.properties";
        Properties prop = new Properties();
        InputStream inputStream = new FileInputStream(config);
        prop.load(inputStream);
        return prop;
    }

    public static void matplot_visualize(String result_xdc){
        String python_script = System.getenv("RAPIDWRIGHT_PATH") + "/src/visualize/main.py";
        String cmd = "python3 " + python_script + " " + result_xdc;
        try{
            execute_cmd(cmd);
        } catch (Exception ignored){}
    }

    public static Design replicateSLR(String routedSLR){

        Design d = Design.readCheckpoint(routedSLR);

        Design design = new Design("SLRCopy", d.getPartName());
        design.setAutoIOBuffers(false);

        Module module = new Module(d);

        for (EDIFCell cell : d.getNetlist().getWorkLibrary().getCells()){
            design.getNetlist().getWorkLibrary().addCell(cell);
        }

        EDIFLibrary hdi = design.getNetlist().getHDIPrimitivesLibrary();
        for (EDIFCell cell : d.getNetlist().getHDIPrimitivesLibrary().getCells()){
            if (!hdi.containsCell(cell))
                hdi.addCell(cell);
        }

        ArrayList<Site> allValidPlacement = module.calculateAllValidPlacements(d.getDevice());

        // create global clock port
        EDIFPort clkPort = design.getTopEDIFCell().createPort("clk", EDIFDirection.INPUT, 1);
        EDIFCell top = design.getTopEDIFCell();

        System.out.println("SLR Replication: start...");
        for(Site anchor : allValidPlacement){
            EDIFCellInst ci = top.createChildCellInst(allValidPlacement.indexOf(anchor) + "_moduleInst", d.getTopEDIFCell());
            ModuleInst mi = design.createModuleInst(allValidPlacement.indexOf(anchor) + "_moduleInst", module);
            mi.setCellInst(ci);
            mi.connect("clk","clk");
            System.out.println("SLR Replication: placing SLR " + allValidPlacement.indexOf(anchor));
            mi.place(anchor);
        }

        System.out.println("SLR Replication: done");

        return design;
    }

    public static void printParameters() throws IOException {
        Properties prop = getProperties();
        System.out.println("==================Runtime Parameters====================");
        System.out.println("Using Device: " + prop.getProperty("device"));
        System.out.println("Hard Block Optimization Algorithm: " + prop.getProperty("method"));
        System.out.println("Proceed Optimization? " + prop.getProperty("optimization"));
        boolean opt = Boolean.parseBoolean(prop.getProperty("optimization"));
        if (!opt){
            System.out.println("Use previous placement file: " + prop.getProperty("placement"));
        }
        System.out.println("Use RapidWright for fast synthesis (RapidSynth)? " + prop.getProperty("rapidSynth"));
        System.out.println("Use SLR Replication? " + prop.getProperty("SLRCopy"));
        System.out.println("Use automatic pipeline? " + prop.getProperty("autoPipeline"));
        System.out.println("Pipeline Depth: " + prop.getProperty("pipelineDepth") + " (if auto-pipeline is enabled, this will be ignored)");
        System.out.println("Vivado verbose: " + prop.getProperty("vivado_verbose"));
        System.out.println("Generate placement GIF: " + prop.getProperty("generate_gif"));
        System.out.println("Enable transfer learning: " + prop.getProperty("transfer"));
        boolean transfer = Boolean.parseBoolean(prop.getProperty("transfer"));
        if (transfer)
            System.out.println("Transfer from placement file: " + prop.getProperty("initial_xdc"));

        System.out.println("========================================================");

    }

    public static void changeProperty( String key, String value) throws IOException {
        String root = System.getenv("RAPIDWRIGHT_PATH") + "/";
        String config = root + "config.properties";

        final File tmpFile = new File(config + ".tmp");
        final File file = new File(config);
        PrintWriter pw = new PrintWriter(tmpFile);
        BufferedReader br = new BufferedReader(new FileReader(file));
        boolean found = false;
        final String toAdd = key + '=' + value;
        for (String line; (line = br.readLine()) != null; ) {
            if (line.startsWith(key + '=')) {
                line = toAdd;
                found = true;
            }
            pw.println(line);
        }
        if (!found)
            pw.println(toAdd);
        br.close();
        pw.close();
        tmpFile.renameTo(file);
    }

    public static String createRegexFromGlob(String glob) {
        StringBuilder out = new StringBuilder("^");
        for(int i = 0; i < glob.length(); ++i) {
            final char c = glob.charAt(i);
            switch(c) {
                case '*': out.append(".*"); break;
                case '?': out.append('.'); break;
                case '.': out.append("\\."); break;
                case '\\': out.append("\\\\"); break;
                default: out.append(c);
            }
        }
        out.append('$');
        return out.toString();
    }

    /* debug computation unit replication */
    public static void main(String[] args) {
        String one_unit_path = System.getenv("RAPIDWRIGHT_PATH") + "/checkpoint/vu11p_seed_0.dcp";
        String output_path = System.getenv("RAPIDWRIGHT_PATH") + "/checkpoint/replicated.dcp";
        Design one_unit = Design.readCheckpoint(one_unit_path);
        Design babies = Tool.replicateConvBlocks(one_unit, 2);
        babies.writeCheckpoint(output_path);
    }
}
