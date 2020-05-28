package Experiment;

import Experiment.optimize.*;
import main.Tool;

import java.io.*;

public class CompareMethods {

    public static void main(String[] args) throws IOException {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));

        String device = "vu11p";

        Tool.changeProperty("transfer", "false");

        int mode = 0;
        final boolean visual = false;

        int iteration = 1;

        NSGA.call(device, visual, mode, iteration);
        NSGAR.call(device, visual, mode, iteration);
        CMAES.call(device, mode, iteration);
        SA.call(device, visual, mode, iteration);
        GA.call(device, visual, mode, iteration);
        UTPlaceF.main(new String[0]);
        VPR.main(new String[0]);

//        String script =  root + "/src/visualize/compare.py";
//        String dataPath = root + "/result";
//        String savePath = root + "/visual";
//        Tool.execute_cmd("python3 " + script + " " + dataPath + " " + savePath);
    }
}
