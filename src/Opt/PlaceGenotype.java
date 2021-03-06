package Opt;

import org.opt4j.core.Genotype;
import org.opt4j.core.genotype.DoubleGenotype;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Random;

public class PlaceGenotype<V> extends DoubleGenotype {
    private static final long serialVersionUID = 1L;

    protected final List<List<V>> available;

    public PlaceGenotype(List<List<V>> available) {
        super(0, 100);
        this.available = available;
    }

    public List<List<V>> getSites(){
        return available;
    }


    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <G extends Genotype> G newInstance() {
        try {
            Constructor<? extends PlaceGenotype> cstr = this.getClass().getConstructor(List.class);
            return (G) cstr.newInstance(available);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init(Random random, int n) {
        //System.out.println("PlaceGenotype Init is called");
        for (int i = 0; i < n; ++i) {
            double lo = this.getLowerBound(i);
            double hi = this.getUpperBound(i);
            double value = lo + random.nextDouble() * (hi - lo);
            if (i >= this.size()) {
                this.add(value);
            } else {
                this.set(i, value);
            }
        }

    }

    public void init(List<Double> value) {
        for (int i = 0 ; i < value.size(); i++) {
            double lo = this.getLowerBound(i);
            double hi = this.getUpperBound(i);
            double v = lo + value.get(i) * (hi - lo);
            if (i >= this.size()) {
                this.add(v);
            } else {
                this.set(i, v);
            }
        }
    }
}
