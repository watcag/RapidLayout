package main;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.device.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MinRect {

    private static final SiteTypeEnum DSP_SITE_TYPE = SiteTypeEnum.DSP48E2;
    private static final SiteTypeEnum BRAM_SITE_TYPE = SiteTypeEnum.RAMB180;
    private static final SiteTypeEnum URAM_SITE_TYPE = SiteTypeEnum.URAM288;

    Device device;
    int dsp;
    int bram;
    int uram;
    int ymax;
    int blockNum;
    int replication;

    public MinRect(String device, int dsp, int bram, int uram) {
        Design d = new Design("tmp", device);
        this.device = d.getDevice();

        this.dsp = dsp;
        this.bram = bram;
        this.uram = uram;

        /* max number of conv blocks for entire FPGA */
        Map<SiteTypeEnum, List<List<Site>>> map = Opt.PlaceCreator.getAllAvailableSites(this.device);

        System.out.println("All Available Hard Blocks on FPGA: ");
        System.out.println("DSP: " + map.get(DSP_SITE_TYPE).size() + " x " + map.get(DSP_SITE_TYPE).get(0).size());
        System.out.println("BRAM: " + map.get(BRAM_SITE_TYPE).size() + " x " + map.get(BRAM_SITE_TYPE).get(0).size());
        System.out.println("URAM: " + map.get(URAM_SITE_TYPE).size() + " x " + map.get(URAM_SITE_TYPE).get(0).size());
        System.out.println("number of SLR: " + this.device.getSLRs().length);
        // int max_dsp_num = map.get(DSP_SITE_TYPE).get(0).size() / 9 * map.get(DSP_SITE_TYPE).size() * 2;
        // int max_bram_num = map.get(BRAM_SITE_TYPE).get(0).size() / 4 * map.get(BRAM_SITE_TYPE).size() * 2;
        int max_dsp_num = map.get(DSP_SITE_TYPE).get(0).size() / 9 * map.get(DSP_SITE_TYPE).size() / 2;
        int max_bram_num = map.get(BRAM_SITE_TYPE).get(0).size() / 4 * map.get(BRAM_SITE_TYPE).size() / 2;
        int max_uram_num = map.get(URAM_SITE_TYPE).get(0).size() / 2 * map.get(URAM_SITE_TYPE).size();
        int blocks = Math.min(Math.min(max_dsp_num, max_bram_num), max_uram_num);
        System.out.println("max number of block = " + blocks);

        int numSLR = this.device.getNumOfSLRs();
        int blocksPerSLR = blocks / numSLR;

        int ymax = 0;
        SLR SLR0 = this.device.getSLR(0);
        List<Site> ulsites = new ArrayList<>();
        for (Tile t : this.device.getAllTiles())
            if (SLR0.containsTile(t))
                ulsites.addAll(Arrays.asList(t.getSites()));

        for (Site s : ulsites) {
            SiteTypeEnum type = s.getSiteTypeEnum();
            if (type != SiteTypeEnum.DSP48E2 &&
                    type != SiteTypeEnum.RAMB181 &&
                    type != SiteTypeEnum.RAMB180 &&
                    type != SiteTypeEnum.URAM288)
                continue;
            if (s.getRpmY() >= ymax)
                ymax = s.getRpmY();
        }


        for (int blockn = blocksPerSLR; blockn % 2 == 0; blockn = blockn / 2){
            int dsp_needed = blockn * dsp;
            int bram_needed = blockn * bram;
            int uram_needed = blockn * uram;
            int divisor = blocksPerSLR / blockn;
            int y = ymax / divisor;
            Map<SiteTypeEnum, List<List<Site>>> avail =
                    Opt.PlaceCreator.getAvailableSites(this.device, 0, 10000, 0, y);
            int dsp_avail = avail.get(DSP_SITE_TYPE).get(0).size()/9 * avail.get(DSP_SITE_TYPE).size() * 9;
            int bram_avail = avail.get(BRAM_SITE_TYPE).get(0).size()/4 * avail.get(BRAM_SITE_TYPE).size() * 4;
            int uram_avail = avail.get(URAM_SITE_TYPE).get(0).size()/2 * avail.get(URAM_SITE_TYPE).size() * 2;
            boolean fit = dsp_needed <= dsp_avail && bram_needed <= bram_avail && uram_needed <= uram_avail;

            if (fit) {
                this.blockNum = blockn;
                this.ymax = y;
                this.replication = divisor;
            }
        }

        if (this.blockNum == 0) { // if the blockn is indivisible by 2
            this.blockNum = blocksPerSLR;
            this.ymax = ymax;
            this.replication = 1;
        }
    }

    public int getBlocknum() {
        return this.blockNum;
    }

    public int getYmax() {
        return this.ymax;
    }

    public int getReplication() {
        return this.replication;
    }


    public static void main(String[] args) {
        MinRect mr = new MinRect("vu11p", 18, 8, 2);
        int blockNum = mr.getBlocknum();
        int Ymax = mr.getYmax();

        System.out.println("get block num = " + blockNum);
        System.out.println("get Y max = " + Ymax);
    }

}
