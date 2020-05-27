package Experiment;

import Experiment.optimize.*;
import main.Tool;

import java.io.*;


public class Convergence {

    public static void main(String[] args) throws IOException {

        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));

        String device = "vu11p";

        int mode = 1;
        final boolean visual = false;

        NSGA.call(device, visual, mode);
        NSGAR.call(device, visual, mode);
        CMAES.call(device, mode);
        SA.call(device, visual, mode);
        GA.call(device, visual, mode);
    }


}
