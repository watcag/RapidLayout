package cma;

import org.apache.commons.math3.stat.interval.AgrestiCoullInterval;

import java.util.*;

public class PlaceCreator {

    public static double[] getInitial(int block_num){
        Random  random = new Random(1);
        List<Double> guess = new ArrayList<>();
        for (int i = 0 ; i < block_num * 5; i++){
            guess.add(random.nextDouble());
        }
        Double[] Guess = guess.toArray(new Double[0]);
        // unpack
        return Arrays.stream(Guess).mapToDouble(Double::doubleValue).toArray();
    }
}
