package Opt;

import org.opt4j.core.Genotype;
import org.opt4j.core.genotype.DoubleGenotype;

import java.lang.reflect.Constructor;
import java.util.List;

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
}
