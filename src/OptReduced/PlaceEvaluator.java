package OptReduced;

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
    String method = "EA-reduced";

    @Inject
    public void setDevice(@Constant(value = "device") String device){
        this.device = device;
    }

    @Inject
    public void setMethod(@Constant(value = "method") String method) {this.method = method;}

    @Override
    public Objectives evaluate(Map<Integer, List<Site[]>> phenotype){

        Utility U = new Utility(phenotype, device);

        double unifWireLen = U.getUnifiedWireLength();
        double maxSize = U.getMaxBBoxSize();

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
