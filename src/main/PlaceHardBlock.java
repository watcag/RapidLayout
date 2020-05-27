package main;

import com.xilinx.rapidwright.design.*;
import com.xilinx.rapidwright.design.blocks.PBlock;
import com.xilinx.rapidwright.device.*;
import com.xilinx.rapidwright.edif.EDIFCell;
import com.xilinx.rapidwright.edif.EDIFCellInst;
import com.xilinx.rapidwright.edif.EDIFNet;
import com.xilinx.rapidwright.edif.EDIFTools;

import java.util.HashSet;
import java.util.Set;


public class PlaceHardBlock {


    // Low-Level Placement Functions

    public static void placeDSP48E2(Design d, String dspSite, String dsp48InstName) {

        // logical DSP48E2 instance

        EDIFCellInst dsp48Inst = d.getNetlist().getCellInstFromHierName(dsp48InstName);

        // The DSP48E2 is a macro that decomposes into several primitives

        // that each get placed separately

        Site origin = d.getDevice().getSite(dspSite);

        for(BEL elem : origin.getBELs()){

            // Skip any routing BELs / Ports

            if(elem.getBELClass() != BELClass.BEL) continue;

            // Relative name of the primitive to be placed onto the BEL

            String instName = elem.getName() +"_INST";

            EDIFCellInst belInst = dsp48Inst.getCellType().getCellInst(instName);

            if (belInst == null){
                d.createAndPlaceCell(dsp48Inst.getCellType(), dsp48InstName + EDIFTools.EDIF_HIER_SEP + instName, Unisim.valueOf(elem.getName()), origin, elem);
                System.out.println("non-existing DSP children cells at " + dsp48InstName + " : " + instName);
            }
            else{
                // Since the logical cell instance already exists, we have to separate APIs
                Cell c = d.createCell(dsp48InstName + EDIFTools.EDIF_HIER_SEP + instName, belInst);
                d.placeCell(c, origin, elem);
            }


        }

        SiteInst si = d.getSiteInstFromSite(origin);
        EDIFNet edifNet = dsp48Inst.getParentCell().getNet("acin0_reg_reg_n_0_[0]");
        Net net = d.createNet(edifNet);
        si.routeIntraSiteNet(net, origin.getBELPin("OPMODE8"), origin.getBELPin("DSP_ALU/OPMODE8"));
        si.routeIntraSiteNet(net, origin.getBELPin("OPMODE7"), origin.getBELPin("DSP_ALU/OPMODE7"));

    }


    public static PBlock placeBRAM(Design d, String bramSite, String bramCellName, String parentName) {
        Set<Site> used = new HashSet<>();

        int Y = Integer.parseInt(bramSite.substring(bramSite.indexOf("Y")+1));

        Device dev = d.getDevice();
        Site dstSite = dev.getSite(bramSite);
        SiteInst si = Y % 2 == 0 ?
                d.createSiteInst(bramSite, SiteTypeEnum.RAMB180, dstSite) :
                d.createSiteInst(bramSite, SiteTypeEnum.RAMB181, dstSite);
        if (si == null) {
            System.out.println(bramSite);
            System.out.println(bramCellName);
            System.out.println(dstSite.getName());
            System.out.println("ERROR: Invalid Solution");
        }
        BEL bel = Y % 2 == 0 ?
                si.getBEL("RAMB18E2_L") :
                si.getBEL("RAMB18E2_U");

        EDIFCell parentCell = d.getNetlist().getCell(parentName);
        String[] split = bramCellName.split("/");
        Cell bramCell = d.createCell(bramCellName, parentCell.getCellInst(split[split.length - 1]));
        d.placeCell(bramCell, dstSite, bel);

        si.addCell(bramCell);

        PBlock footprint = new PBlock(d.getDevice(), used);
        return footprint;
    }

    public static PBlock placeURAM(Design d, String uramSite, String uramCellName, String parentName) {
        Set<Site> used = new HashSet<>();

        Device dev = d.getDevice();
        Site origin = dev.getSite(uramSite);
        SiteInst si = d.createSiteInst(uramSite, SiteTypeEnum.URAM288, origin);
        BEL bel = origin.getBEL("URAM_288K_INST");
        EDIFCell parentCell = d.getNetlist().getCell(parentName);
        String[] split = uramCellName.split("/");
        Cell uramCell = d.createCell(uramCellName, parentCell.getCellInst(split[split.length - 1]));
        d.placeCell(uramCell, origin, bel);

        si.addCell(uramCell);

        PBlock footprint = new PBlock(d.getDevice(), used);
        return footprint;
    }
}
