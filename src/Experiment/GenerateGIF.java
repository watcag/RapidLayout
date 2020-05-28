package Experiment;

import Experiment.optimize.*;
import main.AutoPlacement;
import main.MinRect;
import main.Tool;

import java.io.IOException;
import java.util.Properties;

import static main.Tool.changeProperty;

public class GenerateGIF {

    public static void main(String[] args) throws IOException {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));

        String device = "vu11p";
        Tool.changeProperty("transfer", "false");

        int mode = 2;
        final boolean visual = false;
        int iteration = 1;

        NSGA.call(device, visual, mode, iteration);
        NSGAR.call(device, visual, mode, iteration);
        CMAES.call(device, mode, iteration);
        SA.call(device, visual, mode, iteration);
        GA.call(device, visual, mode, iteration);
    }
}
