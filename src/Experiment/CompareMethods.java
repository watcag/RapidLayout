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

    public static void collect_specs_data(String method) throws IOException {
        Properties prop = Tool.getProperties();
        String device = prop.getProperty("device");
        MinRect mr = new MinRect(device, 18, 8, 2);
        int blocknum = mr.getBlocknum();

        String output = System.getenv("RAPIDWRIGHT_PATH") + "/result/" + method + "_perf.txt";
        PrintWriter pr = new PrintWriter(new FileWriter(output, true), true);

        // for convergence data collection
        // turn on convergence data collection
        changeProperty("collect_converge_data", "true");

        // optimization parameters
        int population = 5;
        int parents = 20;
        int children = 50;
        double crossoverR = 0.98;
        int x_min = 0;
        int x_max = 6000; // use all columns
        int y_min = 0;
        int y_max = mr.getYmax();

        // how many experiments left to do:
        int expr_done = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(output))) {
            while (br.readLine() != null)
               expr_done += 1;
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < 100 - expr_done; i++) {

            System.out.println("Method: " + method + "Optimization: " + i);

            /*  --- find placement solution ---  */
            long start = System.currentTimeMillis();
            Map<Integer, List<Site[]>> result = AutoPlacement.find_solution(
                    method, blocknum, false, device,
                    population, parents, children, crossoverR,
                    x_min, x_max, y_min, y_max);
            long end = System.currentTimeMillis();


            Utils.Utility U = new Utility(result, device);

            double runtime = (end-start)/1e3;
            double size = U.getMaxBBoxSize();
            double wirelength = U.getUnifiedWireLength();

            pr.println(runtime + " " + size + " " + wirelength);
        }

        pr.close();

    }

    public static void main(String[] args) throws IOException {
        String[] methods = {"CMA", "EA", "EA-reduced", "SA"};
        for (String method : methods){
            collect_specs_data(method);
        }
        String root = System.getenv("RAPIDWRIGHT_PATH");
        String script =  root + "/src/visualize/compare.py";
        String dataPath = root + "/result";
        String savePath = root + "/visual";
        Tool.execute_cmd("python3 " + script + " " + dataPath + " " + savePath);
    }
}
