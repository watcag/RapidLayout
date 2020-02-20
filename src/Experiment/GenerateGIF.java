package Experiment;

import main.AutoPlacement;
import main.MinRect;
import main.Tool;

import java.io.IOException;
import java.util.Properties;

import static main.Tool.changeProperty;

public class GenerateGIF {

    public static void collect_gif_data(String method) throws IOException {

        Properties prop = Tool.getProperties();
        String device = prop.getProperty("device");
        MinRect mr = new MinRect(device, 18, 8, 2);
        int blocknum = mr.getBlocknum();

        changeProperty("collect_gif_data", "true");

        // optimization parameters
        int population = 5;
        int parents = 20;
        int children = 50;
        double crossoverR = 0.98;
        int x_min = 0;
        int x_max = 6000; // use all columns
        int y_min = 0;
        int y_max = mr.getYmax();
        /*  --- find placement solution ---  */
        AutoPlacement.find_solution(
                method, blocknum, false, device,
                population, parents, children, crossoverR,
                x_min, x_max, y_min, y_max);
    }


    public static void main(String[] args) throws IOException {
        String root = System.getenv("RAPIDWRIGHT_PATH");

        String[] methods = {"CMA", "EA", "EA-reduced", "SA"};
        for (String method : methods)
            collect_gif_data(method);

        String script = root + "/src/visualize/createGIF.py";
        Tool.execute_cmd("python3 " + script);
    }
}
