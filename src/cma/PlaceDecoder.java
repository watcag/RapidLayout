package cma;


import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.device.SiteTypeEnum;

import java.util.*;

public class PlaceDecoder {

    private static final SiteTypeEnum DSP_MAP = SiteTypeEnum.SLICEL;
    private static final SiteTypeEnum BRAM_MAP = SiteTypeEnum.BUFG;
    private static final SiteTypeEnum URAM_MAP = SiteTypeEnum.LAGUNA;

    public static Map<Integer, List<Site[]>> Mapping(int block_num, Map<SiteTypeEnum, List<Site[]>> chosenSites, Integer[] dsp, Integer[] bram, Integer[] uram) {
        Map<Integer, List<Site[]>> map = new HashMap<>(); // stores the final configuration for each block
        SiteTypeEnum[] types = new SiteTypeEnum[]{DSP_MAP, BRAM_MAP, URAM_MAP};
        Integer[] lengths = new Integer[]{2, 2, 1};
        List[] mapping = new List[]{
                new ArrayList<>(Arrays.asList(dsp)),
                new ArrayList<>(Arrays.asList(bram)),
                new ArrayList<>(Arrays.asList(uram))};
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

    public static Map<Integer, List<Site[]>> decode(double[] doubles, Map<SiteTypeEnum, List<Site[]>> selected_sites){
        int block_num = doubles.length / 5;

        Integer[] dsp = getSortIndex(Arrays.copyOfRange(doubles, 0, 2 *  block_num));
        Integer[] bram = getSortIndex(Arrays.copyOfRange(doubles, 2 * block_num, 4 *  block_num));
        Integer[] uram = getSortIndex(Arrays.copyOfRange(doubles, 4 * block_num, 5 *  block_num));

        return Mapping(block_num, selected_sites, dsp, bram, uram);
    }

    /*
        for example, if input is [0.3, 0.1, 0.2]
        output is [2, 0, 1]
     */
    public static Integer[] getSortIndex(double[] doubles){
        ArrayIndexComparator comparator = new ArrayIndexComparator(doubles);
        Integer[] indexes = comparator.createIndexArray();
        Arrays.sort(indexes, comparator);
        Integer[] indexesOfSorted = Collections.nCopies(doubles.length, 0).toArray(new Integer[0]);
        for (int i = 0; i < indexes.length; i++){
            indexesOfSorted[indexes[i]] = i;
        }
        return indexesOfSorted;
    }

    public static class ArrayIndexComparator implements Comparator<Integer>
    {
        private final double[] array;

        public ArrayIndexComparator(double[] array)
        {
            this.array = array;
        }

        public Integer[] createIndexArray()
        {
            Integer[] indexes = new Integer[array.length];
            for (int i = 0; i < array.length; i++)
            {
                indexes[i] = i; // Autoboxing
            }
            return indexes;
        }

        @Override
        public int compare(Integer index1, Integer index2)
        {
            // Autounbox from Integer to int to use as array indexes
            return Double.compare(array[index1], array[index2]);
        }
    }



}
