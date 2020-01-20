package Opt;

import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.device.SiteTypeEnum;
import org.opt4j.core.Genotype;
import org.opt4j.core.genotype.CompositeGenotype;
import org.opt4j.core.genotype.DoubleGenotype;
import org.opt4j.core.genotype.PermutationGenotype;
import org.opt4j.core.problem.Decoder;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.floor;
import static java.lang.Math.min;

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
    final SiteTypeEnum DSP_SITE_TYPE = SiteTypeEnum.DSP48E2;
    final SiteTypeEnum BRAM_SITE_TYPE = SiteTypeEnum.RAMB180;
    final SiteTypeEnum BRAM_SITE_TYPE2 = SiteTypeEnum.RAMB181;
    final SiteTypeEnum URAM_SITE_TYPE = SiteTypeEnum.URAM288;
    final SiteTypeEnum DSP_LOC = SiteTypeEnum.BITSLICE_TX;
    final SiteTypeEnum BRAM_LOC = SiteTypeEnum.FIFO36;
    final SiteTypeEnum URAM_LOC = SiteTypeEnum.SYSMONE1;
    final SiteTypeEnum DSP_MAP = SiteTypeEnum.SLICEL;
    final SiteTypeEnum BRAM_MAP = SiteTypeEnum.BUFG;
    final SiteTypeEnum URAM_MAP = SiteTypeEnum.LAGUNA;

    public Map<Integer, List<Site[]>> decode(CompositeGenotype<SiteTypeEnum, Genotype> genotype) {
        PlaceGenotype<Site> dspColNums = genotype.get(DSP_SITE_TYPE);
        PlaceGenotype<Site> bramColNums = genotype.get(BRAM_SITE_TYPE);
        PlaceGenotype<Site> uramColNums = genotype.get(URAM_SITE_TYPE);
        List<Integer> n = new ArrayList<>();
        n.add(n_dsp);
        n.add(n_bram);
        n.add(n_uram);
        List<PlaceGenotype<Site>> genotypes = new ArrayList<>();
        genotypes.add(dspColNums);
        genotypes.add(bramColNums);
        genotypes.add(uramColNums);

        // this is only because one conv block has 1 URAM group. Be careful of this equation
        int block_num = ((PermutationGenotype<Integer>) genotype.get(URAM_MAP)).size();

        // Decode Column Distribution: how many blocks at each column
        Map<PlaceGenotype<Site>, List<Integer>> col_config = new HashMap<>();
        for (int gene_index = 0; gene_index < 3; gene_index++) {
            int group_num = gene_index < 2 ? block_num * 2 : block_num;
            PlaceGenotype<Site> gene = genotypes.get(gene_index);
            List<List<Site>> available = gene.getSites();
            // calc max blocks in one column
            int max_blocks = available.get(0).size() / n.get(gene_index);
            // calc number of blocks on each column
            double sum = 0;
            for (double d : gene)
                sum += d;
            List<Integer> each_col = new ArrayList<>(); // number of blocks on each column
            int int_sum = 0;
            for (int i = 0; i < available.size(); i++) {
                double exact = gene.get(i) / sum * group_num;
                int approx = (int) floor(exact);
                approx = min(approx, max_blocks);
                each_col.add(approx);
                int_sum += approx;
            }
            // handle rounding error
            int adding_count = 0;
            while (adding_count < group_num - int_sum) {
                int col_index = 0;
                while (col_index < available.size()) {
                    if (each_col.get(col_index) + 1 <= max_blocks){
                        each_col.set(col_index, each_col.get(col_index) + 1);
                        adding_count += 1;
                    }
                    col_index++;
                    if (adding_count == group_num - int_sum) break;
                }
                //System.out.println("in while: group number = " + group_num + " int_sum = " + int_sum + " max_per_block = " + max_blocks);
            }
            col_config.put(gene, each_col);

            // Observe Distribution
            /*
            for (int i = 0; i < each_col.size(); i++)
                System.out.print(each_col.get(i) + " ");
            System.out.print("\n");*/
            //System.out.println("int_sum = "+int_sum+" block_num = "+block_num);
        }
        //System.out.println("------------------");

        // pack
        List<Integer> dsp_cols = col_config.get(dspColNums);
        List<Integer> bram_cols = col_config.get(bramColNums);
        List<Integer> uram_cols = col_config.get(uramColNums);

        // Select Specific Locations at each Column
        List[] distribution = new List[]{dsp_cols, bram_cols, uram_cols};
        Integer[] needed = new Integer[]{block_num * 2, block_num * 2, block_num};
        // check if chosen sites are enough
        for (int i = 0; i < distribution.length; i++) {
            int sum = 0;
            List<Integer> list = distribution[i];
            for (Integer perCol : list)
                sum += perCol;
            if (sum < needed[i])
                System.out.println("Didn't select enough sites, maybe initial search  area is too small, type = " + i);
            else if (sum > needed[i])
                System.out.println("Too many sites are selected");
        }


        Map<SiteTypeEnum, List<Site[]>> chosenSites = selectLocations(distribution, genotype);

        // stores the final configuration for each block
        Map<Integer, List<Site[]>> map = Mapping(block_num, chosenSites, genotype);

        return map;
    }

    /*
        return type: all selected DSP sites organized in column fashion, all selected BRAM & URAM in column fashion
        input distribution: List of List of Integers, representing how many blocks should be put at each column for each type of site
     */
    public Map<SiteTypeEnum, List<Site[]>> selectLocations(List[] distribution, CompositeGenotype<SiteTypeEnum, Genotype> genotype) {
        SiteTypeEnum[] types = new SiteTypeEnum[]{DSP_SITE_TYPE, BRAM_SITE_TYPE, URAM_SITE_TYPE};
        SiteTypeEnum[] loc_keys = new SiteTypeEnum[]{DSP_LOC, BRAM_LOC, URAM_LOC};
        List[] allSites = new List[]{
                ((PlaceGenotype<Site>) genotype.get(DSP_SITE_TYPE)).getSites(),
                ((PlaceGenotype<Site>) genotype.get(BRAM_SITE_TYPE)).getSites(),
                ((PlaceGenotype<Site>) genotype.get(URAM_SITE_TYPE)).getSites()};
        // the map to keep selected sites
        Map<SiteTypeEnum, List<Site[]>> map = new HashMap<>();
        // traverse through each type
        for (int type = 0; type < types.length; type++) {
            // keep track of selected sites for each type
            List<Site[]> thisType = new ArrayList<>();
            int start = 0; // select
            // go through each column
            for (int col_idx = 0; col_idx < allSites[type].size(); col_idx++) {
                // selected sites in current column
                List<Site> thisCol = new ArrayList<>();
                // how many sites are in this column
                int num_sites = (int) distribution[type].get(col_idx); // how many group of sites to put in current column

                // DEBUG
                if (start + num_sites - 1 > ((DoubleGenotype) genotype.get(loc_keys[type])).size()) {
                    System.out.println("type = " + type);
                    int sum = 0;
                    for (int ok : (List<Integer>) distribution[type])
                        sum += ok;
                    System.out.println("sum of distribution = " + sum);
                    System.out.println("col index = " + col_idx);
                    System.out.println("length[type] * num_sites = " + num_sites);
                    System.out.println("size = " + ((DoubleGenotype) genotype.get(loc_keys[type])).size());

                }

                List<Double> locations = ((DoubleGenotype) genotype.get(loc_keys[type])).subList(start, start + num_sites); // location genotype
                List<Site> thisColAvail = (List<Site>) allSites[type].get(col_idx);
                int siteNumofCol = thisColAvail.size(); // how many elements available in this column
                List<Integer> heads = locations.stream().map(x -> (int) floor(siteNumofCol * x)).sorted().collect(Collectors.toList());
                if (types[type] == DSP_SITE_TYPE) {
                    // resolve conflicts
                    List<Integer> containedHeads = new ArrayList<>(contain(heads, siteNumofCol, n_dsp));
                    for (Integer head : containedHeads)
                        thisCol.addAll((thisColAvail).subList(head, head + n_dsp));
                } else if (types[type] == BRAM_SITE_TYPE) {
                    // resolve conflicts
                    List<Integer> containedHeads =containBRAM(heads, siteNumofCol, n_bram);
                    for (Integer head : containedHeads)
                        for (int i = 0; i < n_bram * 2; i += 2)
                            thisCol.add(thisColAvail.get(head + i)); /*problem occurs*/

                } else {
                    // resolve conflicts
                    List<Integer> containedHeads = new ArrayList<>( contain(heads, siteNumofCol, n_uram));
                    for (Integer head : containedHeads)
                        thisCol.addAll((thisColAvail).subList(head, head + n_uram));
                }
                thisType.add(thisCol.toArray(new Site[0]));
                start += num_sites;
            }
            map.put(types[type], thisType);
        }

        return map;
    }

    public Map<Integer, List<Site[]>> Mapping(int block_num, Map<SiteTypeEnum, List<Site[]>> chosenSites, CompositeGenotype<SiteTypeEnum, Genotype> genotype) {
        Map<Integer, List<Site[]>> map = new HashMap<>(); // stores the final configuration for each block
        SiteTypeEnum[] types = new SiteTypeEnum[]{DSP_SITE_TYPE, BRAM_SITE_TYPE, URAM_SITE_TYPE};
        Integer[] lengths = new Integer[]{n_dsp, n_bram, n_uram};
        List[] mapping = new List[]{
                (List<Integer>) genotype.get(DSP_MAP),
                (List<Integer>) genotype.get(BRAM_MAP),
                (List<Integer>) genotype.get(URAM_MAP)};
        for (int i = 0; i < block_num; i++) {
            List<Site[]> this_block = new ArrayList<>();
            for (int type = 0; type < types.length; type++) {
                List<Site> sites = new ArrayList<>(); // chosen sites for current hard block and specific type
                if (types[type] != URAM_SITE_TYPE) {
                    Integer[] index0 = findIndex(chosenSites, types[type], (int) mapping[type].get(i), lengths[type]);
                    Integer[] index1 = findIndex(chosenSites, types[type], (int) mapping[type].get(i + block_num), lengths[type]);
                    // DEBUG
                    if (index0[1] * lengths[type] + lengths[type] > chosenSites.get(types[type]).get(index0[0]).length) {
                        System.out.println("type = " + type);
                        System.out.println("col index = " + index0[0] + " row index = " + index0[1]);
                        System.out.println("number = " + (int) mapping[type].get(i));
                    }
                    if (index1[1] * lengths[type] + lengths[type] > chosenSites.get(types[type]).get(index1[0]).length) {
                        System.out.println("type = " + type);
                        System.out.println("col index = " + index1[0] + " row index = " + index1[1]);
                        System.out.println("number = " + (int) mapping[type].get(i + block_num));
                    }

                    sites.addAll(Arrays.asList(chosenSites.get(types[type]).get(index0[0])).subList(index0[1] * lengths[type], index0[1] * lengths[type] + lengths[type]));
                    sites.addAll(Arrays.asList(chosenSites.get(types[type]).get(index1[0])).subList(index1[1] * lengths[type], index1[1] * lengths[type] + lengths[type]));

                } else {
                    Integer[] index0 = findIndex(chosenSites, types[type], (int) mapping[type].get(i), lengths[type]);
                    sites.addAll(Arrays.asList(chosenSites.get(types[type]).get(index0[0])).subList(index0[1] * lengths[type], index0[1] * lengths[type] + lengths[type]));
                }

                // must complete place and mapping before this point
                this_block.add(sites.toArray(new Site[0]));
            }
            map.put(i, this_block);
        }

        return map;

    }

    public Integer[] findIndex(Map<SiteTypeEnum, List<Site[]>> chosenSites, SiteTypeEnum type, Integer dst, Integer elem_size) {
        // find which column we at
        int block_sum = 0;
        int curr_col = 0;
        for (int col_index = 0; col_index < chosenSites.get(type).size(); col_index++) {
            int num = chosenSites.get(type).get(col_index).length / elem_size;
            if (block_sum + num > dst) {
                curr_col = col_index;
                break;
            }
            block_sum += num;
        }
        int idx_in_col = dst - block_sum; // the index of block inside current column

        return new Integer[]{curr_col, idx_in_col};
    }

    public List<Integer> contain(List<Integer> input, int col_size, int ele_size) {
        List<Integer> contained = new ArrayList<>();
        if (input.size() * ele_size > col_size)
            System.out.println("Trying to place too many blocks in one column, it's impossible");
        if (input.size() * ele_size == col_size) {
            for (int i = 0; i < input.size(); i++)
                contained.add(i * ele_size);
            return contained;
        }
        if (input.size() == 0) return contained;
        // required elements should always be less than available beyond this point
        for (int i = 0; i < input.size(); i++) {
            int need = (input.size() - (i + 1)) * ele_size;
            int avail = col_size - (input.get(i) + ele_size + 1);
            if (need > avail) {
                int back = need - avail;
                if (back > 0 && input.get(i) == 0) System.out.println("Cannot move element back anymore!");
                else if (input.get(i) - back < 0) System.out.println("backing up too much!");
                else
                    contained.add(input.get(i) - back);
            } else {
                if (i == 0)
                    contained.add(input.get(i));
                else if (input.get(i) >= contained.get(i - 1) + ele_size)
                    contained.add(input.get(i));
                else
                    contained.add(contained.get(i - 1) + ele_size);
            }
        }

        Collections.sort(contained);

        /* check if the result is legal */
        boolean overlap = false;
        for (int i = 0; i < contained.size() - 1; i++) {
            if (contained.get(i) + ele_size > contained.get(i + 1))
                overlap = true;
        }
        if (overlap)
            System.out.println("Containing failure: overlap is true");
        if (input.size() != contained.size())
            System.out.println("Containing failure: input and output size do not match");
        if (contained.get(contained.size() - 1) >= col_size - 1)
            System.out.println("Exceeds column limit");
        return contained;
    }

    public List<Integer> containBRAM(List<Integer> input, int col_size, int ele_size) {
        Collections.sort(input);
        List<Integer> contained = new ArrayList<>();
        if (input.size() * ele_size > col_size)
            System.out.println("Trying to place too many blocks in one column, it's impossible");
        if (input.size() == 0) return contained;
        if (input.size() == col_size / ele_size){
            for (int i = 0 ; i < input.size() * ele_size; i += 8){
                contained.add(i);
                contained.add(i+1);
            }
            return contained;
        }
        // required elements should always be less than available beyond this point
        // we should treat even and odd heads differently
        List<Integer> even = new ArrayList<>();
        List<Integer> odd = new ArrayList<>();
        for (Integer i : input)
            if (i % 2 == 0) even.add(i);
            else odd.add(i);
        // if even or odd has too many elements, put it in the other side
        if (even.size() > col_size / 8){
            Collections.sort(even);
            int overflow = even.size() - col_size / 8;
            for (int i = 0; i < overflow; i++){
                int temp = even.get(even.size()-1 - i);
                even.remove(even.size()-1 - i);
                odd.add(temp - 1);
            }
        }
        if (odd.size() > col_size / 8){
            Collections.sort(odd);
            int overflow = odd.size() - col_size / 8;
            for (int i = 0; i < overflow; i++){
                int temp = odd.get(odd.size()-1 - i);
                odd.remove(odd.size()-1 - i);
                even.add(temp - 1);
            }
        }

        List[] heads = new List[]{even, odd};
        for (List<Integer> curr : heads) {
            for (int i = 0; i < curr.size(); i++) {
                int need = (curr.size() - i) * ele_size * 2; // at least this many places needed
                int thisLoc = curr.get(i);
                if (i > 0){
                    thisLoc = curr.get(i) < contained.get(contained.size() - 1) + ele_size * 2 - 1 /*would I step on the former one?*/
                            ? contained.get(contained.size() - 1) + ele_size * 2 : curr.get(i);
                }
                int avail = (col_size - 1) - thisLoc + 1;
                if (need > avail) {
                    int back = need - avail;
                    back = back % 2 == 0 ? back : back - 1; // because you should always back up even number of locations
                    if (back > 0 && curr.get(i) == 0)
                        System.out.println("Cannot move element back anymore!");
                    else
                        contained.add(Math.max(thisLoc - back, 0));
                } else {
                    contained.add(thisLoc);
                }
            }
        }

        Collections.sort(contained);

        /* check if the result is legal */
        Set<Integer> set = new HashSet<>(contained);
        if (set.size() < contained.size()){
            System.out.println("Duplicate element");
            contained.clear();
            // my debug session
            for (List<Integer> curr : heads) {
                for (int i = 0; i < curr.size(); i++) {
                    int need = (curr.size() - i) * ele_size * 2; // at least this many places needed
                    int thisLoc = curr.get(i);
                    if (i > 0){
                        thisLoc = curr.get(i) < contained.get(contained.size() - 1) + ele_size * 2 - 1 /*would I step on the former one?*/
                                ? contained.get(contained.size() - 1) + ele_size * 2 : curr.get(i);
                    }
                    int avail = (col_size - 1) - thisLoc + 1;
                    if (need > avail) {
                        int back = need - avail;
                        back = back % 2 == 0 ? back : back - 1; // because you should always back up even number of locations
                        if (back > 0 && curr.get(i) == 0)
                            System.out.println("Cannot move element back anymore!");
                        else
                            contained.add(Math.max(thisLoc - back, 0));
                    } else {
                        contained.add(thisLoc);
                    }
                }
            }
        }


        if (input.size() != contained.size())
            System.out.println("Containing failure: input and output size do not match");
        if (contained.get(contained.size() - 1) + ele_size * 2  > col_size + 1) {
            // for example, if you have 288 in one line, choosing 280 and 281 would be ok
            System.out.println("Exceeds column limit");
        }
        return contained;
    }
}
