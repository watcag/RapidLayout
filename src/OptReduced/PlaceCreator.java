package OptReduced;

import com.google.inject.Inject;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.device.SiteTypeEnum;
import org.opt4j.core.Genotype;
import org.opt4j.core.genotype.CompositeGenotype;
import org.opt4j.core.problem.Creator;
import org.opt4j.core.start.Constant;

import java.util.*;


public class PlaceCreator implements Creator<CompositeGenotype<SiteTypeEnum, Genotype>> {
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

    int block_num = 9;

    String device = "xcvu37p";

    int x_min = 0;
    int x_max = 5000;
    int y_min = 0;
    int y_max = 1500;


    @Inject
    public void setBlock_num(@Constant(value = "block_num") int block_num) {
        this.block_num = block_num;
    }

    @Inject
    public void setDevice(@Constant(value = "device") String device) {
        this.device = device;
    }

    @Inject
    public void setX_min(@Constant(value = "x_min") int x_min){
        this.x_min = x_min;
    }

    @Inject
    public void setX_max(@Constant(value = "x_max") int x_max){
        this.x_max = x_max;
    }

    @Inject
    public void setY_min(@Constant(value = "y_min") int y_min){
        this.y_min = y_min;
    }

    @Inject
    public void setY_max(@Constant(value = "y_max") int y_max){
        this.y_max = y_max;
    }


    public CompositeGenotype<SiteTypeEnum, Genotype> create() {
        // for DSP, I can first get all available DSP sites by column
        // and for each block, take 9 consecutive DSP from 1 column, and repeat
        Design d = new Design("design", device);
        Device dev = d.getDevice();

        Map<SiteTypeEnum, List<List<Site>>> map = getAvailableSites(dev, x_min, x_max, y_min, y_max);

        /*System.out.println("Available Hard Blocks: ");
        System.out.println("DSP: " + map.get(DSP_SITE_TYPE).size() + " x " + map.get(DSP_SITE_TYPE).get(0).size());
        System.out.println("BRAM: " + map.get(BRAM_SITE_TYPE).size() + " x " + map.get(BRAM_SITE_TYPE).get(0).size());
        System.out.println("URAM: " + map.get(URAM_SITE_TYPE).size() + " x " + map.get(URAM_SITE_TYPE).get(0).size());
        int max_dsp_num = map.get(DSP_SITE_TYPE).get(0).size() / 9 * map.get(DSP_SITE_TYPE).size() * 2;
        int max_bram_num = map.get(BRAM_SITE_TYPE).get(0).size() / 4 * map.get(BRAM_SITE_TYPE).size() * 2;
        int max_uram_num = map.get(URAM_SITE_TYPE).get(0).size() / 2 * map.get(URAM_SITE_TYPE).size();
        System.out.println("max number of block = " + Math.min(Math.min(max_dsp_num, max_bram_num), max_uram_num));*/

        CompositeGenotype<SiteTypeEnum, Genotype> genotype = new CompositeGenotype<>();
        // mapping
        PlaceGenotype<Integer> dspMapping = new PlaceGenotype<>(map.get(DSP_SITE_TYPE));
        PlaceGenotype<Integer> bramMapping = new PlaceGenotype<>(map.get(BRAM_SITE_TYPE));
        PlaceGenotype<Integer> uramMapping = new PlaceGenotype<>(map.get(URAM_SITE_TYPE));

        for (int i = 0; i < block_num * 2; i++)
            dspMapping.add(i);
        for (int i = 0; i < block_num * 2; i++)
            bramMapping.add(i);
        for (int i = 0; i < block_num; i++)
            uramMapping.add(i);

        dspMapping.init(new Random());
        bramMapping.init(new Random());
        uramMapping.init(new Random());

        genotype.put(DSP_MAP, dspMapping);
        genotype.put(BRAM_MAP, bramMapping);
        genotype.put(URAM_MAP, uramMapping);

        return genotype;

    }

    public Map<SiteTypeEnum, List<List<Site>>> getAvailableSites(Device dev, int x_min, int x_max, int y_min, int y_max) {
        // get all available sites
        Site[] allDSPSites = dev.getAllCompatibleSites(DSP_SITE_TYPE);
        Site[] RAMB180 = dev.getAllCompatibleSites(BRAM_SITE_TYPE);
        Site[] RAMB181 = dev.getAllCompatibleSites(BRAM_SITE_TYPE2);
        List<Site> RAMB = new ArrayList<>(Arrays.asList(RAMB180));
        RAMB.addAll(Arrays.asList(RAMB181));
        Site[] allBRAMSites = RAMB.toArray(new Site[0]);
        Site[] allURAMSites = dev.getAllCompatibleSites(URAM_SITE_TYPE);

        Collections.sort(Arrays.asList(allDSPSites), (site, t1) -> {
            // ascending order
            String name0 = site.getName();
            String name1 = t1.getName();
            int col0 = Integer.parseInt(name0.substring(name0.indexOf('X') + 1, name0.indexOf('Y')));
            int col1 = Integer.parseInt(name1.substring(name1.indexOf('X') + 1, name1.indexOf('Y')));
            //return col1-col0;
            return col0 - col1;
        });
        Collections.sort(Arrays.asList(allBRAMSites), (site, t1) -> {
            // descending order
            String name0 = site.getName();
            String name1 = t1.getName();
            int col0 = Integer.parseInt(name0.substring(name0.indexOf('X') + 1, name0.indexOf('Y')));
            int col1 = Integer.parseInt(name1.substring(name1.indexOf('X') + 1, name1.indexOf('Y')));
            //return col1-col0;
            return col0 - col1;// ascending order
        });
        Collections.sort(Arrays.asList(allURAMSites), (site, t1) -> {
            // descending order
            String name0 = site.getName();
            String name1 = t1.getName();
            int col0 = Integer.parseInt(name0.substring(name0.indexOf('X') + 1, name0.indexOf('Y')));
            int col1 = Integer.parseInt(name1.substring(name1.indexOf('X') + 1, name1.indexOf('Y')));
            //return col1-col0;
            return col0 - col1;// ascending order
        });

        String n = allDSPSites[allDSPSites.length - 1].getName();
        int max_dsp_col = Integer.parseInt(n.substring(n.indexOf("X") + 1, n.indexOf("Y")));
        n = allBRAMSites[allBRAMSites.length - 1].getName();
        int max_bram_col = Integer.parseInt(n.substring(n.indexOf("X") + 1, n.indexOf("Y")));
        n = allURAMSites[allURAMSites.length - 1].getName();
        int max_uram_col = Integer.parseInt(n.substring(n.indexOf("X") + 1, n.indexOf("Y")));

        List<List<Site>> DSPSites = new ArrayList<>();
        List<List<Site>> BRAMSites = new ArrayList<>();
        List<List<Site>> URAMSites = new ArrayList<>();
        for (int i = 0; i <= max_dsp_col; i++)
            DSPSites.add(new ArrayList<>());
        for (int i = 0; i <= max_bram_col; i++)
            BRAMSites.add(new ArrayList<>());
        for (int i = 0; i <= max_uram_col; i++)
            URAMSites.add(new ArrayList<>());

        for (Site s : allDSPSites)
            if (s.getRpmX() >= x_min & s.getRpmX() <= x_max && s.getRpmY() >= y_min && s.getRpmY() <= y_max) {
                String name = s.getName();
                int col = Integer.parseInt(name.substring(name.indexOf('X') + 1, name.indexOf('Y')));
                DSPSites.get(col).add(s);
            }

        for (Site s : allBRAMSites)
            if (s.getRpmX() >= x_min & s.getRpmX() <= x_max && s.getRpmY() >= y_min && s.getRpmY() <= y_max) {
                String name = s.getName();
                int col = Integer.parseInt(name.substring(name.indexOf('X') + 1, name.indexOf('Y')));
                BRAMSites.get(col).add(s);
            }
        for (Site s : allURAMSites)
            if (s.getRpmX() >= x_min & s.getRpmX() <= x_max && s.getRpmY() >= y_min && s.getRpmY() <= y_max) {
                String name = s.getName();
                int col = Integer.parseInt(name.substring(name.indexOf('X') + 1, name.indexOf('Y')));
                URAMSites.get(col).add(s);
            }

        // clear empty lists
        for (int i = DSPSites.size() - 1; i >= 0; i--) {
            if (DSPSites.get(i).isEmpty())
                DSPSites.remove(i);
        }
        for (int i = BRAMSites.size() - 1; i >= 0; i--) {
            if (BRAMSites.get(i).isEmpty())
                BRAMSites.remove(i);
        }
        for (int i = URAMSites.size() - 1; i >= 0; i--) {
            if (URAMSites.get(i).isEmpty())
                URAMSites.remove(i);
        }

        Map<SiteTypeEnum, List<List<Site>>> map = new HashMap<>();
        map.put(DSP_SITE_TYPE, DSPSites);
        map.put(BRAM_SITE_TYPE, BRAMSites);
        map.put(URAM_SITE_TYPE, URAMSites);

        for (SiteTypeEnum key : map.keySet())
            for (List<Site> sites : map.get(key))
                Collections.sort(sites, (site, t1) -> {
                    String name0 = site.getName();
                    String name1 = t1.getName();
                    int row0 = Integer.parseInt(name0.substring(name0.indexOf('Y') + 1));
                    int row1 = Integer.parseInt(name1.substring(name1.indexOf('Y') + 1));
                    return row0 - row1;// ascending order
                });


        return map;
    }


}

