package cma;

import org.apache.commons.math3.stat.interval.AgrestiCoullInterval;

import java.util.*;

public class PlaceCreator {

    public static double[] getInitial(int block_num){
        Random  random = new Random();
        List<Double> guess = new ArrayList<>();
        for (int i = 0 ; i < block_num * 5; i++){
            guess.add(random.nextDouble());
        }
        Double[] Guess = guess.toArray(new Double[0]);
        // unpack
        return Arrays.stream(Guess).mapToDouble(Double::doubleValue).toArray();
    }

/*    public static double[] getInitial(int block_num) {
        List<Double> guess = new ArrayList<>();
        for (int i= 0; i < block_num * 5; i++) {
            guess.add(1D * i);
        }
        Double[] Guess = guess.toArray(new Double[0]);
        // unpack
        return Arrays.stream(Guess).mapToDouble(Double::doubleValue).toArray();
    }*/

    /*public static double[] getInitial(int block_num) {
        List<Double> guess = new ArrayList<>();
        for (int i = block_num*2; i > 0; i--) {
            guess.add(1D * i);
        }
        for (int i = block_num * 2; i > 0; i--) {
            guess.add(1D * i);
        }
        for (int i = block_num; i > 0; i--)
            guess.add(1D * i);

        Double[] Guess = guess.toArray(new Double[0]);
        return Arrays.stream(Guess).mapToDouble(Double::doubleValue).toArray();
    }*/
}
