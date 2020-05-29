package Utils;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.device.*;

import java.util.*;
import java.util.stream.Collectors;

public class Utility {
    final SiteTypeEnum DSP_SITE_TYPE = SiteTypeEnum.DSP48E2;
    final SiteTypeEnum BRAM_SITE_TYPE = SiteTypeEnum.RAMB180;
    final SiteTypeEnum BRAM_SITE_TYPE2 = SiteTypeEnum.RAMB181;
    final SiteTypeEnum URAM_SITE_TYPE = SiteTypeEnum.URAM288;

    protected Map<Integer, List<Site[]>> phenotype;
    protected Device dev;

    public Utility(Map<Integer, List<Site[]>> phenotype, String device){
        this.phenotype = phenotype;
        // Initiate Device
        Design d = new Design("main", device);
        this.dev = d.getDevice();
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
