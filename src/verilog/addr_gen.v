module addr_gen #(
        parameter IMG_W = 4,
        parameter NUMBER_OF_REG = 1,
	parameter URAM_A_W = 23,
	parameter BRAM_A_W = 14
)
(
input clk,
input rst,
input ce,
input read_en_from_ce_state_machine,
output reg [URAM_A_W-1:0] uram_rd_addr,
output reg [BRAM_A_W-1:0] b1_wr_addr,
output reg b1_wr_en,
output reg [BRAM_A_W-1:0] b2_wr_addr,
output reg b2_wr_en,
output reg [BRAM_A_W-1:0] b3_wr_addr,
output reg b3_wr_en,
output reg [BRAM_A_W-1:0] b1_rd_addr,
output reg [BRAM_A_W-1:0] b2_rd_addr,
output reg [BRAM_A_W-1:0] b3_rd_addr,
output wire init_done
);

//state machine for controlling URAM
localparam URAM_RST_s       = 7'b0000001;
localparam URAM_Init_s      = 7'b0000010;
localparam URAM_Rd1_s       = 7'b0000100;
localparam URAM_Rd2_s       = 7'b0001000;
localparam URAM_Rd3_s       = 7'b0010000;
localparam URAM_initDone_s  = 7'b0100000;
localparam URAM_RdN_s       = 7'b1000000;

//state machine for controlling BRAM1
localparam BRAM1_RST_s      = 4'b0001;
localparam BRAM1_Init_s     = 4'b0010;
localparam BRAM1_Rd_s       = 4'b0100;
localparam BRAM1_initDone_s = 4'b1000;

//state machine for controlling BRAM2
localparam BRAM2_RST_s      = 4'b0001;
localparam BRAM2_Init_s     = 4'b0010;
localparam BRAM2_Rd_s       = 4'b0100;
localparam BRAM2_initDone_s = 4'b1000;

reg [6:0]     uram_p_state;
reg [6:0]     uram_n_state;
reg [3:0]     bram1_p_state;
reg [3:0]     bram1_n_state;
reg [3:0]     bram2_p_state;
reg [3:0]     bram2_n_state;
reg           uram_rd_en_tmp;
reg [BRAM_A_W-1:0] b1_wr_addr_r1;
reg [BRAM_A_W-1:0] b1_wr_addr_r2;
reg [BRAM_A_W-1:0] b1_wr_addr_r3;
reg [BRAM_A_W-1:0] b1_wr_addr_r4;
reg [BRAM_A_W-1:0] b1_wr_addr_r5;
reg           b1_wr_en_r1;
reg           b1_wr_en_r2;
reg           b1_wr_en_r3;
reg           b1_wr_en_r4;
reg           rd_en_bram1_r1;
reg           rd_en_bram1_r2;
reg           rd_en_bram1_r3;
reg           rd_en_bram1_r4;
reg           rd_en_bram1_r5;
reg           rd_en_bram1_r6;
reg           rd_en_bram2_r1;
reg           rd_en_bram2_r2;
reg           rd_en_bram2_r3;
reg           rd_en_bram2_r4;
reg           rd_en_bram2_r5;
reg           rd_en_bram2_r6;
reg           rd_en_bram2_r7;
reg           rd_en_bram2_r8;
reg           rd_en_bram2_r9;
reg           rd_en_from_uram_for_bram1;
reg           rd_en_from_uram_for_bram2;
reg [BRAM_A_W-1:0] b2_wr_addr_r1;
reg [BRAM_A_W-1:0] b3_wr_addr_r1;
reg           b2_wr_en_r1;
reg           b3_wr_en_r1;
reg           read_en_from_ce_state_machine_r;
reg           read_en_from_ce_state_machine_2r;
reg           read_en_from_ce_state_machine_3r;

wire          rd_en_bram1;
wire          rd_en_bram2;
wire          rd_en_bram3;
wire          uram_rd_en;


//state machine URAM reading
always@(posedge clk) begin
  if (rst) begin
    uram_p_state <= URAM_RST_s;
  end else begin
    uram_p_state <= uram_n_state;
  end
end

always@(*) begin
  case (uram_p_state)
    URAM_RST_s    : uram_n_state <= URAM_Init_s;
    URAM_Init_s   : uram_n_state <= URAM_Rd1_s;
    URAM_Rd1_s    : begin
                      if (uram_rd_addr == 16*(IMG_W-1)) uram_n_state <= URAM_Rd2_s;
                      else uram_n_state <= URAM_Rd1_s;
                    end
    URAM_Rd2_s    : begin
                      if (uram_rd_addr == 16*(2*IMG_W-1)) uram_n_state <= URAM_Rd3_s;
                      else uram_n_state <= URAM_Rd2_s;
                    end
    URAM_Rd3_s    : begin
                      if (uram_rd_addr == 16*(3*IMG_W-1)) uram_n_state <= URAM_initDone_s;
                      else uram_n_state <= URAM_Rd3_s;
                    end
    URAM_initDone_s : /*begin
                        if (read_en_from_ce_state_machine) uram_n_state <= URAM_RdN_s;
                        else uram_n_state <= URAM_initDone_s;
                      end*/ uram_n_state <= URAM_initDone_s;
    //URAM_RdN_s    : begin
    //                  if (read_en_from_ce_state_machine) uram_n_state <= URAM_RdN_s;
    //                  else uram_n_state <= URAM_initDone_s;
    //                end
    default       : uram_n_state <= URAM_RST_s;
  endcase
end

//uram rd addr
always@(posedge clk) begin
  if (rst) begin
    uram_rd_addr <= {URAM_A_W{1'b0}};
  end else begin
    if (uram_p_state[1])  // URAM Init s
      uram_rd_addr <= {URAM_A_W{1'b0}};
    //else if (uram_p_state[2] || uram_p_state[3] || uram_p_state[4] || read_en_from_ce_state_machine) // Rd1, Rd2, Rd3
    else if (uram_rd_en_tmp || read_en_from_ce_state_machine)
      uram_rd_addr <= uram_rd_addr + 'd16;
  end
end

//uram rd enable
always@(posedge clk) begin
  if (rst) begin
    uram_rd_en_tmp <= 1'b0;
  end else begin
    if (uram_n_state[2] || uram_n_state[3] || uram_n_state[4]) //Rd1, Rd2, Rd3
      uram_rd_en_tmp <= 1'b1;
    else
      uram_rd_en_tmp <= 1'b0;
  end
end

assign uram_rd_en = uram_rd_en_tmp || read_en_from_ce_state_machine; 

//B1 wr addr/wr en
//registering 3 times as I want to register uram_rd_data before giving it to BRAM1
always@(posedge clk) begin
  if (rst) begin
    b1_wr_addr_r1 <= {BRAM_A_W{1'b0}};
  end else begin
    if (uram_rd_en) begin
      if (b1_wr_addr_r1 == 16*(IMG_W-1)) b1_wr_addr_r1 <= {BRAM_A_W{1'b0}};
      else b1_wr_addr_r1 <= b1_wr_addr_r1 + 'd16;
    end
  end
end

always@(posedge clk) begin
  if (rst) begin
    b1_wr_addr_r2 <= {BRAM_A_W{1'b0}}; 
    b1_wr_addr_r3 <= {BRAM_A_W{1'b0}}; 
    b1_wr_en_r1   <= 1'b0;
    b1_wr_en_r2   <= 1'b0;
  end else begin
    b1_wr_addr_r2 <= b1_wr_addr_r1;
    b1_wr_addr_r3 <= b1_wr_addr_r2;
    b1_wr_en_r1   <= uram_rd_en;
    b1_wr_en_r2   <= b1_wr_en_r1;
  end
end

// read enable for BRAM 1 from URAM state machine
always@(posedge clk) begin
  if (rst) begin
    rd_en_from_uram_for_bram1 <= 1'b0;
  end else begin
    if (uram_n_state[2] || uram_n_state[3]) //Rd1, Rd2
      rd_en_from_uram_for_bram1 <= 1'b1;
    else
      rd_en_from_uram_for_bram1 <= 1'b0;
  end
end

// read enable for BRAM 2 from URAM state machine
always@(posedge clk) begin
  if (rst) begin
    rd_en_from_uram_for_bram2 <= 1'b0;
  end else begin
    if (uram_n_state[2]) //Rd1
      rd_en_from_uram_for_bram2 <= 1'b1;
    else
      rd_en_from_uram_for_bram2 <= 1'b0;
  end
end

//delaying BRAM1 read en 4-times to align with B1 read cycle in the initialization stage
always@(posedge clk) begin
  if (rst) begin
    rd_en_bram1_r1 <= 1'b0;
    rd_en_bram1_r2 <= 1'b0;
    rd_en_bram1_r3 <= 1'b0;
  end else begin 
    rd_en_bram1_r1 <= rd_en_from_uram_for_bram1;
    rd_en_bram1_r2 <= rd_en_bram1_r1;
    rd_en_bram1_r3 <= rd_en_bram1_r2;
  end
end

//delaying BRAM2 read en 7-times to align with B2 read cycle in the initialization stage
always@(posedge clk) begin
  if (rst) begin
    rd_en_bram2_r1 <= 1'b0;
    rd_en_bram2_r2 <= 1'b0;
    rd_en_bram2_r3 <= 1'b0;
    rd_en_bram2_r4 <= 1'b0;
    rd_en_bram2_r5 <= 1'b0;
    rd_en_bram2_r6 <= 1'b0;
  end else begin 
    rd_en_bram2_r1 <= rd_en_from_uram_for_bram2;
    rd_en_bram2_r2 <= rd_en_bram2_r1;
    rd_en_bram2_r3 <= rd_en_bram2_r2;
    rd_en_bram2_r4 <= rd_en_bram2_r3;
    rd_en_bram2_r5 <= rd_en_bram2_r4;
    rd_en_bram2_r6 <= rd_en_bram2_r5;
  end
end

always@(posedge clk) begin
  read_en_from_ce_state_machine_r <= read_en_from_ce_state_machine;
  read_en_from_ce_state_machine_2r <= read_en_from_ce_state_machine_r;
  read_en_from_ce_state_machine_3r <= read_en_from_ce_state_machine_2r;
end



//state machine BRAM1 reading
always@(posedge clk) begin
  if (rst) begin
    bram1_p_state <= BRAM1_RST_s;
  end else begin
    bram1_p_state <= bram1_n_state;
  end
end

always@(*) begin
  case (bram1_p_state)
    BRAM1_RST_s      : bram1_n_state <= BRAM1_Init_s;
    BRAM1_Init_s     : begin
                         if (rd_en_bram1_r4) bram1_n_state <= BRAM1_Rd_s;
                         else bram1_n_state <= BRAM1_Init_s;
                       end
    BRAM1_Rd_s       : begin
                         if (!rd_en_bram1_r4) bram1_n_state <= BRAM1_initDone_s;
                         else bram1_n_state <= BRAM1_Rd_s;
                       end
    BRAM1_initDone_s : bram1_n_state <= BRAM1_initDone_s;
    default          : bram1_n_state <= BRAM1_RST_s;
  endcase
end

//bram1 rd addr
always@(posedge clk) begin
  if (rst) begin
    b1_rd_addr <= {BRAM_A_W{1'b0}};
  end else begin
    if (b1_rd_addr == 16*(IMG_W-1))
      b1_rd_addr <= {BRAM_A_W{1'b0}};
    //else if (rd_en_bram1_r4 || read_en_from_ce_state_machine) 
    else if (rd_en_bram1) 
      b1_rd_addr <= b1_rd_addr + 'd16;
  end
end

//state machine BRAM2 reading
always@(posedge clk) begin
  if (rst) begin
    bram2_p_state <= BRAM2_RST_s;
  end else begin
    bram2_p_state <= bram2_n_state;
  end
end

always@(*) begin
  case (bram2_p_state)
    BRAM2_RST_s      : bram2_n_state <= BRAM2_Init_s;
    BRAM2_Init_s     : begin
                         if (rd_en_bram2_r7) bram2_n_state <= BRAM2_Rd_s;
                         else bram2_n_state <= BRAM2_Init_s;
                       end
    BRAM2_Rd_s       : begin
                         if (!rd_en_bram2_r7) bram2_n_state <= BRAM2_initDone_s;
                         else bram2_n_state <= BRAM2_Rd_s;
                       end
    BRAM2_initDone_s : bram2_n_state <= BRAM2_initDone_s;
    default          : bram2_n_state <= BRAM2_RST_s;
  endcase
end

//bram2 rd addr
always@(posedge clk) begin
  if (rst) begin
    b2_rd_addr <= {BRAM_A_W{1'b0}};
  end else begin
    if (b2_rd_addr == 16*(IMG_W-1))
      b2_rd_addr <= {BRAM_A_W{1'b0}};
    //else if (rd_en_bram2_r7 || read_en_from_ce_state_machine) 
    else if (rd_en_bram2) 
      b2_rd_addr <= b2_rd_addr + 'd16;
  end
end


//B2 wr addr/wr en
always@(posedge clk) begin
  if (rst) begin
    b2_wr_addr_r1 <= {BRAM_A_W{1'b0}};
    b2_wr_addr    <= {BRAM_A_W{1'b0}};
    b2_wr_en_r1   <= 1'b0;
    b2_wr_en      <= 1'b0;
  end else begin
    b2_wr_addr_r1 <= b1_rd_addr; 
    b2_wr_addr    <= b2_wr_addr_r1; 
    b2_wr_en_r1   <= rd_en_bram1; 
    b2_wr_en      <= b2_wr_en_r1;
  end
end

//B3 wr addr/wr en
always@(posedge clk) begin
  if (rst) begin
    b3_wr_addr_r1 <= {BRAM_A_W{1'b0}};
    b3_wr_addr    <= {BRAM_A_W{1'b0}};
    b3_wr_en_r1   <= 1'b0;
    b3_wr_en      <= 1'b0;
  end else begin
    b3_wr_addr_r1 <= b2_rd_addr; 
    b3_wr_addr    <= b3_wr_addr_r1; 
    b3_wr_en_r1   <= rd_en_bram2; 
    b3_wr_en      <= b3_wr_en_r1;
  end
end

//bram2 rd addr
always@(posedge clk) begin
  if (rst) begin
    b3_rd_addr <= {BRAM_A_W{1'b0}};
  end else begin
    if (b3_rd_addr == 16*(IMG_W-1))
      b3_rd_addr <= {BRAM_A_W{1'b0}};
    else if (rd_en_bram3) 
      b3_rd_addr <= b3_rd_addr + 'd16;
  end
end


assign init_done = bram1_p_state[3] && bram2_p_state[3];


///////////////////optional register //////////////////////
generate if (NUMBER_OF_REG == 1) begin : wr_en1

  assign rd_en_bram1 = rd_en_bram1_r4 || read_en_from_ce_state_machine;
  assign rd_en_bram2 = rd_en_bram2_r7 || read_en_from_ce_state_machine;
  assign rd_en_bram3 = read_en_from_ce_state_machine;

  always@(posedge clk) begin
    if (rst) begin
      b1_wr_addr    <= {BRAM_A_W{1'b0}}; 
      b1_wr_en      <= 1'b0;
      rd_en_bram1_r4 <= 1'b0;
      rd_en_bram2_r7 <= 1'b0;
    end else begin
      b1_wr_addr    <= b1_wr_addr_r3;
      b1_wr_en      <= b1_wr_en_r2;
      rd_en_bram1_r4 <= rd_en_bram1_r3;
      rd_en_bram2_r7 <= rd_en_bram2_r6;
    end
  end
end endgenerate

generate if (NUMBER_OF_REG == 2) begin : wr_en2
  assign rd_en_bram1 = rd_en_bram1_r4 || read_en_from_ce_state_machine_r;
  assign rd_en_bram2 = rd_en_bram2_r7 || read_en_from_ce_state_machine_r;
  assign rd_en_bram3 = read_en_from_ce_state_machine_r;
  always@(posedge clk) begin
    if (rst) begin
      b1_wr_addr_r4 <= {BRAM_A_W{1'b0}}; 
      b1_wr_addr    <= {BRAM_A_W{1'b0}}; 
      b1_wr_en_r3   <= 1'b0;
      b1_wr_en      <= 1'b0;
      rd_en_bram1_r5 <= 1'b0;
      rd_en_bram1_r4 <= 1'b0;
      rd_en_bram2_r8 <= 1'b0;
      rd_en_bram2_r7 <= 1'b0;
    end else begin
      b1_wr_addr_r4 <= b1_wr_addr_r3; 
      b1_wr_addr    <= b1_wr_addr_r4; 
      b1_wr_en_r3   <= b1_wr_en_r2;
      b1_wr_en      <= b1_wr_en_r3;
      rd_en_bram1_r5 <= rd_en_bram1_r3;
      rd_en_bram1_r4 <= rd_en_bram1_r5;
      rd_en_bram2_r8 <= rd_en_bram2_r6;
      rd_en_bram2_r7 <= rd_en_bram2_r8;
    end
  end
end endgenerate

generate if (NUMBER_OF_REG == 3) begin : wr_en3
  assign rd_en_bram1 = rd_en_bram1_r4 || read_en_from_ce_state_machine_2r;
  assign rd_en_bram2 = rd_en_bram2_r7 || read_en_from_ce_state_machine_2r;
  assign rd_en_bram3 = read_en_from_ce_state_machine_2r;
  always@(posedge clk) begin
    if (rst) begin
      b1_wr_addr_r4 <= {BRAM_A_W{1'b0}}; 
      b1_wr_addr_r5 <= {BRAM_A_W{1'b0}}; 
      b1_wr_addr    <= {BRAM_A_W{1'b0}}; 
      b1_wr_en_r3   <= 1'b0;
      b1_wr_en_r4   <= 1'b0;
      b1_wr_en      <= 1'b0;
      rd_en_bram1_r6 <= 1'b0;
      rd_en_bram1_r5 <= 1'b0;
      rd_en_bram1_r4 <= 1'b0;
      rd_en_bram2_r9 <= 1'b0;
      rd_en_bram2_r8 <= 1'b0;
      rd_en_bram2_r7 <= 1'b0;
    end else begin
      b1_wr_addr_r4 <= b1_wr_addr_r3; 
      b1_wr_addr_r5 <= b1_wr_addr_r4; 
      b1_wr_addr    <= b1_wr_addr_r5; 
      b1_wr_en_r3   <= b1_wr_en_r2;
      b1_wr_en_r4   <= b1_wr_en_r3;
      b1_wr_en      <= b1_wr_en_r4;
      rd_en_bram1_r6 <= rd_en_bram1_r3;
      rd_en_bram1_r5 <= rd_en_bram1_r6;
      rd_en_bram1_r4 <= rd_en_bram1_r5;
      rd_en_bram2_r9 <= rd_en_bram2_r6;
      rd_en_bram2_r8 <= rd_en_bram2_r9;
      rd_en_bram2_r7 <= rd_en_bram2_r8;
    end
  end
end endgenerate


///////////////////////////////////////////////////////////
endmodule

