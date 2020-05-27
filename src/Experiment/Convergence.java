package Experiment;

import main.Tool;

import java.io.*;


public class Convergence {

    public static void main(String[] args) throws IOException {

        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));

        String root = System.getenv("RAPIDWRIGHT_PATH");
        String saveDir = root + "/visual";

        String script = root + "/src/visualize/converge.py";

        Tool.execute_cmd("python3 " + script + " " + saveDir);
    }


}
