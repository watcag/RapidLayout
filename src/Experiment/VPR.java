package Experiment;

import Utils.Utility;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.Site;
import main.Tool;
import main.Vivado;

import java.io.*;
import java.util.*;

public class VPR {
    private static Integer[] dsp_col_index = new Integer[]{4, 16, 20, 24, 32, 40, 44, 52, 60, 64, 68, 72, 84, 88, 104, 112, 120, 124,
            132, 140,  148, 152, 156, 160, 168, 176, 180, 184, 188, 192, 200};
    private static Integer[] bram_col_index = new Integer[]{8, 12, 28, 48, 56, 76, 80, 100, 108, 128, 136, 164, 196, 198};
    private static Integer[] uram_col_index = new Integer[]{36, 92, 116, 144, 172};

    private static String vpr_root_folder = System.getProperty("user.home") + "/vtr_release/vpr";

    public static double[] calc_perf(String vpr_place_path) {
        // set up device
        String device = "vu11p";
        Device dev = new Design("name", device).getDevice();

        // create wildcard expression to filter hard blocks form .place file's rows
        String uramPattern1 = Tool.createRegexFromGlob("uram2_out_*");
        String uramPattern2 = Tool.createRegexFromGlob("bram1_in_*");
        String bramPattern = Tool.createRegexFromGlob("bram*_out_*");
        String dspPattern = Tool.createRegexFromGlob("dsp*_out_*");

        // Initialize col index lists
        List<Integer> dsp_col_idx = new ArrayList<>(Arrays.asList(dsp_col_index));
        List<Integer> bram_col_idx = new ArrayList<>(Arrays.asList(bram_col_index));
        List<Integer> uram_col_idx = new ArrayList<>(Arrays.asList(uram_col_index));

        // initialize placement map
        Map<Integer, List<List<Site>>> temp_result = new HashMap<>();
        for (int n=0; n < 80; n++) {
            List<List<Site>> typesofBlock = new ArrayList<>();
            for (int type=0; type<3; type++) {
                List<Site> sitesoftype = new ArrayList<>();
                typesofBlock.add(sitesoftype);
            }
            temp_result.put(n, typesofBlock);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(vpr_place_path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String name = line.split("\t")[0];

                boolean isURAM = name.matches(uramPattern1) || name.matches(uramPattern2);
                boolean isBRAM = name.matches(bramPattern);
                boolean isDSP = name.matches(dspPattern);

                int x, y;
                int vpr_x = 0, vpr_y = 0;
                int block_idx = 0;

                if (isDSP || isBRAM || isURAM) {
                    vpr_x = Integer.parseInt(line.split("\t")[1]);
                    vpr_y = Integer.parseInt(line.split("\t")[2]);
                    block_idx = Integer.parseInt(name.split("_")[2]);
                    block_idx -= 1;
                }

                // parse DSPs
                if (isDSP) {
                    x = dsp_col_idx.indexOf(vpr_x);
                    y = (vpr_y-1)/2;
                    Site dsp_site = dev.getSite("DSP48E2_X"+x+"Y"+y);
                    //System.out.println(dsp_site.getName() + " block idx = " + block_idx);
                    temp_result.get(block_idx).get(0).add(dsp_site);
                }
                // parse BRAMs
                if (isBRAM) {
                    x = bram_col_idx.indexOf(vpr_x);
                    y = (vpr_y-1)/2;
                    Site bram_site = dev.getSite("RAMB18_X"+x+"Y"+y);
                    //System.out.println(bram_site.getName() + " block idx = " + block_idx);
                    temp_result.get(block_idx).get(1).add(bram_site);
                }
                // parse URAMs
                if (isURAM) {
                    x = uram_col_idx.indexOf(vpr_x);
                    y = (vpr_y-1)/3;
                    Site uram_site = dev.getSite("URAM288_X"+x+"Y"+y);
                    //System.out.println(uram_site.getName() + " block idx = " + block_idx);
                    temp_result.get(block_idx).get(2).add(uram_site);
                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // generate VPR's placement result in our data structure
        Map<Integer, List<Site[]>> placement = new HashMap<>();
        for (Integer key : temp_result.keySet()) {
            List<Site[]> typesofBlock = new ArrayList<>();
            for (int type=0; type<3; type++) {
                List<Site> sitesofType = temp_result.get(key).get(type);
                typesofBlock.add(sitesofType.toArray(new Site[0]));
            }
            placement.put(key, typesofBlock);
        }


        Utility U = new Utility(placement, device);

        double wl = U.getUnifiedWireLength();
        double bbox = U.getMaxBBoxSize();

        System.out.println("average wirelength = " + U.getUnifiedWireLength() + " max half bbox perimeter = " + U.getMaxBBoxSize());

        return new double[]{bbox, wl};
    }

    public static void main(String[] args) throws IOException {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));

        String home = System.getProperty("user.home");

        /* change this to specify VPR's placement result*/
        String vpr_place_path = vpr_root_folder + "/conv.place";
        String output = System.getenv("RAPIDWRIGHT_PATH") + "/result/VPR_perf.txt";
        PrintWriter pr = new PrintWriter(new FileWriter(output, true), true);

        int times = 10;


        for (int i=0; i < times; i++) {

            String command = vpr_root_folder + "/vpr " + vpr_root_folder +
                    "/my_arch.xml " + vpr_root_folder + "/conv.blif -nodisp -place -seed " + i;

            long start = System.currentTimeMillis();

            Vivado.vivado_cmd(command, false);

            long end = System.currentTimeMillis();

            double secs = (end - start) / 1e3;
            double[] perfs = calc_perf(vpr_place_path);

            pr.println(secs + " " + perfs[0] + " " + perfs[1]);

        }

        pr.close();

    }
}
