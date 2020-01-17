package OptReduced;

import Utils.Utils;
import com.google.inject.Inject;
import org.opt4j.core.Objective;
import org.opt4j.core.Objectives;
import org.opt4j.core.problem.Evaluator;

import com.xilinx.rapidwright.device.*;
import org.opt4j.core.start.Constant;

import java.util.*;

public class PlaceEvaluator implements Evaluator <Map<Integer, List<Site[]>>> {

    String device = "xcvu37p";
    String method = "EA-reduced";

    @Inject
    public void setDevice(@Constant(value = "device") String device){
        this.device = device;
    }

    @Inject
    public void setMethod(@Constant(value = "method") String method) {this.method = method;}

    @Override
    public Objectives evaluate(Map<Integer, List<Site[]>> phenotype){

        Set<Integer> keys = phenotype.keySet();
        List<Site> dspSite = new ArrayList<>();
        List<Site> bramSite = new ArrayList<>();
        List<Site> uramSite = new ArrayList<>();

        for (Integer i : keys){
            dspSite.addAll(Arrays.asList(phenotype.get(i).get(0)));
            bramSite.addAll(Arrays.asList(phenotype.get(i).get(1)));
            uramSite.addAll(Arrays.asList(phenotype.get(i).get(2)));
        }

        Utils U = new Utils(phenotype, device);

        double areaPerBlock = U.AreaPerBlock();
        double unifWireLen = U.getUnifiedWireLength();
        double bottomLeft = U.getCoord();
        double maxWireLen = U.getMaxWireLength();
        double maxRange = U.getMaxSpread();
        double maxArea = U.getMaxArea();
        double maxSize = U.getMaxBBoxSize();
        double[] utilization = U.getUtilization();
        double value = (areaPerBlock/100) * (unifWireLen/100) * (bottomLeft/10);
        if (U.checkDuplicate()){
            value *= 1000;
            utilization[0] = 0;
            utilization[1] = 0;
            utilization[2] = 0;
        }

        // Observe Evaluation of Current Individual
        Objectives objectives = new Objectives();
        if (method.equals("EA-reduced"))
        {
            objectives.add("Spread", Objective.Sign.MIN, maxSize);
            objectives.add("unifWireLength", Objective.Sign.MIN, unifWireLen * unifWireLen);
            //System.out.println("two objectives");
        } else {
            objectives.add("Spread", Objective.Sign.MIN, maxSize);
            objectives.add("unifWireLength", Objective.Sign.MIN, unifWireLen * unifWireLen);
            objectives.add("product", Objective.Sign.MIN, unifWireLen * unifWireLen * maxSize);
            //System.out.println("three objectives -> one objective");
        }


        return objectives;
    }

}
