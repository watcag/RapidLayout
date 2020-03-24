package Experiment;

/*
*     ---  Vanilla Smiluated Annealing Experiment, as baseline  ---
*   This class implements a vanilla simulated annealing placement for
*   80 convolutional blocks in one SLR of UltraScale+ 11P device. The
*   output includes a valid placement solution, evaluation at each step.
*   The annealing schedule must be tuned fully.
*/

import Utils.Utility;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.device.SiteTypeEnum;
import main.MinRect;
import org.opt4j.core.common.random.Rand;

import java.util.*;

import static java.lang.Math.*;

public class vanillaSA {

    private static final SiteTypeEnum DSP_SITE_TYPE = SiteTypeEnum.DSP48E2;
    private static final SiteTypeEnum BRAM_SITE_TYPE = SiteTypeEnum.RAMB180;
    private static final SiteTypeEnum URAM_SITE_TYPE = SiteTypeEnum.URAM288;

    private static String device;
    private static Device dev;
    private static int dsp_in_group, bram_in_group, uram_in_group;
    private static int blocknum;
    private static int ymax;

    private static DeviceMap deviceMap;
    private static Map<Integer, List<Site[]>> currPlacement = new HashMap<>();
    private static Map<SiteTypeEnum, List<List<Site>>> availSites;
    private static Map<SiteTypeEnum, int[]> connections = new HashMap<>();

    private static Random rand;
    private static double temp;


    public static void random_init(int seed) {
        rand = new Random(1);
        SiteTypeEnum[] types = new SiteTypeEnum[]{DSP_SITE_TYPE, BRAM_SITE_TYPE, URAM_SITE_TYPE};
        int[] group_sizes = new int[]{dsp_in_group, bram_in_group, uram_in_group};
        int[] group_numbers = new int[]{2,2,1};

        // select group locations
        Map<SiteTypeEnum, List<List<Site>>> groups = new HashMap<>(); // each List<Site> is a group
        for (int i = 0; i < 3; i++) {
            SiteTypeEnum t = types[i];
            int sizeofCol = deviceMap.getRowNum(t);
            int colNum = deviceMap.getColNum(t);
            int groupSize = group_sizes[i];
            int groupNum = sizeofCol / groupSize;
            List<List<Site>> groups_t = new ArrayList<>();
            // randomly select groupNum * groupSize sites from each column
            int[] sel_idx = helper.select_sites(sizeofCol, groupNum * groupSize, rand);

            for (int col = 0; col < colNum; col++) {
                // group sel_idx according to groupSize
                for (int idx = 0; idx < sel_idx.length; idx += groupSize) {
                    int start = sel_idx[idx];
                    for (int k = 1; k < groupSize; k++) {
                        int curr_idx = idx + k;
                        if (sel_idx[curr_idx] != start + k)
                            sel_idx[curr_idx] = start + k;
                    }
                }
                // add selected sites of this column
                for (int g=0; g < groupNum; g++) {
                    List<Site> aGroup = new ArrayList<>();
                    for (int s = 0; s < groupSize; s++) {
                        int row = sel_idx[g * groupSize + s];
                        Site site = deviceMap.getSite(t, col, row);
                        aGroup.add(site);
                    }
                    groups_t.add(aGroup);
                }
            }

            groups.put(t, groups_t);
        }

        // shuffle connections
        for (int t=0; t < 3; t++) { // go through each type
            SiteTypeEnum type = types[t];
            List<List<Site>> groups_t = groups.get(type);
            int[] idxs = helper.rand_idx(groups_t.size(), rand);
            int i = 0;

            for (int n=0; n < blocknum; n++) { // go through each conv unit
                // extract groups - current type, if bram or dsp then 2 groups, else 1 group
                List<Site> currTypeGroup = new ArrayList<>();
                for (int g=0; g < group_numbers[t]; g++) {
                    int idx = idxs[i];
                    Site[] aGroup = groups_t.get(idx).toArray(new Site[0]); // a group of current type
                    currTypeGroup.addAll(Arrays.asList(aGroup));
                    i++; // go to next random index

                    // update usage map
                    for (Site site : aGroup)
                        deviceMap.setSitePlaced(type, site.getInstanceX(), site.getInstanceY());
                }
                currPlacement.get(n).add(currTypeGroup.toArray(new Site[0]));
            }

        }



    }


    public static Map<Integer, List<Site[]>> get_neighbor() {
        Map<Integer, List<Site[]>> placement = new HashMap<>();
        for (int i=0; i<blocknum; i++)
            placement.put(i, new ArrayList<>());

        SiteTypeEnum[] types = new SiteTypeEnum[]{DSP_SITE_TYPE, BRAM_SITE_TYPE, URAM_SITE_TYPE};
        int[] group_sizes = new int[]{dsp_in_group, bram_in_group, uram_in_group};
        int[] group_numbers = new int[]{2,2,1};

        // select group locations
        Map<SiteTypeEnum, List<List<Site>>> groups = new HashMap<>(); // each List<Site> is a group
        for (int i = 0; i < 3; i++) {
            SiteTypeEnum t = types[i];
            List<List<Site>> groups_t = new ArrayList<>();

            for (int n=0; n < blocknum; n++) {
                List<Site[]> threeTypes = currPlacement.get(n);
                    Site[] tmp = threeTypes.get(i);
                    int group_num = group_numbers[i];
                    int group_size = group_sizes[i];
                    for (int g=0; g < group_num; g++) {
                        Site[] oneGroup = new Site[group_size];
                        for (int w=0; w<group_size; w++)
                            oneGroup[w] = tmp[g * group_size + w];

                        // move the group a little bit
                        List<Site> moved = deviceMap.MoveAbit(t, rand, oneGroup);
                        groups_t.add(moved);
                    }

            }

            groups.put(t, groups_t);
        }

        // shuffle connections
        for (int t=0; t < 3; t++) { // go through each type
            SiteTypeEnum type = types[t];
            List<List<Site>> groups_t = groups.get(type);
            int[] idxs;
            if (connections.size() == 3) {
                idxs = connections.get(type);
                idxs = helper.mixed_idx(idxs, rand);
                connections.remove(type);
            } else {
                idxs = helper.rand_idx(groups_t.size(), rand);
            }
            connections.put(type, idxs);

            int i = 0;

            for (int n=0; n < blocknum; n++) { // go through each conv unit
                List<Site> currTypeGroup = new ArrayList<>();
                for (int g=0; g < group_numbers[t]; g++) {
                    int idx = idxs[i];
                    Site[] aGroup = groups_t.get(idx).toArray(new Site[0]); // a group of current type
                    currTypeGroup.addAll(Arrays.asList(aGroup));
                    i++; // go to next random index
                }
                placement.get(n).add(currTypeGroup.toArray(new Site[0]));
            }

        }

        return placement;
    }

    public static double currTemp(int step, double init_temp, int choice, double parameter) {
        // choice:
        // 0 = exponential, 1 = linear, 2 = logarithmic
        switch (choice) {
            case 0: // exponential
                if (step == 0)
                    temp = init_temp * pow(parameter, step);
                else {
                    temp = temp * parameter;
                }
                break;
            case 2: // logarithmic
                temp = init_temp / parameter * log(step + 1);
                break;
            default: // linear
                temp = init_temp - parameter * step;
                break;
        }

        return temp;
    }

    public static void run() {
        double init_temp = 5e10;
        int choice = 1; // 0 = exponential, 1 = linear, 2 = logarithmic
        double parameter = 5;

        random_init(1);

        double index = 0;
        double standard = 10;
        int check_period = 10000;
        System.out.println("  step      wirelength      bboxSize     probability");
        System.out.println("------------------------------------------------------");
        int step = 0;
        while (true) {
            Utility curr = new Utility(currPlacement, device);
            double currWirelength = curr.getUnifiedWireLength();
            double currBBox = curr.getMaxBBoxSize();
            double currValue = pow(currWirelength, 2) * currBBox;
            //double currValue = curr.getMaxWireLength();

            // get new placement
            Map<Integer, List<Site[]>> nextPlacement = get_neighbor();
            Utility next = new Utility(nextPlacement, device);
            double nextWirelength = next.getUnifiedWireLength();
            double nextBBox = next.getMaxBBoxSize();
            double nextValue = pow(nextWirelength, 2) * nextBBox;
            //double nextValue = curr.getMaxWireLength();

            double temp = currTemp(step, init_temp, choice, parameter);
            double prob = 0;

            if (nextValue < currValue) {
                currPlacement = nextPlacement;
            } else {
                prob = exp(-(nextValue - currValue)/temp);
                Random rand = new Random();
                if (rand.nextDouble() < prob)
                    currPlacement = nextPlacement;
            }

            // print information
            if (step % 10000 == 0)
                System.out.println(step + "\t\t" + currWirelength + "\t\t" +  currBBox + "\t\t" + prob);

            step++;

            // stop criterion
//            if (step % check_period == 0)
//                index = currValue;
//            else if (Math.abs(nextValue - index) < standard)
//                break;
        }

    }

    public static void setParam() {
        device = "vu11p";
        dev = new Design("name", device).getDevice();
        dsp_in_group = 9;
        bram_in_group = 4;
        uram_in_group = 2;
        MinRect mr = new MinRect(device, dsp_in_group * 2, bram_in_group * 2, uram_in_group);
        blocknum = mr.getBlocknum();
        int x_min = 0;
        int x_max = 6000; // use all columns
        int y_min = 0;
        ymax = mr.getYmax();
        System.out.println("ymax = " + ymax);
        availSites = Opt.PlaceCreator.getAvailableSites(dev, x_min, x_max, y_min, ymax);
        deviceMap = new DeviceMap(availSites);
        // initialize placement map
        for (int i=0; i < blocknum; i++) {
            List<Site[]> l = new ArrayList<>();
            currPlacement.put(i, l);
        }

    }



    public static void main(String[] args) {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));

        setParam();

        run();

        deviceMap.printCurrentMapStatus();

    }
}







class helper {

    // randomly select n numbers from sequence 0:m-1
    public static int[] select_sites(int m, int n, Random random) {
        List<Integer> a = new ArrayList<>();
        for (int i=0; i < m; i++)
            a.add(i);
        Collections.shuffle(a, random);

        List<Integer> b = new ArrayList<>();
        for (int i=0; i < n; i++)
            b.add(a.get(i));

        Collections.sort(b);

        int[] c = new int[n];
        for (int i=0; i < n; i++) {
            c[i] = b.get(i);
        }

        return c;
    }

    // return a random index list from 0 to m
    public static int[] rand_idx(int m, Random rand) {
        List<Integer> a = new ArrayList<>();
        for (int i=0; i < m; i++)
            a.add(i);
        Collections.shuffle(a, rand);

        int[] b = new int[m];
        for (int i=0; i < m; i++) {
            b[i] = a.get(i);
        }

        return b;
    }

    public static int[] swap_idx(int[] connection, Random rand) {
        int size = connection.length;
        List<Integer> newList = new ArrayList<>();
        for (int value : connection) newList.add(value);
        if (size > 1) {
            int i = rand.nextInt(size);
            int j;
            do {
                j = rand.nextInt(size);
            } while (j==i);
            Collections.swap(newList, i, j);
        }
        int[] newArray = new int[size];
        for (int i=0; i < size; i++) {
            newArray[i] = newList.get(i);
        }
        return newArray;
    }

    public static int[] insert_idx(int[] connection, Random rand) {
        int size = connection.length;
        List<Integer> newList = new ArrayList<>();
        for (int value : connection) newList.add(value);
        if (size > 1) {
            final int i = rand.nextInt(size);
            Integer object = newList.remove(i);
            final int j = rand.nextInt(newList.size());
            newList.add(j, object);
        }
        int[] newArray = new int[size];
        for (int i=0; i < size; i++) {
            newArray[i] = newList.get(i);
        }
        return newArray;
    }

    public static int[] revert_idx(int[] connection, Random rand) {
        int size = connection.length;
        List<Integer> newList = new ArrayList<>();
        for (int value : connection) newList.add(value);

        if (size > 1) {
            int a = rand.nextInt(size - 1);
            int b;
            do {
                b = a + rand.nextInt(size - a);
            } while (b == a);

            while (a < b) {
                Collections.swap(newList, a, b);
                a++;
                b--;
            }
        }

        int[] newArray = new int[size];
        for (int i=0; i < size; i++) {
            newArray[i] = newList.get(i);
        }
        return newArray;
    }

    public static int[] mixed_idx(int[] connection, Random rand) {
        if (rand.nextDouble() < 0.33) {
            connection = swap_idx(connection, rand);
        } else if (rand.nextBoolean()) {
            connection = insert_idx(connection, rand);
        } else {
            connection = revert_idx(connection, rand);
        }

        return connection;
    }


}
