package Opt;

import Utils.Utility;
import com.google.inject.Inject;
import org.opt4j.core.Objective;
import org.opt4j.core.Objectives;
import org.opt4j.core.problem.Evaluator;

import com.xilinx.rapidwright.device.*;
import org.opt4j.core.start.Constant;

import java.util.*;

public class PlaceEvaluator implements Evaluator <Map<Integer, List<Site[]>>> {

    String device = "xcvu37p";
    String method = "EA";

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

        Utility U = new Utility(phenotype, device);

        double unifWireLen = U.getUnifiedWireLength();
        double maxSize = U.getMaxBBoxSize();

        // Observe Evaluation of Current Individual
        Objectives objectives = new Objectives();
        if (method.equals("EA"))
        {
            objectives.add("Spread", Objective.Sign.MIN, maxSize);
            objectives.add("unifWireLength", Objective.Sign.MIN, unifWireLen * unifWireLen);
        } else if (method.equals("GA")) {
            objectives.add("size", Objective.Sign.MIN, maxSize);
        } else {
            objectives.add("Spread", Objective.Sign.MIN, maxSize);
            objectives.add("unifWireLength", Objective.Sign.MIN, unifWireLen * unifWireLen);
            objectives.add("product", Objective.Sign.MIN, unifWireLen * unifWireLen * maxSize);
        }


        return objectives;
    }

}
