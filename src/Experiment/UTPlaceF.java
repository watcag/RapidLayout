package Experiment;

import Utils.Utility;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.Site;
import main.Vivado;

import java.io.*;
import java.util.*;

public class UTPlaceF {
    private static List<Integer> dsp_cols = new ArrayList<>(Arrays.asList(
            1*2, 4*2, 5*2, 6*2, 8*2, 10*2, 11*2, 13*2, 15*2, 16*2, 17*2, 18*2, 21*2, 22*2, 24*2, 26*2, 28*2, 30*2,
            31*2, 33*2, 35*2, 37*2, 38*2, 39*2, 40*2, 42*2, 44*2, 45*2, 46*2, 47*2, 48*2, 51*2
    ));
    private static List<Integer> bram_cols = new ArrayList<>(Arrays.asList(
            2*2, 3*2, 7*2, 12*2, 14*2, 19*2, 20*2, 25*2, 27*2, 32*2, 34*2, 41*2, 49*2, 50*2
    ));
    private static List<Integer> uram_cols = new ArrayList<>(Arrays.asList(
            9*2, 23*2, 29*2, 36*2, 43*2
    ));

    private static String utplacer_root_folder = System.getProperty("user.home") + "/UTPlace";

    public static double[] calc_perf(String placePath) {
        // set up device
        String device = "vu11p";
        Device dev = new Design("name", device).getDevice();

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

        try (BufferedReader br = new BufferedReader(new FileReader(placePath))) {
            String line;
            int line_idx = 0;
            while ((line = br.readLine()) != null) {
                if (line_idx == 0) {line_idx ++; continue;}

                int inst_idx = Integer.parseInt(line.split(" ")[0].split("_")[1]) - 1;
                int block_idx = inst_idx / 28;
                inst_idx = inst_idx % 28;
                int col = Integer.parseInt(line.split(" ")[1]);
                float row = Integer.parseInt(line.split(" ")[2]);

                /*
                *   starting from inst_idx = 1, every conv has 2+18+8=28 hard blocks
                *   2 URAMs, 8 BRAMs, 18 DSPs, respectively
                */

                if (inst_idx >= 0 && inst_idx <= 1) { // URAMs
                    int X = uram_cols.indexOf(col);
                    int Y = (int) (row / 5);
                    Site uram_site = dev.getSite("URAM288_X" + X + "Y" + Y);
                    //System.out.println(uram_site.getName() + " block idx = " + block_idx);
                    temp_result.get(block_idx).get(2).add(uram_site);
                } else if (inst_idx > 1 && inst_idx <= 9) { // BRAMs
                    if (bram_cols.contains(col)) {
                        int X = bram_cols.indexOf(col);
                        int Y = (int)Math.round(row / 2.5);
                        Site s = dev.getSite("RAMB18_X" + X + "Y" + Y);
                        //System.out.println(s.getName() + " block idx = " + block_idx);
                        temp_result.get(block_idx).get(1).add(s);
                    } else {
                        int X = dsp_cols.indexOf(col);
                        int Y = (int)Math.round(row / 2.5);
                        Site s = dev.getSite("DSP48E2_X" + X + "Y" + Y);
                        //System.out.println(s.getName() + " block idx = " + block_idx);
                        temp_result.get(block_idx).get(1).add(s);
                    }

                } else { // DSP48E2
                    if (bram_cols.contains(col)) {
                        int X = bram_cols.indexOf(col);
                        int Y = (int)Math.round(row / 2.5);
                        Site s = dev.getSite("RAMB18_X" + X + "Y" + Y);
                        //System.out.println(s.getName() + " block idx = " + block_idx);
                        temp_result.get(block_idx).get(0).add(s);
                    } else {
                        int X = dsp_cols.indexOf(col);
                        int Y = (int)Math.round(row / 2.5);
                        Site s = dev.getSite("DSP48E2_X" + X + "Y" + Y);
                        //System.out.println(s.getName() + " block idx = " + block_idx);
                        temp_result.get(block_idx).get(0).add(s);
                    }
                }

                line_idx++;
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

        return new double[]{bbox, wl};
    }


    public static void main(String[] args) throws IOException {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));

        String result = System.getProperty("RAPIDWRIGHT_PATH") + "/result/";

        /* change this to specify VPR's placement result*/
        String vpr_place_path = utplacer_root_folder + "/conv.out";
        String output = System.getenv("RAPIDWRIGHT_PATH") + "/result/UTPlaceF_perf.txt";
        PrintWriter pr = new PrintWriter(new FileWriter(output, true), true);

        int times = 10;


        for (int i=0; i < times; i++) {

            String command = utplacer_root_folder + "/UTPlaceF_TCAD17 -aux " + utplacer_root_folder +
                    "/conv/design.aux -out " + utplacer_root_folder + "/conv.out";

            long start = System.currentTimeMillis();

            Vivado.vivado_cmd(command, false);

            long end = System.currentTimeMillis();

            double secs = (end - start) / 1e3;
            double[] perfs = calc_perf(vpr_place_path);

            pr.println(secs + " " + perfs[0] + " " + perfs[1]);

            System.out.println("time(secs) = " + secs + " average wirelength = "
                    + perfs[1] + " max half bbox perimeter = " + perfs[0]);

        }

        pr.close();

    }


}
