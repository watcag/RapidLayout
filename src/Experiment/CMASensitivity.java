package Experiment;

import main.Tool;

import java.io.IOException;

public class CMASensitivity {

    public static void main(String[] args) throws IOException {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));

        // sensitivity analysis data collection
        cma.cmaes.main(args);

        // draw plot
        String script = System.getenv("RAPIDWRIGHT_PATH") + "/src/visualize/sensitivity.py";
        Tool.execute_cmd("python3 " + script);
    }
}
