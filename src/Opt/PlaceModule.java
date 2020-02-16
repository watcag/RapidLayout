package Opt;

import com.xilinx.rapidwright.device.Site;
import org.opt4j.core.problem.ProblemModule;
import org.opt4j.core.start.Constant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaceModule extends ProblemModule {

    @Constant(value = "block_num")
    protected int block_num = 10;

    @Constant(value="device")
    protected String device = "xcvu37p";

    @Constant(value="x_min")
    protected int x_min = 0;
    
    @Constant(value = "x_max")
    protected int x_max = 5000;
    
    @Constant(value = "y_min")
    protected int y_min = 0;
    
    @Constant(value = "y_max")
    protected int y_max = 1500;

    @Constant(value = "method")
    protected String method = "EA";

    @Constant(value = "prev_placement")
    protected String prev_placement = "";

    public int getBlock_num(){ return block_num; }
    public void setBlock_num(int block_num){ this.block_num = block_num; }

    public String getDevice() {return device;}
    public void setDevice(String device) {this.device = device;}
    
    public int getX_min(){return x_min;}
    public void setX_min(int x_min){this.x_min = x_min;}

    public int getX_max(){return x_max;}
    public void setX_max(int x_max){this.x_max = x_max;}

    public int getY_min(){return y_min;}
    public void setY_min(int y_min){this.y_min = y_min;}

    public int getY_max(){return y_max;}
    public void setY_max(int y_max){this.y_max = y_max;}

    public void setMethod(String method){this.method = method;}
    public String getMethod(){return method;}

    public void setPrev_placement(String prev_placement){this.prev_placement = prev_placement;}
    public String getPrev_placement(){return this.prev_placement;}


    protected void config (){
        bindProblem(PlaceCreator.class, PlaceDecoder.class, PlaceEvaluator.class);
    }
}
