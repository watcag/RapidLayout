package Experiment;

import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.device.SiteTypeEnum;

import java.util.*;

public class DeviceMap {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    private static final SiteTypeEnum DSP_SITE_TYPE = SiteTypeEnum.DSP48E2;
    private static final SiteTypeEnum BRAM_SITE_TYPE = SiteTypeEnum.RAMB180;
    private static final SiteTypeEnum URAM_SITE_TYPE = SiteTypeEnum.URAM288;

    private static Map<SiteTypeEnum, boolean[][]> usage_map = new HashMap<>();
    private static Map<SiteTypeEnum, Site[][]> site_map = new HashMap<>();

    public void printCurrentMapStatus() {
        SiteTypeEnum[] types = new SiteTypeEnum[]{DSP_SITE_TYPE, BRAM_SITE_TYPE, URAM_SITE_TYPE};
        for (int t=0; t<3; t++) {
            SiteTypeEnum type = types[t];
            System.out.println("SiteType: " + type.name());

            boolean[][] avail = usage_map.get(type);
            Site[][] sites = site_map.get(type);
            int col = sites.length;
            int row = sites[0].length;

            for (int i = 0; i < col; i++) {
                for (int j = 0; j < row; j++) {
                    boolean placed = avail[i][j];
                    String name = sites[i][j].getName();
                    name = placed ? ANSI_GREEN + name + ANSI_RESET : ANSI_CYAN + name + ANSI_RESET;
                    System.out.print(name + "\t");
                }
                System.out.print("\n");
            }

            System.out.println("--------------------------\n\n");

        }
    }

    public int getColNum(SiteTypeEnum type) { return site_map.get(type).length; }

    public int getRowNum(SiteTypeEnum type) { return site_map.get(type)[0].length; }

    public Site getSite(SiteTypeEnum type, int col_idx, int row_idx) {
        return site_map.get(type)[col_idx][row_idx];
    }

    public void setSitePlaced(SiteTypeEnum type, int col_idx, int row_idx) {
        usage_map.get(type)[col_idx][row_idx] = true;
    }

    public DeviceMap(Map<SiteTypeEnum, List<List<Site>>> availSites) {

        // get site maps for each type
        SiteTypeEnum[] types = new SiteTypeEnum[]{DSP_SITE_TYPE, BRAM_SITE_TYPE, URAM_SITE_TYPE};

        for (int t=0; t<3; t++) {
            // initialize usage maps
            SiteTypeEnum type = types[t];
            List<List<Site>> sites = availSites.get(type);
            int num_col = sites.size();
            int num_row = sites.get(0).size();
            boolean[][] map = new boolean[num_col][num_row];
            for (int i=0; i<num_col; i++)
                for (int j=0; j<num_row; j++)
                    map[i][j] = false;
            usage_map.put(type, map);

            // initialize site map
            Site[][] currSiteMap = new Site[num_col][num_row];
            for (int i=0; i<num_col; i++)
                for (int j=0; j<num_row; j++) {
                    Site s = sites.get(i).get(j);
                    int col_idx = s.getInstanceX();
                    int row_idx = s.getInstanceY();
                    currSiteMap[col_idx][row_idx] = s;
                }
            site_map.put(type, currSiteMap);
        }
    }

    public int spaceAtNorth(SiteTypeEnum type, Site s) {
        // that means we count downloads
        int counter = 0;
        for (int row = s.getInstanceY(); row < getRowNum(type); row++) {
            int col = s.getInstanceX();
            if (!usage_map.get(type)[col][row])
                counter++;
            else
                break;
        }

        return counter;
    }

    public int spaceAtSouth(SiteTypeEnum type, Site s) {
        // that means we count downwards
        int counter = 0;
        for (int row = s.getInstanceY(); row > 0; row--) {
            int col = s.getInstanceX();
            if (!usage_map.get(type)[col][row])
                counter++;
            else
                break;
        }

        return counter;
    }

    public List<Site> MoveAbit(SiteTypeEnum type, Random rand, Site[] orig) {
        // orig is ordered, from bottom to top

        List<Site> moved = new ArrayList<>();
        int northSpace = spaceAtNorth(type, orig[orig.length-1]);
        int southSpace = spaceAtSouth(type, orig[0]);

        int move = 0;
        if (northSpace + southSpace > 0)
            move = rand.nextInt(northSpace + southSpace);
        move -= southSpace;

        for (int i=0; i < orig.length; i++) {
            int row = orig[i].getInstanceY();
            int col = orig[i].getInstanceX();
            usage_map.get(type)[col][row] = false;
            row += move;
            // debug info
//            System.out.println("row = " + row + " bound = " + getRowNum(type) +
//                    " move = " + move + " north=" + northSpace + " south = " + southSpace);
            Site s = getSite(type, col, row);
            moved.add(s);
            usage_map.get(type)[col][row] = true;
        }

        return moved;
    }
}
