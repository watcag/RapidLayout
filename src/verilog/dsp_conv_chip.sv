module dsp_conv_chip #(
        parameter KERN_SZ = 3,
        parameter IMG_W = 4,
        parameter IMG_D = 6,
	parameter A_W = 14,
	parameter M_W = 18,
	parameter D_W = 48,
	parameter URAM_D_W = 72,
	parameter URAM_A_W = 23,
    parameter Y = 480,
    parameter NUMBER_OF_REG= 1
)
(
input clk,
input rst,
input ce,
input [URAM_A_W-1:0] uram1_wr_addr [Y],
input [URAM_D_W-1:0] uram1_wr_data [Y],
input uram1_wr_en [Y],
input [URAM_A_W-1:0] uram2_rd_addr_external [Y],
input read_en_external [Y],
output [URAM_D_W-1:0] uram2_rd_data [Y],
input ld_new_kernel [Y],
input [A_W-1:0] krnl_bram1_wraddr [Y],
input [M_W-1:0] krnl_bram1_wrdata [Y],
input krnl_bram1_wren [Y],
input [A_W-1:0] krnl_bram2_wraddr [Y],
input [M_W-1:0] krnl_bram2_wrdata [Y],
input krnl_bram2_wren [Y],
input	[22:0]	addr_chain [Y],
input	[8:0]	bwe_chain  [Y],  
input	[0:0]	dbiterr_chain [Y],
input	[71:0]	din_chain [Y],
input	[71:0]	dout_chain [Y],        
input	[0:0]	en_chain [Y],          
input	[0:0]	rdacess_chain [Y],
input	[0:0]	rdb_wr_chain [Y],
input	[0:0]	sbiterr_chain [Y]

);


genvar y;

generate for (y = 0; y < Y; y = y + 1) begin : name
 (* dont_touch = "true" *)  dsp_conv_top #(
         .KERN_SZ  (KERN_SZ)
        ,.IMG_W    (IMG_W)
        ,.IMG_D    (IMG_D)
	,.A_W      (A_W)
	,.M_W      (M_W)
	,.D_W      (D_W)
        ,.CASCADE_ORDER_A_RDURAM ("FIRST")
        ,.CASCADE_ORDER_A_WRURAM ("LAST")
        ,.NUMBER_OF_REG (NUMBER_OF_REG)
	,.URAM_D_W (URAM_D_W)
	,.URAM_A_W (URAM_A_W)
  )
  dut (
         .clk                   (clk)
        ,.rst                   (rst)
        ,.ce                    (ce)
        ,.uram1_wr_addr         (uram1_wr_addr[y])
        ,.uram1_wr_data         (uram1_wr_data[y])
        ,.uram1_wr_en           (uram1_wr_en[y])
        ,.uram2_rd_addr_external (uram2_rd_addr_external[y])
        ,.read_en_external      (read_en_external[y])
        ,.uram2_rd_data         (uram2_rd_data[y])
        ,.krnl_bram1_wraddr     (krnl_bram1_wraddr[y])
        ,.krnl_bram1_wrdata     (krnl_bram1_wrdata[y])
        ,.krnl_bram1_wren       (krnl_bram1_wren[y])
        ,.krnl_bram2_wraddr     (krnl_bram2_wraddr[y])
        ,.krnl_bram2_wrdata     (krnl_bram2_wrdata[y])
        ,.krnl_bram2_wren       (krnl_bram2_wren[y])
//        ,.CAS_OUT_ADDR          (addr_chain[y+1])
//        ,.CAS_OUT_BWE           (bwe_chain[y+1])
//        ,.CAS_OUT_DBITERR       (dbiterr_chain[y+1])
//        ,.CAS_OUT_DIN           (din_chain[y+1])
//        ,.CAS_OUT_DOUT          (dout_chain[y+1])
//        ,.CAS_OUT_EN            (en_chain[y+1])
//        ,.CAS_OUT_RDACCESS      (rdacess_chain[y+1])
//        ,.CAS_OUT_RDB_WR        (rdb_wr_chain[y+1])
//        ,.CAS_OUT_SBITERR       (sbiterr_chain[y+1])
        ,.CAS_IN_ADDR           (addr_chain[y])
        ,.CAS_IN_BWE            (bwe_chain[y])
        ,.CAS_IN_DBITERR        (dbiterr_chain[y])
        ,.CAS_IN_DIN            (din_chain[y])
        ,.CAS_IN_DOUT           (dout_chain[y])
        ,.CAS_IN_EN             (en_chain[y])
        ,.CAS_IN_RDACCESS       (rdacess_chain[y])
        ,.CAS_IN_RDB_WR         (rdb_wr_chain[y])
        ,.CAS_IN_SBITERR        (sbiterr_chain[y])
  );
end
endgenerate


endmodule

