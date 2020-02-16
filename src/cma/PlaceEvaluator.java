package cma;

import Utils.Utility;
import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.device.SiteTypeEnum;
import org.apache.commons.math3.analysis.MultivariateFunction;

import java.util.List;
import java.util.Map;

public class PlaceEvaluator {

    private static Map<SiteTypeEnum, List<Site[]>> selectedSites;
    private static String device;

    final static MultivariateFunction fitnessFunction = new MultivariateFunction() {
        @Override
        public double value(double[] doubles) {
            Map<Integer, List<Site[]>> solution = PlaceDecoder.decode(doubles, selectedSites);
            Utility U = new Utility(solution, device);
            double wireLength = U.getUnifiedWireLength();
            double spread = U.getMaxSpread();
            double size = U.getMaxBBoxSize();
            //System.out.println("wire length = " + wireLength + ", \t spread = " + spread);
            //System.out.println(wireLength + " " + spread);
            //System.out.println("area = " + area);
            return wireLength * wireLength * size;
        }
    };


    public PlaceEvaluator(Map<SiteTypeEnum, List<Site[]>> selectedSites, String device){
        PlaceEvaluator.selectedSites = selectedSites;
        PlaceEvaluator.device = device;
    }

    public MultivariateFunction getFitnessFunction(){
        return fitnessFunction;
    }

}
