package OptReduced;

import com.xilinx.rapidwright.device.Site;
import org.opt4j.core.Genotype;
import org.opt4j.core.genotype.PermutationGenotype;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class myGenotype<V> extends PermutationGenotype<V> {
    private static final long serialVersionUID = 1L;

    protected final List<List<Site>> available;

    public myGenotype(List<List<Site>> available) {
        super();
        this.available = available;
    }

    public List<List<Site>> getSites(){
        return available;
    }


    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <G extends Genotype> G newInstance() {
        try {
            Constructor<? extends myGenotype> cstr = this.getClass().getConstructor(List.class);
            return (G) cstr.newInstance(available);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
