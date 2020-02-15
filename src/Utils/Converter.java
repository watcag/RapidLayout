package Utils;

import Opt.DoubleGenotypeRe;
import Opt.PlaceDecoder;
import Opt.PlaceGenotype;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.device.SiteTypeEnum;
import main.Tool;
import org.opt4j.core.Genotype;
import org.opt4j.core.genotype.CompositeGenotype;
import org.opt4j.core.genotype.PermutationGenotype;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class Converter {

    private static final SiteTypeEnum DSP_SITE_TYPE = SiteTypeEnum.DSP48E2;
    private static final SiteTypeEnum BRAM_SITE_TYPE = SiteTypeEnum.RAMB180;
    private static final SiteTypeEnum URAM_SITE_TYPE = SiteTypeEnum.URAM288;
    private static final SiteTypeEnum DSP_LOC = SiteTypeEnum.BITSLICE_TX;
    private static final SiteTypeEnum BRAM_LOC = SiteTypeEnum.FIFO36;
    private static final SiteTypeEnum URAM_LOC = SiteTypeEnum.SYSMONE1;
    private static final SiteTypeEnum DSP_MAP = SiteTypeEnum.SLICEL;
    private static final SiteTypeEnum BRAM_MAP = SiteTypeEnum.BUFG;
    private static final SiteTypeEnum URAM_MAP = SiteTypeEnum.LAGUNA;

    public static int getColHeight(String device, int ymax, int type) {
        Device dev = new Design("name", device).getDevice();
        List<Site> compatSites = new ArrayList<>();
        switch (type){
            case 0: // dsp
                Site[] sites = dev.getAllSitesOfType(SiteTypeEnum.DSP48E2);
                compatSites.addAll(Arrays.asList(sites));
                break;
            case 1: // bram
                Site[] bram180 = dev.getAllSitesOfType(SiteTypeEnum.RAMB180);
                Site[] bram181 = dev.getAllSitesOfType(SiteTypeEnum.RAMB181);
                compatSites.addAll(Arrays.asList(bram180));
                compatSites.addAll(Arrays.asList(bram181));
                break;

            default: // uram
                Site[] urams = dev.getAllSitesOfType(SiteTypeEnum.URAM288);
                compatSites.addAll(Arrays.asList(urams));
                break;
        }

        List<Site> oneCol = new ArrayList<>();
        for (Site s : compatSites)
            if (s.getInstanceX() == 0 && s.getRpmY() <= ymax)
                oneCol.add(s);

        if (type == 1)
            return oneCol.size() * 2; // because BRAMs are interleaved

        return oneCol.size();
    }

    public static List<Site> getSitesOfType(String device, int type) {
        Device dev = new Design("name", device).getDevice();
        List<Site> compatSites = new ArrayList<>();
        switch (type){
            case 0: // dsp
                Site[] sites = dev.getAllSitesOfType(SiteTypeEnum.DSP48E2);
                compatSites.addAll(Arrays.asList(sites));
                break;
            case 1: // bram
                Site[] bram180 = dev.getAllSitesOfType(SiteTypeEnum.RAMB180);
                Site[] bram181 = dev.getAllSitesOfType(SiteTypeEnum.RAMB181);
                compatSites.addAll(Arrays.asList(bram180));
                compatSites.addAll(Arrays.asList(bram181));
                break;

            default: // uram
                Site[] urams = dev.getAllSitesOfType(SiteTypeEnum.URAM288);
                compatSites.addAll(Arrays.asList(urams));
                break;
        }

        return compatSites;
    }

    public static int matchGroup(Site[] target, List<List<Site[]>> sortedGroups) {
        int i = 0;
        for (List<Site[]> thisCol : sortedGroups) {
            for (Site[] group : thisCol) {
                if (Arrays.equals(group, target)) return i;
                i += 1;
            }
        }
        return i;
    }

    public static CompositeGenotype<SiteTypeEnum, Genotype> convert(Map<Integer, List<Site[]>> result, String device,
                                                                    int block_num, int x_min, int y_min, int x_max, int y_max) {

        // number of cascaded hard blocks (grouping)
        int dsp_cas = 9;
        int bram_cas = 4;
        int uram_cas = 2;
        // number of groups per conv unit
        int dsp_group = 2;
        int bram_group = 2;
        int uram_group = 1;

        /* result map structure:
         -  Key = Integer, conv block number
         -  Value = List<Site[]>, {dsp_sites[18], bram_sites[8], uram_sites[2]}
         */

        /*
            - distribution size: number of hard block columns
            - location sizes: dsp: 2*blockn, bram: 2*blockn, uram: blockn, because we select by group
            - mapping size:   dsp* 2*blockn, bram: 2*blockn, uram: blockn, because we map by group
         */

        // get lists of grouped hard blocks
        List<Site[]> dsp_groups = new ArrayList<>(); // size: length(dsp_groups) = blockn * 2, len(Site[]) = dsp_cas
        List<Site[]> bram_groups = new ArrayList<>();
        List<Site[]> uram_groups = new ArrayList<>();
        for (int n = 0; n < result.size(); n++ ) {
            List<Site[]> thisConv = result.get(n);
            List<Site> dspGroups = Arrays.asList(thisConv.get(0));
            List<Site> bramGroups = Arrays.asList(thisConv.get(1));
            List<Site> uramGroups = Arrays.asList(thisConv.get(2));
            for (int i = 0; i < dsp_group; i++)
                dsp_groups.add(dspGroups.subList(i * dsp_cas, (i+1) * dsp_cas)
                        .toArray(new Site[0]));
            for (int i = 0; i < bram_group; i++)
                bram_groups.add(bramGroups.subList(i * bram_cas, (i+1) * bram_cas)
                        .toArray(new Site[0]));
            for (int i = 0; i < uram_group; i++)
                uram_groups.add(uramGroups.subList(i * uram_cas, (i+1) * uram_cas)
                        .toArray(new Site[0]));
        }

        /* calculate distribution */
        // sort sites according to column
        List<List<Double>> distribution = new ArrayList<>(); // SiteType/an array of number
        List<List<Site[]>> allGroups = new ArrayList<>(); // SiteType/groups
        allGroups.add(dsp_groups); allGroups.add(bram_groups); allGroups.add(uram_groups);
        List<List<List<Site[]>>> sortedGroups = new ArrayList<>(); // type / column / list of groups
        List<List<List<Double>>> locationsByCol = new ArrayList<>(); // type / column / location
        List<List<Double>> locations = new ArrayList<>(); // type / locations


        for (int type = 0; type < 3; type++) {
            List<List<Site>> allColumns = new ArrayList<>();
            List<Site> allSites = new ArrayList<>(); int maxCol = 0;
            List<Site[]> groups = allGroups.get(type);

            for (Site[] group : groups)
                allSites.addAll(Arrays.asList(group));

            // determine max column
            for (Site s : getSitesOfType(device, type)) {
                int col = s.getInstanceX();
                if (col > maxCol)
                    maxCol = col;
            }

            // initialize new empty columns
            for (int i=0; i <= maxCol; i++)
                allColumns.add(new ArrayList<>());

            // put all sites in corresponding columns
            for (Site s : allSites){
                int col = s.getInstanceX();
                allColumns.get(col).add(s);
            }

            List<Double> distribution_thisType = new ArrayList<>();
            for (List<Site> col : allColumns) {
                double size = col.size();
                distribution_thisType.add(size);
            }
            // normalize
            double sum = 0;
            for (Double v : distribution_thisType)
                sum += v;
            for (int i = 0; i < distribution_thisType.size(); i++){
                double orig = distribution_thisType.get(i);
                distribution_thisType.set(i, orig / sum);
            }

            distribution.add(distribution_thisType); // distribution genotype

            /*--------------- location ------------------*/
            List<List<Site[]>> sortedGroups_thisType = new ArrayList<>();
            for (int i = 0; i <= maxCol; i++)
                sortedGroups_thisType.add(new ArrayList<>());
            // put groups into each column
            for (Site[] group : groups) {
                int col = group[0].getInstanceX();
                sortedGroups_thisType.get(col).add(group);
            }
            // go through each column and sort groups according to its head
            // we know that sites within each group are ordered
            for (List<Site[]> groupsInCol : sortedGroups_thisType) {
                // sort groupsInCol
                groupsInCol.sort((g0, g1) -> {
                    // ascending order
                    int Y0 = g0[0].getInstanceY();
                    int Y1 = g1[0].getInstanceY();
                    return Y0 - Y1;
                });
            }

            sortedGroups.add(sortedGroups_thisType);

            // calculate locationsByCol in each column
            int colHeight = getColHeight(device, y_max, type);
            List<List<Double>> locations_thsType = new ArrayList<>();
            for (int col = 0; col <= maxCol; col++){
                locations_thsType.add(new ArrayList<>()); // add a new column
                List<Site[]> groupsThisCol = sortedGroups_thisType.get(col);
                for (Site[] group : groupsThisCol) {
                    int head = group[0].getInstanceY();
                    double location = (double)head / (double)colHeight;
                    locations_thsType.get(col).add(location);
                }
            }
            // concat locations in col to one list
            List<Double> concatLocations = new ArrayList<>();
            for (List<Double> col : locations_thsType)
                if (col.size() > 0)
                    concatLocations.addAll(col);

            locationsByCol.add(locations_thsType);
            locations.add(concatLocations);
        }

        // get mapping from sorted and original sites
        // List<List<Site[]>> allGroups = new ArrayList<>(); // SiteType/groups
        // List<List<List<Site[]>>> sortedGroups = new ArrayList<>(); // type / column / list of groups

        List<List<Integer>> mapping = new ArrayList<>(); // type / mapping list
        for (int type = 0; type < 3; type++) {
            List<Site[]> groupsByConv = allGroups.get(type);
            List<List<Site[]>> sortedGroups_thisType = sortedGroups.get(type);
            List<Integer> mapping_thisType = new ArrayList<>();
            for (Site[] target : groupsByConv) {
                int i = matchGroup(target, sortedGroups_thisType);
                mapping_thisType.add(i);
            }
            // re-order mapping for DSP and BRAM
            List<Integer> re_ordered = new ArrayList<>();
            if (type <= 1) {
                for (int i=0; i < mapping_thisType.size(); i+=2)
                    re_ordered.add(mapping_thisType.get(i));
                for (int i=1; i < mapping_thisType.size(); i+=2)
                    re_ordered.add(mapping_thisType.get(i));
            } else
                re_ordered.addAll(mapping_thisType);
            mapping.add(re_ordered);
        }

        // construct genotype
        Device dev = new Design("name", device).getDevice();
        CompositeGenotype<SiteTypeEnum, Genotype> genotype = new CompositeGenotype<>();
        Map<SiteTypeEnum, List<List<Site>>> map = Opt.PlaceCreator.getAvailableSites(dev, x_min, x_max, y_min, y_max);


        // distribution
        PlaceGenotype<Site> dspColNum = new PlaceGenotype<>(map.get(DSP_SITE_TYPE));
        PlaceGenotype<Site> bramColNum = new PlaceGenotype<>(map.get(BRAM_SITE_TYPE));
        PlaceGenotype<Site> uramColNum = new PlaceGenotype<>(map.get(URAM_SITE_TYPE));
        // location
        DoubleGenotypeRe dspLoc = new DoubleGenotypeRe();
        DoubleGenotypeRe bramLoc = new DoubleGenotypeRe();
        DoubleGenotypeRe uramLoc = new DoubleGenotypeRe();
        // mapping
        PermutationGenotype<Integer> dspMapping = new PermutationGenotype<>();
        PermutationGenotype<Integer> bramMapping = new PermutationGenotype<>();
        PermutationGenotype<Integer> uramMapping = new PermutationGenotype<>();

        // initialization

        dspColNum.init(distribution.get(0));
        bramColNum.init(distribution.get(1));
        uramColNum.init(distribution.get(2));

        dspLoc.init(locations.get(0));
        bramLoc.init(locations.get(1));
        uramLoc.init(locations.get(2));

        dspMapping.addAll(mapping.get(0));
        bramMapping.addAll(mapping.get(1));
        uramMapping.addAll(mapping.get(2));

        genotype.put(DSP_SITE_TYPE, dspColNum);
        genotype.put(BRAM_SITE_TYPE, bramColNum);
        genotype.put(URAM_SITE_TYPE, uramColNum);

        genotype.put(DSP_LOC, dspLoc);
        genotype.put(BRAM_LOC, bramLoc);
        genotype.put(URAM_LOC, uramLoc);

        genotype.put(DSP_MAP, dspMapping);
        genotype.put(BRAM_MAP, bramMapping);
        genotype.put(URAM_MAP, uramMapping);

        return genotype;

    }


    public static void main(String[] args) throws IOException {

        String root = System.getenv("RAPIDWRIGHT_PATH") + "/";

        String device = "vu11p";
        int blockn = 80;

        String orig = root + "result/blockNum=" + blockn + ".xdc";
        String decoded_xdc = root + "result/blockNum=" + blockn + "_decoded.xdc";

        Map<Integer, List<Site[]>> result = Tool.getMapFromXDC(orig, device);

        int x_min = 0;
        int y_min = 0;
        int x_max = 6000;
        int y_max = 240;

        CompositeGenotype<SiteTypeEnum, Genotype> genotype =
                convert(result, device, blockn, x_min, y_min, x_max, y_max);

        PlaceDecoder decoder = new PlaceDecoder();
        Map<Integer, List<Site[]>> decoded = decoder.decode(genotype);

        PrintWriter pr = new PrintWriter(new FileWriter(decoded_xdc), true);
        Tool.write_XDC(decoded, pr);
        pr.close();

        String visual1 = "python3 " + root + "src/visualize/overall_visual.py " + orig + " " + root + "result/visual/";
        String visual2 = "python3 " + root + "src/visualize/overall_visual.py " + decoded_xdc + " " + root + "result/visual/";

        Tool.execute_cmd(visual1);
        Tool.execute_cmd(visual2);
    }

}
