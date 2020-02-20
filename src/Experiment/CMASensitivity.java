package Experiment;

import main.Tool;

import java.io.IOException;

public class CMASensitivity {

    public static void main(String[] args) throws IOException {

        // sensitivity analysis data collection
        cma.cmaes.main(args);

        // draw plot
        String script = System.getenv("RAPIDWRIGHT_PATH") + "/src/visualize/sensitivity.py";
        Tool.execute_cmd("python3 " + script);
    }
}
