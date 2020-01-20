package Utils;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.device.*;

import java.util.*;
import java.util.stream.Collectors;

public class Utils {
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

    protected Map<Integer, List<Site[]>> phenotype;
    protected Device dev;

    public Utils(Map<Integer, List<Site[]>> phenotype, String device){
        this.phenotype = phenotype;
        // Initiate Device
        Design d = new Design("main", device);
        this.dev = d.getDevice();
    }

    public double AreaPerBlock(){

        // population census
        Set<Integer> keys = phenotype.keySet();
        List<Site> dspSite = new ArrayList<>();
        List<Site> bramSite = new ArrayList<>();
        List<Site> uramSite = new ArrayList<>();
        int blocknum = keys.size();

        for (Integer i : keys){
            dspSite.addAll(Arrays.asList(phenotype.get(i).get(0)));
            bramSite.addAll(Arrays.asList(phenotype.get(i).get(1)));
            uramSite.addAll(Arrays.asList(phenotype.get(i).get(2)));
        }

        // get all used sites
        List<Site> usedSites = new ArrayList<Site>();
        usedSites.addAll(dspSite);
        usedSites.addAll(bramSite);
        usedSites.addAll(uramSite);

        // get bounding rectangular area
        int x_min = Integer.MAX_VALUE; int x_max = 0; int y_min = Integer.MAX_VALUE; int y_max = 0;
        for (Site s : usedSites){
            int x = s.getRpmX();
            int y = s.getRpmY();
            if (x < x_min) x_min = x;
            if (x > x_max) x_max = x;
            if (y < y_min) y_min = y;
            if (y > y_max) y_max = y;
        }

        return (x_max-x_min) * (y_max-y_min) / (double)blocknum;
    }


    public double getUnifiedWireLength(){
        double wireLength = 0;
        Set<Integer> keys = phenotype.keySet();
        for (Integer key : keys){
            List<Site[]> thisBlock = phenotype.get(key);
            Site[] dsp = thisBlock.get(0);
            Site[] bram = thisBlock.get(1);
            Site[] uram = thisBlock.get(2);
            for(Site d : dsp)
                for (Site b : bram){
                    Tile dTile = d.getTile();
                    Tile bTile = b.getTile();
                    wireLength += dTile.getManhattanDistance(bTile);
                }
            for (Site b : bram)
                for (Site u : uram){
                    Tile bTile =  b.getTile();
                    Tile uTile = u.getTile();
                    wireLength += bTile.getManhattanDistance(uTile);
                }
        }

        wireLength /= keys.size();

        return wireLength;
    }

    public double getMaxWireLength(){
        List<Double> wireLengths = new ArrayList<>();
        Set<Integer> keys = phenotype.keySet();
        for (Integer key : keys){
            double wireLength = 0;
            List<Site[]> thisBlock = phenotype.get(key);
            Site[] dsp = thisBlock.get(0);
            Site[] bram = thisBlock.get(1);
            Site[] uram = thisBlock.get(2);
            for(Site d : dsp)
                for (Site b : bram){
                    Tile dTile = d.getTile();
                    Tile bTile = b.getTile();
                    wireLength += dTile.getManhattanDistance(bTile);
                }
            for (Site b : bram)
                for (Site u : uram){
                    Tile bTile =  b.getTile();
                    Tile uTile = u.getTile();
                    wireLength += bTile.getManhattanDistance(uTile);
                }
            wireLengths.add(wireLength);
        }


        return Collections.max(wireLengths);
    }

    public double getMaxArea() {
        double max_area = 0;

        Set<Integer> keys = phenotype.keySet();

        for (Integer i : keys){
            List<Site> dspSite = new ArrayList<>();
            List<Site> bramSite = new ArrayList<>();
            List<Site> uramSite = new ArrayList<>();
            dspSite.addAll(Arrays.asList(phenotype.get(i).get(0)));
            bramSite.addAll(Arrays.asList(phenotype.get(i).get(1)));
            uramSite.addAll(Arrays.asList(phenotype.get(i).get(2)));
            // get all used sites
            List<Site> usedSites = new ArrayList<Site>();
            usedSites.addAll(dspSite);
            usedSites.addAll(bramSite);
            usedSites.addAll(uramSite);
            // get bounding rectangular area
            int x_min = Integer.MAX_VALUE; int x_max = 0; int y_min = Integer.MAX_VALUE; int y_max = 0;
            for (Site s : usedSites){
                int x = s.getRpmX();
                int y = s.getRpmY();
                if (x < x_min) x_min = x;
                if (x > x_max) x_max = x;
                if (y < y_min) y_min = y;
                if (y > y_max) y_max = y;
            }

            double area = (x_max-x_min) * (y_max-y_min);
            if (area > max_area)
                max_area = area;
        }

        return max_area;
    }

    public double getMaxSpread() {
        double max_range = 0;

        Set<Integer> keys = phenotype.keySet();

        for (Integer i : keys){
            List<Site> dspSite = new ArrayList<>();
            List<Site> bramSite = new ArrayList<>();
            List<Site> uramSite = new ArrayList<>();
            dspSite.addAll(Arrays.asList(phenotype.get(i).get(0)));
            bramSite.addAll(Arrays.asList(phenotype.get(i).get(1)));
            uramSite.addAll(Arrays.asList(phenotype.get(i).get(2)));
            // get all used sites
            List<Site> usedSites = new ArrayList<Site>();
            usedSites.addAll(dspSite);
            usedSites.addAll(bramSite);
            usedSites.addAll(uramSite);
            // get bounding rectangular area
            int x_min = Integer.MAX_VALUE; int x_max = 0; int y_min = Integer.MAX_VALUE; int y_max = 0;
            for (Site s : usedSites){
                int x = s.getRpmX();
                int y = s.getRpmY();
                if (x < x_min) x_min = x;
                if (x > x_max) x_max = x;
                if (y < y_min) y_min = y;
                if (y > y_max) y_max = y;
            }

            double width = x_max - x_min;
            double height = y_max - y_min;
            if (width > max_range)
                max_range = width;
        }

        return max_range;
    }

    public double getMaxBBoxSize() {
        double max_size = 0;

        Set<Integer> keys = phenotype.keySet();

        for (Integer i : keys){
            List<Site> dspSite = new ArrayList<>();
            List<Site> bramSite = new ArrayList<>();
            List<Site> uramSite = new ArrayList<>();
            dspSite.addAll(Arrays.asList(phenotype.get(i).get(0)));
            bramSite.addAll(Arrays.asList(phenotype.get(i).get(1)));
            uramSite.addAll(Arrays.asList(phenotype.get(i).get(2)));
            // get all used sites
            List<Site> usedSites = new ArrayList<Site>();
            usedSites.addAll(dspSite);
            usedSites.addAll(bramSite);
            usedSites.addAll(uramSite);
            // get bounding rectangular area
            int x_min = Integer.MAX_VALUE; int x_max = 0; int y_min = Integer.MAX_VALUE; int y_max = 0;
            for (Site s : usedSites){
                int x = s.getRpmX();
                int y = s.getRpmY();
                if (x < x_min) x_min = x;
                if (x > x_max) x_max = x;
                if (y < y_min) y_min = y;
                if (y > y_max) y_max = y;
            }

            double width = x_max - x_min;
            double height = y_max - y_min;
            double size = width + height;
            if (size > max_size)
                max_size = size;

        }

        return max_size;
    }



    public double[] getUtilization(int x_min, int x_max, int y_min, int y_max){
        // get all available sites
        Site[] allDSPSites = dev.getAllCompatibleSites(DSP_SITE_TYPE);
        Site[] RAMB180 = dev.getAllCompatibleSites(BRAM_SITE_TYPE);
        Site[] RAMB181 = dev.getAllCompatibleSites(BRAM_SITE_TYPE2);
        List<Site> RAMB = new ArrayList<>(Arrays.asList(RAMB180));
        RAMB.addAll(Arrays.asList(RAMB181));
        Site[] allBRAMSites = RAMB.toArray(new Site[0]);
        Site[] allURAMSites = dev.getAllCompatibleSites(URAM_SITE_TYPE);

        // get all used sites
        List<Site> usedDSP = new ArrayList<>();
        List<Site> usedBRAM = new ArrayList<>();
        List<Site> usedURAM = new ArrayList<>();
        for (int i : phenotype.keySet())
        {
            List<Site[]> thisBlock = phenotype.get(i);
            usedDSP.addAll(Arrays.asList(thisBlock.get(0)));
            usedBRAM.addAll(Arrays.asList(thisBlock.get(1)));
            usedURAM.addAll(Arrays.asList(thisBlock.get(2)));
        }

        // get all available resources in the rectangular area
        List<Site[]> allSites = new ArrayList<>();
        List<List<Site>> inRect = new ArrayList<>();
        allSites.add(allDSPSites); allSites.add(allBRAMSites); allSites.add(allURAMSites);
        for (int i = 0; i < allSites.size(); i++){
            inRect.add(new ArrayList<>());
            for (Site s : allSites.get(i))
                if (s.getRpmX() >= x_min && s.getRpmX() <= x_max && s.getRpmY() >= y_min && s.getRpmY() <= y_max)
                    inRect.get(i).add(s);
        }

        double dspRate = (double)usedDSP.size() / (double)inRect.get(0).size() * 100;
        double bramRate = (double)usedBRAM.size() / (double)inRect.get(1).size() * 100;
        double uramRate = (double)usedURAM.size() / (double)inRect.get(2).size() * 100;

        return new double[]{dspRate, bramRate, uramRate};
    }
    public double[] getUtilization(){
        // get all available sites
        Site[] allDSPSites = dev.getAllCompatibleSites(DSP_SITE_TYPE);
        Site[] RAMB180 = dev.getAllCompatibleSites(BRAM_SITE_TYPE);
        Site[] RAMB181 = dev.getAllCompatibleSites(BRAM_SITE_TYPE2);
        List<Site> RAMB = new ArrayList<>(Arrays.asList(RAMB180));
        RAMB.addAll(Arrays.asList(RAMB181));
        Site[] allBRAMSites = RAMB.toArray(new Site[0]);
        Site[] allURAMSites = dev.getAllCompatibleSites(URAM_SITE_TYPE);

        // get all used sites
        List<Site> usedDSP = new ArrayList<>();
        List<Site> usedBRAM = new ArrayList<>();
        List<Site> usedURAM = new ArrayList<>();
        for (int i : phenotype.keySet())
        {
            List<Site[]> thisBlock = phenotype.get(i);
            usedDSP.addAll(Arrays.asList(thisBlock.get(0)));
            usedBRAM.addAll(Arrays.asList(thisBlock.get(1)));
            usedURAM.addAll(Arrays.asList(thisBlock.get(2)));
        }

        // get bounding rectangular area
        // get all used sites
        List<Site> usedSites = new ArrayList<>();
        for (int i : phenotype.keySet())
            for (Site[] s : phenotype.get(i))
                usedSites.addAll(Arrays.asList(s));

        int x_min = Integer.MAX_VALUE; int x_max = 0; int y_min = Integer.MAX_VALUE; int y_max = 0;
        for (Site s : usedSites){
            int x = s.getRpmX();
            int y = s.getRpmY();
            if (x < x_min) x_min = x;
            if (x > x_max) x_max = x;
            if (y < y_min) y_min = y;
            if (y > y_max) y_max = y;
        }

        // get all available resources in the rectangular area
        List<Site[]> allSites = new ArrayList<>();
        List<List<Site>> inRect = new ArrayList<>();
        allSites.add(allDSPSites); allSites.add(allBRAMSites); allSites.add(allURAMSites);
        for (int i = 0; i < allSites.size(); i++){
            inRect.add(new ArrayList<>());
            for (Site s : allSites.get(i))
                if (s.getRpmX() >= x_min && s.getRpmX() <= x_max && s.getRpmY() >= y_min && s.getRpmY() <= y_max)
                    inRect.get(i).add(s);
        }

        double dspRate = (double)usedDSP.size() / (double)inRect.get(0).size() * 100;
        double bramRate = (double)usedBRAM.size() / (double)inRect.get(1).size() * 100;
        double uramRate = (double)usedURAM.size() / (double)inRect.get(2).size() * 100;

        return new double[]{dspRate, bramRate, uramRate};
    }

    public double getCoord(){
        double sum = 0;
        Set<Integer> keys = phenotype.keySet();
        for (Integer key : keys){
            List<Site[]> thisBlock = phenotype.get(key);
            for(int i = 0 ; i < thisBlock.size(); i++){
                Site[] sites = thisBlock.get(i);
                for (Site s : sites){
                    String name = s.getName();
                    int x = Integer.parseInt(name.substring(name.indexOf('X')+1, name.indexOf('Y')));
                    int y = Integer.parseInt(name.substring(name.indexOf('Y')+1));
                    sum += y + x;
                }
            }

        }


        return sum;
    }

    public boolean checkDuplicate(){
        Set<Integer> keys = phenotype.keySet();
        List<Site> all = new ArrayList<>();
        for (Integer key : keys){
            List<Site[]> thisBlock = phenotype.get(key);
            for (Site[] s : thisBlock)
                all.addAll(Arrays.asList(s));
        }
        Set<Site> set = new HashSet<>(all);
        return all.size() > set.size();
    }

    public Site[] getDSPSiteRangeOf(){
        List<Site> usedSites = new ArrayList<>();
        for (Integer index : phenotype.keySet()){
            List<Site[]> thisBlock = phenotype.get(index);
            Site[] sites = thisBlock.get(0);
            usedSites.addAll(Arrays.asList(sites));
        }

        Collections.sort(usedSites, (s0, s1) -> {
            int xy0 = s0.getRpmX() + s0.getRpmY();
            int xy1 = s1.getRpmX() + s1.getRpmY();
            return xy0 - xy1; // ascending order
        });

        return new Site[]{usedSites.get(0), usedSites.get(usedSites.size()-1)};
    }

    public Site[] getBRAMSiteRangeOf(){
        List<Site> usedSites = new ArrayList<>();
        for (Integer index : phenotype.keySet()){
            List<Site[]> thisBlock = phenotype.get(index);
            Site[] sites = thisBlock.get(1);
            usedSites.addAll(Arrays.asList(sites));
        }

        Collections.sort(usedSites, (s0, s1) -> {
            int xy0 = s0.getRpmX() + s0.getRpmY();
            int xy1 = s1.getRpmX() + s1.getRpmY();
            return xy0 - xy1; // ascending order
        });

        return new Site[]{usedSites.get(0), usedSites.get(usedSites.size()-1)};
    }

    public Site[] getURAMSiteRangeOf(){
        List<Site> usedSites = new ArrayList<>();
        for (Integer index : phenotype.keySet()){
            List<Site[]> thisBlock = phenotype.get(index);
            Site[] sites = thisBlock.get(2);
            usedSites.addAll(Arrays.asList(sites));
        }

        Collections.sort(usedSites, (s0, s1) -> {
            int xy0 = s0.getRpmX() + s0.getRpmY();
            int xy1 = s1.getRpmX() + s1.getRpmY();
            return xy0 - xy1; // ascending order
        });


        return new Site[]{usedSites.get(0), usedSites.get(usedSites.size()-1)};
    }

    public ClockRegion[] getClockRegionRange(){
        List<Site> usedSites = new ArrayList<>();
        for (Integer i : phenotype.keySet())
            for (Site[] s : phenotype.get(i))
                usedSites.addAll(Arrays.asList(s));

        List<ClockRegion> clkRegions = new ArrayList<>();
        for(Site s : usedSites){
            ClockRegion tmp = s.getTile().getClockRegion();
            if (!clkRegions.contains(tmp))
                clkRegions.add(tmp);
        }
        Collections.sort(clkRegions, (a, b) -> {
            int xy0 = a.getInstanceX() + a.getInstanceY();
            int xy1 = b.getInstanceX() + b.getInstanceY();
            return xy0 - xy1; // ascending
        });

        return new ClockRegion[]{clkRegions.get(0), clkRegions.get(clkRegions.size()-1)};
    }

    public Site[] getSLICERangeOf(){
        List<Site> usedSites = new ArrayList<>();
        for (Integer i : phenotype.keySet())
            for (Site[] s : phenotype.get(i))
                usedSites.addAll(Arrays.asList(s));
            int x_min = Integer.MAX_VALUE; int x_max = 0; int y_min = Integer.MAX_VALUE; int y_max = 0;
        for (Site s : usedSites){
            int x = s.getRpmX();
            int y = s.getRpmY();
            if (x < x_min) x_min = x;
            if (x > x_max) x_max = x;
            if (y < y_min) y_min = y;
            if (y > y_max) y_max = y;
        }

        List<Site> slices = new ArrayList<>();
        slices.addAll(Arrays.asList(dev.getAllCompatibleSites(SiteTypeEnum.SLICEL)));
        slices.addAll(Arrays.asList(dev.getAllCompatibleSites(SiteTypeEnum.SLICEM)));

        int finalX_min = x_min;
        int finalX_max = x_max;
        int finalY_min = y_min;
        int finalY_max = y_max;
        List<Site> slices_in_bbox = slices.stream()
                .filter(p -> p.getRpmX() >= finalX_min && p.getRpmY() <= finalX_max
                && p.getRpmY() >= finalY_min && p.getRpmY() <= finalY_max)
                .collect(Collectors.toList());
        Collections.sort(slices_in_bbox, (a, b)  -> {
            int xy0 = a.getRpmX() + a.getRpmY();
            int xy1 = b.getRpmX() + b.getRpmY();
            return xy0 - xy1; // ascending order
        });

        return new Site[]{slices_in_bbox.get(0), slices_in_bbox.get(slices_in_bbox.size()-1)};
    }

    public int getURAMColHeight(){
        Site[] allURAMSites = dev.getAllCompatibleSites(URAM_SITE_TYPE);
        List<Site> col0 = new ArrayList<>();
        for(Site s : allURAMSites){
            if (s.getInstanceX() == 0)
                col0.add(s);
        }

        return col0.size();
    }

    public int[] getHeight(){
        // get all available sites
        Site[] allDSPSites = dev.getAllCompatibleSites(DSP_SITE_TYPE);
        Site[] RAMB180 = dev.getAllCompatibleSites(BRAM_SITE_TYPE);
        Site[] RAMB181 = dev.getAllCompatibleSites(BRAM_SITE_TYPE2);
        List<Site> RAMB = new ArrayList<>(Arrays.asList(RAMB180));
        RAMB.addAll(Arrays.asList(RAMB181));
        Site[] allBRAMSites = RAMB.toArray(new Site[0]);
        Site[] allURAMSites = dev.getAllCompatibleSites(URAM_SITE_TYPE);
        // get bounding rectangular area
        // get all used sites
        List<Site> usedSites = new ArrayList<>();
        for (int i : phenotype.keySet())
            for (Site[] s : phenotype.get(i))
                usedSites.addAll(Arrays.asList(s));

        int x_min = Integer.MAX_VALUE; int x_max = 0; int y_min = Integer.MAX_VALUE; int y_max = 0;
        for (Site s : usedSites){
            int x = s.getRpmX();
            int y = s.getRpmY();
            if (x < x_min) x_min = x;
            if (x > x_max) x_max = x;
            if (y < y_min) y_min = y;
            if (y > y_max) y_max = y;
        }

        // get all available resources in the rectangular area
        List<Site[]> allSites = new ArrayList<>();
        List<List<Site>> inRect = new ArrayList<>();
        allSites.add(allDSPSites); allSites.add(allBRAMSites); allSites.add(allURAMSites);
        for (int i = 0; i < allSites.size(); i++){
            inRect.add(new ArrayList<>());
            for (Site s : allSites.get(i))
                if (s.getRpmX() >= x_min && s.getRpmX() <= x_max && s.getRpmY() >= y_min && s.getRpmY() <= y_max)
                    inRect.get(i).add(s);
        }// inRect.get(0) -> dsp, inRect.get(1) -> bram,  inRect.get(2) -> uram

        List<Site> inRect_dsp_col = inRect.get(0).stream()
                .filter(s -> s.getInstanceX()==0).collect(Collectors.toList());
        List<Site> inRect_bram_col = inRect.get(1).stream()
                .filter(s -> s.getInstanceX()==0).collect(Collectors.toList());
        List<Site> inRect_uram_col = inRect.get(2).stream()
                .filter(s -> s.getInstanceX()==0).collect(Collectors.toList());

        return new int[]{inRect_dsp_col.size(), inRect_bram_col.size(), inRect_uram_col.size()};

    }

    public Device getDevice(){
        return dev;
    }

}
