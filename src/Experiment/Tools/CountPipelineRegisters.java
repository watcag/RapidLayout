package Experiment.Tools;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.edif.EDIFCell;
import com.xilinx.rapidwright.edif.EDIFCellInst;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class CountPipelineRegisters {


    public static void count_register(String checkpoint) throws IOException {

        Design design = Design.readCheckpoint(checkpoint);

        List<EDIFCellInst> cellInsts = design.getNetlist().getAllLeafCellInstances();

        int reg_num = 0;

        for (EDIFCellInst cellInst : cellInsts) {
            EDIFCell cellType = cellInst.getCellType();
            if (cellType.isPrimitive()) {
                String name = cellType.getName();
                if (name.equals("FDRE"))
                    reg_num++;
            }
        }

        System.out.println(checkpoint);
        System.out.println(" #FF  = " + reg_num);

    }

    public static void main(String[] args) throws IOException {
        // set up env variable
        if (System.getenv("RAPIDWRIGHT_PATH") == null)
            System.setProperty("RAPIDWRIGHT_PATH", System.getProperty("user.home") + "/RapidWright");
        else
            System.setProperty("RAPIDWRIGHT_PATH", System.getenv("RAPIDWRIGHT_PATH"));

        Scanner input = new Scanner(System.in);

        String design = "";
        new File(design);
        File file;
        do {
            System.out.println("Please input the design checkpoint DCP file: ");
            design = input.next();
            file = new File(design);

        } while (!file.exists());

        count_register(design);
    }
}
