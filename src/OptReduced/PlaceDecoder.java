package OptReduced;

import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.device.SiteTypeEnum;
import org.opt4j.core.Genotype;
import org.opt4j.core.genotype.CompositeGenotype;
import org.opt4j.core.problem.Decoder;

import java.util.*;

// About this phenotype structure:
/*
    This phenotype is a map, whose key is of type Integer, value is of type List<Site[]>
    Each value corresponds to the placement strategy of one block
    Each item in list is an array on Site, which corresponds to one block's DSP, BRAM, URAM's placement
 */

public class PlaceDecoder implements Decoder<CompositeGenotype<SiteTypeEnum, Genotype>, Map<Integer, List<Site[]>>> {
    int n_dsp = 9;
    int n_bram = 4;
    int n_uram = 2;

    final SiteTypeEnum DSP_MAP = SiteTypeEnum.SLICEL;
    final SiteTypeEnum BRAM_MAP = SiteTypeEnum.BUFG;
    final SiteTypeEnum URAM_MAP = SiteTypeEnum.LAGUNA;

    public Map<Integer, List<Site[]>> decode(CompositeGenotype<SiteTypeEnum, Genotype> genotype) {
        Map<Integer, List<Site[]>> result = new HashMap<>();

        SiteTypeEnum[] siteTypes = new SiteTypeEnum[]{DSP_MAP, BRAM_MAP, URAM_MAP};
        // because each conv block only has one group of URAM so block number is the same as the number of
        // URAM groups in mapping genotype
        int block_num = ((PlaceGenotype<Integer>) genotype.get(URAM_MAP)).size();

        /* select physical sites in uniform distribution */

        // List<Site[]> each Site[] is a group of physical sites for one group of logical sites
        Map<SiteTypeEnum, List<Site[]>> selected_sites = new HashMap<>();
        // how many groups of hard blocks you need
        Integer[] numberOfSites = new Integer[]{2 * block_num, 2 * block_num, block_num};
        // how many hard blocks in each group
        Integer[] inGroup = new Integer[]{n_dsp, n_bram, n_uram};

        for (SiteTypeEnum siteType : siteTypes) {
            // available sites for current site type
            List<List<Site>> availSites = ((PlaceGenotype<Integer>) genotype.get(siteType)).getSites();
            List<Site[]> selected_sites_for = new ArrayList<>(); // to store chosen sites for this type
            // calculate how many you should choose on each column
            Integer index = Arrays.asList(siteTypes).indexOf(siteType);
            Integer perCol = numberOfSites[index] / (availSites.size());
            Integer mod = numberOfSites[index] % (availSites.size());

            List<Integer> group_num_per_col = new ArrayList<>();
            for (int i = 0; i < availSites.size(); i++) {
                int num = i > mod - 1 ? perCol : perCol + 1;
                group_num_per_col.add(num);
            }

            if (siteType == BRAM_MAP) {
                // because you have to interleave BRAMs so it is kinda special
                for (List<Site> thisColSite : availSites) {
                    int col_idx = availSites.indexOf(thisColSite);
                    int num = group_num_per_col.get(col_idx);
                    for (int i = 0; i < num; i++) {
                        Site[] group = i % 2 == 0
                                ? new Site[]{thisColSite.get(4 * i), thisColSite.get(4 * i + 2), thisColSite.get(4 * i + 4), thisColSite.get(4 * i + 6)} // even
                                : new Site[]{thisColSite.get(4 * i - 3), thisColSite.get(4 * i - 1), thisColSite.get(4 * i + 1), thisColSite.get(4 * i + 3)};
                        selected_sites_for.add(group);
                    }
                }

            } else {
                // URAM and DSP you can just choose them continuously
                for (List<Site> thisColSite : availSites) {
                    int col_idx = availSites.indexOf(thisColSite);
                    int num = group_num_per_col.get(col_idx);
                    for (int i = 0; i < num; i++) { // group index
                        List<Site> group = new ArrayList<>();
                        for (int j = 0; j < inGroup[index]; j++) {
                            group.add(thisColSite.get(i * inGroup[index] + j));
                        }
                        // add this group to save list
                        selected_sites_for.add(group.toArray(new Site[0]));
                    }

                }
            }

            // collect selected groups of sites for each type
            selected_sites.put(siteType, selected_sites_for);
        }

        /* --- Mapping ----- */
        result = Mapping(block_num, selected_sites, genotype);

        return result;
    }


    public Map<Integer, List<Site[]>> Mapping(int block_num, Map<SiteTypeEnum, List<Site[]>> chosenSites, CompositeGenotype<SiteTypeEnum, Genotype> genotype) {
        Map<Integer, List<Site[]>> map = new HashMap<>(); // stores the final configuration for each block
        SiteTypeEnum[] types = new SiteTypeEnum[]{DSP_MAP, BRAM_MAP, URAM_MAP};
        Integer[] lengths = new Integer[]{2, 2, 1};
        List[] mapping = new List[]{
                (List<Integer>) genotype.get(DSP_MAP),
                (List<Integer>) genotype.get(BRAM_MAP),
                (List<Integer>) genotype.get(URAM_MAP)};
        for (int i = 0; i < block_num; i++) {
            List<Site[]> this_block = new ArrayList<>();
            for (int type = 0; type < types.length; type++) {
                List<Site> sites = new ArrayList<>(); // chosen sites for current hard block and specific type

                for (int index = i * lengths[type]; index < (i+1) * lengths[type]; index++){
                    int group_index = (Integer)mapping[type].get(index);
                    sites.addAll(Arrays.asList(chosenSites.get(types[type]).get(group_index)));
                }

                this_block.add(sites.toArray(new Site[0]));
            }
            map.put(i, this_block);
        }

        return map;

    }




}
