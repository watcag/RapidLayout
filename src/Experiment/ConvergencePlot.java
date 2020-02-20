package Experiment;

import main.Tool;

import java.io.*;


public class ConvergencePlot {

    public static void main(String[] args) throws IOException {

        String root = System.getenv("RAPIDWRIGHT_PATH");
        String saveDir = root + "/visual";

        String script = root + "/src/visualize/converge.py";

        Tool.execute_cmd("python3 " + script + " " + saveDir);
    }


}
