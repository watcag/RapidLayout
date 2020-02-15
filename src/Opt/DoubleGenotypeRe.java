package Opt;

import org.opt4j.core.Genotype;
import org.opt4j.core.genotype.DoubleGenotype;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Random;

public class DoubleGenotypeRe extends DoubleGenotype {

    private static final long serialVersionUID = 1L;


    public DoubleGenotypeRe() {
        super(0, 100);
    }


    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <G extends Genotype> G newInstance() {
        try {
            Constructor<? extends DoubleGenotypeRe> cstr = this.getClass().getConstructor();
            return (G) cstr.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init(Random random, int n) {
        for (int i = 0; i < n; ++i) {
            double lo = this.getLowerBound(i);
            double hi = this.getUpperBound(i);
            double value = lo + random.nextDouble() * (hi - lo);
            //double value = lo + 0.1 * (hi - lo);
            if (i >= this.size()) {
                this.add(value);
            } else {
                this.set(i, value);
            }
        }

    }


    public void init(List<Double> value) {
        for (int i = 0 ; i < value.size(); i++) {
            double v = value.get(i);
            if (i >= this.size()) {
                this.add(v);
            } else {
                this.set(i, v);
            }
        }
    }

}
