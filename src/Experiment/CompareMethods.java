package Experiment;

import Utils.Utility;
import com.xilinx.rapidwright.device.Site;
import main.AutoPlacement;
import main.MinRect;
import main.Tool;

import java.io.*;
import java.util.*;

import static main.Tool.changeProperty;

public class CompareMethods {

    private static String device;
    private static String root;
    private static String result;

    /* Function: collect specs data
       Evaluate the performance of **one** optimization method
       and save the result metrics
       output file: ./result/<method>_perf.txt
    */
    public static void collect_specs_data(String method) throws IOException {

    }

    public static void main(String[] args) throws IOException {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));

        root = System.getProperty("RAPIDWRIGHT_PATH") + "/";
        result = root + "result/";

        device = "vu11p";

        String[] methods = {"CMA", "EA", "EA-reduced", "SA"};
        for (String method : methods){
            collect_specs_data(method);
        }

//        String script =  root + "/src/visualize/compare.py";
//        String dataPath = root + "/result";
//        String savePath = root + "/visual";
//        Tool.execute_cmd("python3 " + script + " " + dataPath + " " + savePath);
    }
}
