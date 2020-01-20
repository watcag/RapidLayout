module dsp_conv#(
    parameter A_W=14,
    parameter M_W=18,
    parameter NUMBER_OF_REG=1,
    parameter URAM_D_W=48,
    parameter URAM_A_W=14
)
    (
        input clk,
        input rst,
        input ce,
        input ce_dsp,
        input ce_b_in,
        input [A_W-1:0] b1_wr_addr,
        input b1_wr_en,
        input [A_W-1:0] b2_wr_addr,
        input b2_wr_en,
        input [A_W-1:0] b3_wr_addr,
        input b3_wr_en,
        input [A_W-1:0] b1_rd_addr,
        input [A_W-1:0] b2_rd_addr,
        input [A_W-1:0] b3_rd_addr,
        input [A_W-1:0] rdaddr_b,
        input [15:0] data_in,
        input [A_W-1:0] knl_b_wraddr,
        input [M_W-1:0] knl_b_wrdata,
        input knl_b_wren,

        output reg [15:0] data_out
    );

    reg ce_b;
    reg ce_b_1;
    reg ce_b_2;
    reg ce_b_3;
    reg ce_b_4;
    reg [M_W-1:0] rd_data_b2_r1;
    reg [M_W-1:0] rd_data_b2_r2;
    reg [M_W-1:0] rd_data_b2_r3;
    reg [M_W-1:0] rd_data_b2_r4;
    reg [M_W-1:0] rd_data_b2_r5;
    reg [M_W-1:0] rd_data_b2_r6;
    reg [M_W-1:0] rd_data_b2_r7;
    reg [M_W-1:0] rd_data_b3_r1;
    reg [M_W-1:0] rd_data_b3_r2;
    reg [M_W-1:0] rd_data_b3_r3;
    reg [M_W-1:0] rd_data_b3_r4;
    reg [M_W-1:0] rd_data_b3_r5;
    reg [M_W-1:0] rd_data_b3_r6;
    reg [M_W-1:0] rd_data_b3_r7;
    reg [M_W-1:0] rd_data_b3_r8;
    reg [M_W-1:0] rd_data_b3_r9;
    reg [M_W-1:0] rd_data_b3_r10;
    reg signed [47:0] final_accumulation;
    reg [47:0] p8_reg;
    reg ce_dsp_1;
    reg ce_dsp_2;
    reg [2:0] ce_tmp_r1;
    reg [2:0] ce_tmp_r2;
    reg [2:0] ce_tmp_r3;
    reg [2:0] ce_tmp_r4;
    reg [2:0] ce_tmp_r5;
    reg [2:0] ce_tmp_r6;
    reg [2:0] ce_tmp_r7;
    reg [2:0] ce_a0;
    reg [2:0] ce_a0_r1;
    reg [2:0] ce_a0_r2;
    reg [2:0] ce_a1;
    reg [2:0] ce_a1_r1;
    reg [2:0] ce_a1_r2;
    reg [2:0] ce_a2;
    reg acin0_reg [0:2];
    reg acin01_r1;
    reg acin02_r1;
    reg acin1_reg [0:2];
    reg acin10_r1;
    reg acin11_r1;
    reg acin12_r1;
    reg acin2_reg [0:2];
    reg acin20_r1;
    reg acin21_r1;
    reg acin22_r1;
    reg [7:0] bcin0_reg [0:2];
    reg [7:0] bcin1_reg [0:2];
    reg [7:0] bcin2_reg [0:2];
    reg [M_W-1:0] dsp_a1;
    reg [M_W-1:0] dsp_a2;
    reg signed [47:0] cascade_op;
    reg signed [47:0] separate_dsp_op;
    reg [M_W-1:0] dsp_a0_r;
    reg [M_W-1:0] dsp_k0_r;
    reg [M_W-1:0] dsp_a0_1;
    reg [M_W-1:0] dsp_k0_1;
    reg [M_W-1:0] dsp_a0_2;
    reg [M_W-1:0] dsp_k0_2;
    reg [M_W-1:0] dsp_a0_3;
    reg [M_W-1:0] dsp_k0_3;
    reg [M_W-1:0] dsp_a0_4;
    reg [M_W-1:0] dsp_k0_4;
    reg [47:0] new_acc_fab;
    reg [47:0] new_acc1;

    wire [M_W-1:0] casc_data_b1;
    wire [M_W-1:0] casc_data_b2;
    wire [M_W-1:0] casc_data_b3;
    wire [M_W-1:0] rd_data_b2;
    wire [M_W-1:0] rd_data_b3;
    wire [47:0] p[0:9];
    wire [47:0] p_out[0:1];
    wire [29:0] acin0[0:3];
    wire [29:0] acin1[0:3];
    wire [29:0] acin2[0:3];
    wire [17:0] bcin0[0:8];
    wire [M_W-1:0] dsp_a0;
    wire [M_W-1:0] dsp_k0;
    wire [47:0] new_acc;
    wire [2:0] ce_tmp;
//////////////////////// optional register /////////////////
    generate if (NUMBER_OF_REG == 0) begin : a0k0_0
        always @(posedge clk) begin
            dsp_a0_r <= dsp_a0;
            dsp_k0_r <= dsp_k0;
            ce_b <= ce_b_in;
            dsp_a1 <= rd_data_b2_r3;
            dsp_a2 <= rd_data_b3_r6;
            ce_tmp_r1 <= ce_tmp;
        end
    end endgenerate

    generate if (NUMBER_OF_REG == 1) begin : a0k0_1
        always @(posedge clk) begin
            dsp_a0_1 <= dsp_a0;
            dsp_k0_1 <= dsp_k0;
            dsp_a0_r <= dsp_a0_1;
            dsp_k0_r <= dsp_k0_1;
            ce_b_1 <= ce_b_in;
            ce_b <= ce_b_1;
            rd_data_b2_r4 <= rd_data_b2_r3;
            dsp_a1 <= rd_data_b2_r4;
            rd_data_b3_r7 <= rd_data_b3_r6;
            dsp_a2 <= rd_data_b3_r7;
            ce_tmp_r3 <= ce_tmp;
            ce_tmp_r2 <= ce_tmp_r3;
            ce_tmp_r1 <= ce_tmp_r2;
        end
    end endgenerate

    generate if (NUMBER_OF_REG == 2) begin : a0k0_2
        always @(posedge clk) begin
            dsp_a0_1 <= dsp_a0;
            dsp_k0_1 <= dsp_k0;
            dsp_a0_2 <= dsp_a0_1;
            dsp_k0_2 <= dsp_k0_1;
            dsp_a0_r <= dsp_a0_2;
            dsp_k0_r <= dsp_k0_2;
            ce_b_1 <= ce_b_in;
            ce_b_2 <= ce_b_1;
            ce_b <= ce_b_2;
            rd_data_b2_r4 <= rd_data_b2_r3;
            rd_data_b2_r5 <= rd_data_b2_r4;
            dsp_a1 <= rd_data_b2_r5;
            rd_data_b3_r7 <= rd_data_b3_r6;
            rd_data_b3_r8 <= rd_data_b3_r7;
            dsp_a2 <= rd_data_b3_r8;
            ce_tmp_r5 <= ce_tmp;
            ce_tmp_r4 <= ce_tmp_r5;
            ce_tmp_r3 <= ce_tmp_r4;
            ce_tmp_r2 <= ce_tmp_r3;
            ce_tmp_r1 <= ce_tmp_r2;
        end
    end endgenerate

    generate if (NUMBER_OF_REG == 3) begin : a0k0_3
        always @(posedge clk) begin
            dsp_a0_1 <= dsp_a0;
            dsp_k0_1 <= dsp_k0;
            dsp_a0_2 <= dsp_a0_1;
            dsp_k0_2 <= dsp_k0_1;
            dsp_a0_3 <= dsp_a0_2;
            dsp_k0_3 <= dsp_k0_2;
            dsp_a0_r <= dsp_a0_3;
            dsp_k0_r <= dsp_k0_3;
            ce_b_1 <= ce_b_in;
            ce_b_2 <= ce_b_1;
            ce_b_3 <= ce_b_2;
            ce_b <= ce_b_3;
            rd_data_b2_r4 <= rd_data_b2_r3;
            rd_data_b2_r5 <= rd_data_b2_r4;
            rd_data_b2_r6 <= rd_data_b2_r5;
            dsp_a1 <= rd_data_b2_r6;
            rd_data_b3_r7 <= rd_data_b3_r6;
            rd_data_b3_r8 <= rd_data_b3_r7;
            rd_data_b3_r9 <= rd_data_b3_r8;
            dsp_a2 <= rd_data_b3_r9;
            ce_tmp_r6 <= ce_tmp;
            ce_tmp_r5 <= ce_tmp_r6;
            ce_tmp_r4 <= ce_tmp_r5;
            ce_tmp_r3 <= ce_tmp_r4;
            ce_tmp_r2 <= ce_tmp_r3;
            ce_tmp_r1 <= ce_tmp_r2;
        end
    end endgenerate

    generate if (NUMBER_OF_REG == 4) begin : a0k0_3
        always @(posedge clk) begin
            dsp_a0_1 <= dsp_a0;
            dsp_k0_1 <= dsp_k0;
            dsp_a0_2 <= dsp_a0_1;
            dsp_k0_2 <= dsp_k0_1;
            dsp_a0_3 <= dsp_a0_2;
            dsp_k0_3 <= dsp_k0_2;
            dsp_a0_4 <= dsp_a0_3;
            dsp_k0_4 <= dsp_k0_3;
            dsp_a0_r <= dsp_a0_4;
            dsp_k0_r <= dsp_k0_4;
            ce_b_1 <= ce_b_in;
            ce_b_2 <= ce_b_1;
            ce_b_3 <= ce_b_2;
            ce_b_4 <= ce_b_3;
            ce_b <= ce_b_4;
            rd_data_b2_r4 <= rd_data_b2_r3;
            rd_data_b2_r5 <= rd_data_b2_r4;
            rd_data_b2_r6 <= rd_data_b2_r5;
            rd_data_b2_r7 <= rd_data_b2_r6;
            dsp_a1 <= rd_data_b2_r7;
            rd_data_b3_r7 <= rd_data_b3_r6;
            rd_data_b3_r8 <= rd_data_b3_r7;
            rd_data_b3_r9 <= rd_data_b3_r8;
            rd_data_b3_r10 <= rd_data_b3_r9;
            dsp_a2 <= rd_data_b3_r10;
            ce_tmp_r8 <= ce_tmp;
            ce_tmp_r7 <= ce_tmp_r8;
            ce_tmp_r5 <= ce_tmp_r6;
            ce_tmp_r4 <= ce_tmp_r5;
            ce_tmp_r3 <= ce_tmp_r4;
            ce_tmp_r2 <= ce_tmp_r3;
            ce_tmp_r1 <= ce_tmp_r2;
        end
    end endgenerate


//////////////////////////////////////////////////////////
    always @(posedge clk) begin
    end


    RAMB18E2#(
        .DOA_REG(1), .DOB_REG(1),
        .CASCADE_ORDER_A("FIRST"), .CASCADE_ORDER_B("NONE"),
        .CLOCK_DOMAINS("COMMON"),
        .WRITE_MODE_A("WRITE_FIRST"), .WRITE_MODE_B("WRITE_FIRST"),

        .WRITE_WIDTH_A(18), .WRITE_WIDTH_B(18),
        .READ_WIDTH_A(18), .READ_WIDTH_B(18))
    bram_inst_rdc1(
        .ADDRARDADDR(b1_rd_addr),
        .ADDRBWRADDR(b1_wr_addr),
        .ADDRENA(1'b1),
        .ADDRENB(1'b1),
        .WEA({2{1'b0}}),
        .WEBWE({4{b1_wr_en}}),

        // horizontal links
        .CASDOUTA(casc_data_b1[15:0]),
        .CASDOUTPA(casc_data_b1[17:16]),
        .DINBDIN(data_in[15:0]),
        .DINPBDINP(2'b00),
        .CASDIMUXA(1'b0),
        .CASDIMUXB(1'b0),
        .DOUTADOUT(dsp_a0[15:0]),
        .DOUTPADOUTP(dsp_a0[17:16]),

        // clocking, reset, and enable control
        .CLKARDCLK(clk),
        .CLKBWRCLK(clk),

        .ENARDEN(ce),
        .ENBWREN(ce),
        .REGCEAREGCE(ce),
        .REGCEB(ce),

        .RSTRAMARSTRAM(rst),
        .RSTRAMB(rst),
        .RSTREGARSTREG(rst),
        .RSTREGB(rst)
    );


    RAMB18E2#(
        .DOA_REG(1), .DOB_REG(1),
        .CASCADE_ORDER_A("LAST"), .CASCADE_ORDER_B("FIRST"),
        .CLOCK_DOMAINS("COMMON"),

        .WRITE_MODE_A("WRITE_FIRST"), .WRITE_MODE_B("WRITE_FIRST"),
        .WRITE_WIDTH_A(18), .WRITE_WIDTH_B(18),
        .READ_WIDTH_A(18), .READ_WIDTH_B(18))
    bram_inst_rdc2(
        .ADDRARDADDR(b2_wr_addr),
        .ADDRBWRADDR(b2_rd_addr),
        .ADDRENA(1'b1),
        .ADDRENB(1'b1),
        .WEA({2{b2_wr_en}}),
        .WEBWE({4{1'b0}}),

        // horizontal links
        .CASDOUTB(casc_data_b2[15:0]),
        .CASDOUTPB(casc_data_b2[17:16]),
        .CASDINA(casc_data_b1[15:0]),
        .CASDINPA(casc_data_b1[17:16]),
        .CASDIMUXB(1'b0),
        .CASDIMUXA(1'b1),
        .DOUTBDOUT(rd_data_b2[15:0]),
        .DOUTPBDOUTP(rd_data_b2[17:16]),

        // clocking, reset, and enable control
        .CLKARDCLK(clk),
        .CLKBWRCLK(clk),

        .ENARDEN(ce),
        .ENBWREN(ce),
        .REGCEAREGCE(ce),
        .REGCEB(ce),

        .RSTRAMARSTRAM(rst),
        .RSTRAMB(rst),
        .RSTREGARSTREG(rst),
        .RSTREGB(rst)
    );

    RAMB18E2#(
        .DOA_REG(1), .DOB_REG(1),
        .CASCADE_ORDER_A("NONE"), .CASCADE_ORDER_B("LAST"),
        .CLOCK_DOMAINS("COMMON"),

        .WRITE_MODE_A("WRITE_FIRST"), .WRITE_MODE_B("WRITE_FIRST"),
        .WRITE_WIDTH_A(18), .WRITE_WIDTH_B(18),
        .READ_WIDTH_A(18), .READ_WIDTH_B(18))
    bram_inst_rdc3(
        .ADDRARDADDR(b3_rd_addr),
        .ADDRBWRADDR(b3_wr_addr),
        .ADDRENA(1'b1),
        .ADDRENB(1'b1),
        .WEA({2{1'b0}}),
        .WEBWE({4{b3_wr_en}}),

        // horizontal links
        .DOUTADOUT(rd_data_b3[15:0]),
        .DOUTPADOUTP(rd_data_b3[17:16]),
        .CASDINB(casc_data_b2[15:0]),
        .CASDINPB(casc_data_b2[17:16]),
        .DOUTBDOUT(),
        .DOUTPBDOUTP(),
        .CASDIMUXB(1'b1),
        .CASDIMUXA(1'b0),

        // clocking, reset, and enable control
        .CLKARDCLK(clk),
        .CLKBWRCLK(clk),

        .ENARDEN(ce),
        .ENBWREN(ce),
        .REGCEAREGCE(ce),
        .REGCEB(ce),

        .RSTRAMARSTRAM(rst),
        .RSTRAMB(rst),
        .RSTREGARSTREG(rst),
        .RSTREGB(rst)
    );

///////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////


    always @(posedge clk) begin
        if (rst) acin0_reg[0] <= 'b0;
        else if (ce_a0[0]) acin0_reg[0] <= acin0[0] [26];

        if (rst) begin
            acin01_r1 <= 'b0;
            acin0_reg[1] <= 'b0;
        end else if (ce_a0[1]) begin
            acin01_r1 <= acin0_reg[0];
            acin0_reg[1] <= acin01_r1;
        end

        if (rst) begin
            acin02_r1 <= 'b0;
            acin0_reg[2] <= 'b0;
        end else if (ce_a0[2]) begin
            acin02_r1 <= acin0_reg[1];
            acin0_reg[2] <= acin02_r1;
        end
////
        if (rst) acin1_reg[0] <= 'b0;
        else if (ce_a1[0]) acin1_reg[0] <= acin1[0] [26];

        if (rst) begin
            acin11_r1 <= 'b0;
            acin1_reg[1] <= 'b0;
        end else if (ce_a1[1]) begin
            acin11_r1 <= acin1_reg[0];
            acin1_reg[1] <= acin11_r1;
        end

        if (rst) begin
            acin12_r1 <= 'b0;
            acin1_reg[2] <= 'b0;
        end else if (ce_a1[2]) begin
            acin12_r1 <= acin1_reg[1];
            acin1_reg[2] <= acin12_r1;
        end
////
        if (rst) acin2_reg[0] <= 'b0;
        else if (ce_a2[0]) acin2_reg[0] <= acin2[0] [26];

        if (rst) begin
            acin21_r1 <= 'b0;
            acin2_reg[1] <= 'b0;
        end else if (ce_a2[1]) begin
            acin21_r1 <= acin2_reg[0];
            acin2_reg[1] <= acin21_r1;
        end

        if (rst) begin
            acin22_r1 <= 'b0;
            acin2_reg[2] <= 'b0;
        end else if (ce_a2[2]) begin
            acin22_r1 <= acin2_reg[1];
            acin2_reg[2] <= acin22_r1;
        end
    end

    always @(posedge clk) begin
        if (rst) begin
            bcin0_reg[0] <= 'b0;
            bcin0_reg[1] <= 'b0;
            bcin0_reg[2] <= 'b0;
            bcin1_reg[0] <= 'b0;
            bcin1_reg[1] <= 'b0;
            bcin1_reg[2] <= 'b0;
            bcin2_reg[0] <= 'b0;
            bcin2_reg[1] <= 'b0;
            bcin2_reg[2] <= 'b0;
        end else if (ce_b) begin
            bcin0_reg[0] <= bcin0[0][7:0];
            bcin0_reg[1] <= bcin0_reg[0];
            bcin0_reg[2] <= bcin0_reg[1];
            bcin1_reg[0] <= bcin0_reg[2];
            bcin1_reg[1] <= bcin1_reg[0];
            bcin1_reg[2] <= bcin1_reg[1];
            bcin2_reg[0] <= bcin1_reg[2];
            bcin2_reg[1] <= bcin2_reg[0];
            bcin2_reg[2] <= bcin2_reg[1];
        end
    end

    always @(posedge clk) begin
        ce_dsp_1 <= ce_dsp;
        ce_dsp_2 <= ce_dsp_1;
    end

    assign ce_tmp = {ce_dsp_2, ce_dsp_1, ce_dsp};

//registering ce_a for 11 clock cycle
    always @(posedge clk) begin
        ce_a0 <= ce_tmp_r1;

        ce_a0_r1 <= ce_a0;
        ce_a0_r2 <= ce_a0_r1;
        ce_a1 <= ce_a0_r2;

        ce_a1_r1 <= ce_a1;
        ce_a1_r2 <= ce_a1_r1;
        ce_a2 <= ce_a1_r2;
    end

    assign acin0[0] = {{3{1'b0}}, dsp_a0_r[15:8], {11{1'b0}}, dsp_a0_r[7:0]};
    assign bcin0[0] = {{10{dsp_k0_r[7]}}, dsp_k0_r[7:0]};
    assign acin1[0] = {{3{1'b0}}, dsp_a1[15:8], {11{1'b0}}, dsp_a1[7:0]};
    assign acin2[0] = {{3{1'b0}}, dsp_a2[15:8], {11{1'b0}}, dsp_a2[7:0]};

    always @(posedge clk) begin
        rd_data_b2_r1 <= rd_data_b2;
        rd_data_b2_r2 <= rd_data_b2_r1;
        rd_data_b2_r3 <= rd_data_b2_r2;
        rd_data_b3_r1 <= rd_data_b3;
        rd_data_b3_r2 <= rd_data_b3_r1;
        rd_data_b3_r3 <= rd_data_b3_r2;
        rd_data_b3_r4 <= rd_data_b3_r3;
        rd_data_b3_r5 <= rd_data_b3_r4;
        rd_data_b3_r6 <= rd_data_b3_r5;
    end

//////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////// DSP 0 ///////////////////////////////////////

//kernel ram

    RAMB18E2#(
        .DOA_REG(1), .DOB_REG(1),
        .CASCADE_ORDER_A("NONE"), .CASCADE_ORDER_B("NONE"),
        .CLOCK_DOMAINS("COMMON"),

        //.INIT_FILE("k_pixels_3.hex"),
        .WRITE_WIDTH_A(18), .WRITE_WIDTH_B(18),
        .READ_WIDTH_A(18), .READ_WIDTH_B(18))
    bram_inst_rdc4(
        .ADDRARDADDR(rdaddr_b),
        .ADDRBWRADDR(knl_b_wraddr),
        .ADDRENA(1'b1),
        .ADDRENB(1'b1),
        .WEA({2{1'b0}}),
        .WEBWE({4{knl_b_wren}}),

        // horizontal links
        .DOUTADOUT(dsp_k0[15:0]),
        .DOUTPADOUTP(dsp_k0[17:16]),
        .DINBDIN(knl_b_wrdata[15:0]),
        .DINPBDINP(knl_b_wrdata[17:16]),

        // clocking, reset, and enable control
        .CLKARDCLK(clk),
        .CLKBWRCLK(clk),

        .ENARDEN(ce),
        .ENBWREN(ce),
        .REGCEAREGCE(ce),
        .REGCEB(ce),

        .RSTRAMARSTRAM(rst),
        .RSTRAMB(rst),
        .RSTREGARSTREG(rst),
        .RSTREGB(rst)
    );

    genvar i;
    generate
        for (i = 0; i < 3; i = i+1) begin : dsp_chain0
            if (i == 0) begin
                DSP48E2#(
                    .AMULTSEL("A"), .BMULTSEL("B"),
                    .A_INPUT("DIRECT"), .B_INPUT("DIRECT"),
                    .AREG(1), .ACASCREG(1))
                dsp_inst(
                    .A(acin0[i]),
                    .B(bcin0[i]),
                    .P(),
                    .C({{13{bcin0_reg[i] [7]}}, bcin0_reg[i][7:0], {27{1'b0}}}),
                    .PCIN(),
                    .PCOUT(p[i+1]),
                    //.ACIN(acin0[i]),
                    .ACOUT(acin0[i+1]),
                    //.BCIN(bcin0[i]),
                    .BCOUT(bcin0[i+1]),

                    // control DSP
                    .ALUMODE(4'b0000),
                    .INMODE(5'b00100),
                    //.OPMODE({acin0_reg[i][26],acin0_reg[i][26],7'b0010101}),
                    .OPMODE({acin0_reg[i], acin0_reg[i], 7'b0000101}),

                    // clocking reset and enables.. control signals
                    .CLK(clk),
                    .RSTA(rst),
                    .RSTALLCARRYIN(rst),
                    .RSTALUMODE(rst),
                    .RSTB(rst),
                    .RSTC(rst),
                    .RSTCTRL(rst),
                    .RSTD(rst),
                    .RSTINMODE(rst),
                    .RSTM(rst),
                    .RSTP(rst),
                    .CEA1(ce_a0[i]),
                    .CEA2(ce_a0[i]),
                    .CEAD(ce),
                    .CEALUMODE(ce),
                    .CEB1(ce_b),
                    .CEB2(ce_b),
                    .CEC(ce),
                    .CECARRYIN(ce),
                    .CECTRL(ce),
                    .CED(ce),
                    .CEINMODE(ce),
                    .CEM(ce),
                    .CEP(ce)
                );
            end else begin
                DSP48E2#(
                    .AMULTSEL("A"), .BMULTSEL("B"),
                    .A_INPUT("CASCADE"), .B_INPUT("CASCADE"),
                    .AREG(2), .ACASCREG(2))
                dsp_inst(
                    //.A({{3{1'b0}},a[i],{11{1'b0}},d[i]}),
                    //.B({{10{k[i][7]}},k[i]}),
                    .P(),
                    .C({{13{bcin0_reg[i] [7]}}, bcin0_reg[i][7:0], {27{1'b0}}}),
                    .PCIN(p[i]),
                    .PCOUT(p[i+1]),
                    .ACIN(acin0[i]),
                    .ACOUT(acin0[i+1]),
                    .BCIN(bcin0[i]),
                    .BCOUT(bcin0[i+1]),

                    // control DSP
                    .ALUMODE(4'b0000),
                    .INMODE(5'b00100),
                    //.OPMODE({acin0_reg[i][26],acin0_reg[i][26],7'b0010101}),
                    .OPMODE({acin0_reg[i], acin0_reg[i], 7'b0010101}),

                    // clocking reset and enables.. control signals
                    .CLK(clk),
                    .RSTA(rst),
                    .RSTALLCARRYIN(rst),
                    .RSTALUMODE(rst),
                    .RSTB(rst),
                    .RSTC(rst),
                    .RSTCTRL(rst),
                    .RSTD(rst),
                    .RSTINMODE(rst),
                    .RSTM(rst),
                    .RSTP(rst),
                    .CEA1(ce_a0[i]),
                    .CEA2(ce_a0[i]),
                    .CEAD(ce),
                    .CEALUMODE(ce),
                    .CEB1(ce_b),
                    .CEB2(ce_b),
                    .CEC(ce),
                    .CECARRYIN(ce),
                    .CECTRL(ce),
                    .CED(ce),
                    .CEINMODE(ce),
                    .CEM(ce),
                    .CEP(ce)
                );
            end
        end
    endgenerate


//////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////// DSP 1 ///////////////////////////////////////

    genvar j;
    generate
        for (j = 0; j < 3; j = j+1) begin : dsp_chain1
            if (j == 0) begin
                DSP48E2#(
                    .AMULTSEL("A"), .BMULTSEL("B"),
                    .A_INPUT("DIRECT"), .B_INPUT("CASCADE"),
                    .AREG(1), .ACASCREG(1))
                dsp_inst(
                    .A(acin1[j]),
                    //.B(bcin1[j]),
                    .P(),
                    .C({{13{bcin1_reg[j] [7]}}, bcin1_reg[j][7:0], {27{1'b0}}}),
                    .PCIN(p[j+3]),
                    .PCOUT(p[j+4]),
                    //.ACIN(acin1[j]),
                    .ACOUT(acin1[j+1]),
                    .BCIN(bcin0[j+3]),
                    .BCOUT(bcin0[j+4]),

                    // control DSP
                    .ALUMODE(4'b0000),
                    .INMODE(5'b00100),
                    //.OPMODE({acin1_reg[j][26],acin1_reg[j][26],7'b0010101}),
                    .OPMODE({acin1_reg[j], acin1_reg[j], 7'b0010101}),

                    // clocking reset and enables.. control signals
                    .CLK(clk),
                    .RSTA(rst),
                    .RSTALLCARRYIN(rst),
                    .RSTALUMODE(rst),
                    .RSTB(rst),
                    .RSTC(rst),
                    .RSTCTRL(rst),
                    .RSTD(rst),
                    .RSTINMODE(rst),
                    .RSTM(rst),
                    .RSTP(rst),
                    .CEA1(ce_a1[j]),
                    .CEA2(ce_a1[j]),
                    .CEAD(ce),
                    .CEALUMODE(ce),
                    .CEB1(ce_b),
                    .CEB2(ce_b),
                    .CEC(ce),
                    .CECARRYIN(ce),
                    .CECTRL(ce),
                    .CED(ce),
                    .CEINMODE(ce),
                    .CEM(ce),
                    .CEP(ce)
                );
            end else begin
                DSP48E2#(
                    .AMULTSEL("A"), .BMULTSEL("B"),
                    .A_INPUT("CASCADE"), .B_INPUT("CASCADE"),
                    .AREG(2), .ACASCREG(2))
                dsp_inst(
                    //.A({{3{1'b0}},a[i],{11{1'b0}},d[i]}),
                    //.B({{10{k[i][7]}},k[i]}),
                    .P(),
                    .C({{13{bcin1_reg[j] [7]}}, bcin1_reg[j][7:0], {27{1'b0}}}),
                    .PCIN(p[j+3]),
                    .PCOUT(p[j+4]),
                    .ACIN(acin1[j]),
                    .ACOUT(acin1[j+1]),
                    .BCIN(bcin0[j+3]),
                    .BCOUT(bcin0[j+4]),

                    // control DSP
                    .ALUMODE(4'b0000),
                    .INMODE(5'b00100),
                    .OPMODE({acin1_reg[j], acin1_reg[j], 7'b0010101}),

                    // clocking reset and enables.. control signals
                    .CLK(clk),
                    .RSTA(rst),
                    .RSTALLCARRYIN(rst),
                    .RSTALUMODE(rst),
                    .RSTB(rst),
                    .RSTC(rst),
                    .RSTCTRL(rst),
                    .RSTD(rst),
                    .RSTINMODE(rst),
                    .RSTM(rst),
                    .RSTP(rst),
                    .CEA1(ce_a1[j]),
                    .CEA2(ce_a1[j]),
                    .CEAD(ce),
                    .CEALUMODE(ce),
                    .CEB1(ce_b),
                    .CEB2(ce_b),
                    .CEC(ce),
                    .CECARRYIN(ce),
                    .CECTRL(ce),
                    .CED(ce),
                    .CEINMODE(ce),
                    .CEM(ce),
                    .CEP(ce)
                );
            end
        end
    endgenerate


//////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////// DSP 2 ///////////////////////////////////////

    genvar k;
    generate
        for (k = 0; k < 2; k = k+1) begin : dsp_chain2
            if (k == 0) begin
                DSP48E2#(
                    .AMULTSEL("A"), .BMULTSEL("B"),
                    .A_INPUT("DIRECT"), .B_INPUT("CASCADE"),
                    .AREG(1), .ACASCREG(1))
                dsp_inst(
                    .A(acin2[k]),
                    //.B(bcin2[k]),
                    .P(p_out[k]),
                    .C({{13{bcin2_reg[k] [7]}}, bcin2_reg[k][7:0], {27{1'b0}}}),
                    .PCIN(p[k+6]),
                    .PCOUT(p[k+7]),
                    //.ACIN(acin2[k]),
                    .ACOUT(acin2[k+1]),
                    .BCIN(bcin0[k+6]),
                    .BCOUT(bcin0[k+7]),

                    // control DSP
                    .ALUMODE(4'b0000),
                    .INMODE(5'b00100),
                    //.OPMODE({acin2_reg[k][26],acin2_reg[k][26],7'b0010101}),
                    .OPMODE({acin2_reg[k], acin2_reg[k], 7'b0010101}),

                    // clocking reset and enables.. control signals
                    .CLK(clk),
                    .RSTA(rst),
                    .RSTALLCARRYIN(rst),
                    .RSTALUMODE(rst),
                    .RSTB(rst),
                    .RSTC(rst),
                    .RSTCTRL(rst),
                    .RSTD(rst),
                    .RSTINMODE(rst),
                    .RSTM(rst),
                    .RSTP(rst),
                    .CEA1(ce_a2[k]),
                    .CEA2(ce_a2[k]),
                    .CEAD(ce),
                    .CEALUMODE(ce),
                    .CEB1(ce_b),
                    .CEB2(ce_b),
                    .CEC(ce),
                    .CECARRYIN(ce),
                    .CECTRL(ce),
                    .CED(ce),
                    .CEINMODE(ce),
                    .CEM(ce),
                    .CEP(ce)
                );
            end else begin
                DSP48E2#(
                    .AMULTSEL("A"), .BMULTSEL("B"),
                    .A_INPUT("CASCADE"), .B_INPUT("CASCADE"),
                    .AREG(2), .ACASCREG(2))
                dsp_inst(
                    //.A({{3{1'b0}},a[i],{11{1'b0}},d[i]}),
                    //.B({{10{k[i][7]}},k[i]}),
                    .P(p_out[k]),
                    .C({{13{bcin2_reg[k] [7]}}, bcin2_reg[k][7:0], {27{1'b0}}}),
                    .PCIN(p[k+6]),
                    .PCOUT(p[k+7]),
                    .ACIN(acin2[k]),
                    .ACOUT(acin2[k+1]),
                    .BCIN(bcin0[k+6]),
                    .BCOUT(bcin0[k+7]),

                    // control DSP
                    .ALUMODE(4'b0000),
                    .INMODE(5'b00100),
                    .OPMODE({acin2_reg[k], acin2_reg[k], 7'b0010101}),

                    // clocking reset and enables.. control signals
                    .CLK(clk),
                    .RSTA(rst),
                    .RSTALLCARRYIN(rst),
                    .RSTALUMODE(rst),
                    .RSTB(rst),
                    .RSTC(rst),
                    .RSTCTRL(rst),
                    .RSTD(rst),
                    .RSTINMODE(rst),
                    .RSTM(rst),
                    .RSTP(rst),
                    .CEA1(ce_a2[k]),
                    .CEA2(ce_a2[k]),
                    .CEAD(ce),
                    .CEALUMODE(ce),
                    .CEB1(ce_b),
                    .CEB2(ce_b),
                    .CEC(ce),
                    .CECARRYIN(ce),
                    .CECTRL(ce),
                    .CED(ce),
                    .CEINMODE(ce),
                    .CEM(ce),
                    .CEP(ce)
                );

            end
        end
    endgenerate

    DSP48E2#(
        .AMULTSEL("A"), .BMULTSEL("B"),
        .A_INPUT("CASCADE"), .B_INPUT("CASCADE"),
        .AREG(2), .ACASCREG(2))
    dsp_inst8(
        //.A({{3{1'b0}},a[i],{11{1'b0}},d[i]}),
        //.B({{10{k[i][7]}},k[i]}),
        .P(p[9]),
        .C({{13{bcin2_reg[2] [7]}}, bcin2_reg[2][7:0], {27{1'b0}}}),
        .PCIN(48'd0),
        .PCOUT(),
        .ACIN(acin2[2]),
        .ACOUT(acin2[3]),
        .BCIN(bcin0[8]),
        //.BCOUT(bcin2[3]),

        // control DSP
        .ALUMODE(4'b0000),
        .INMODE(5'b00100),
        .OPMODE({acin2_reg[2], acin2_reg[2], 7'b0000101}),

        // clocking reset and enables.. control signals
        .CLK(clk),
        .RSTA(rst),
        .RSTALLCARRYIN(rst),
        .RSTALUMODE(rst),
        .RSTB(rst),
        .RSTC(rst),
        .RSTCTRL(rst),
        .RSTD(rst),
        .RSTINMODE(rst),
        .RSTM(rst),
        .RSTP(rst),
        .CEA1(ce_a2[2]),
        .CEA2(ce_a2[2]),
        .CEAD(ce),
        .CEALUMODE(ce),
        .CEB1(ce_b),
        .CEB2(ce_b),
        .CEC(ce),
        .CECARRYIN(ce),
        .CECTRL(ce),
        .CED(ce),
        .CEINMODE(ce),
        .CEM(ce),
        .CEP(ce)
    );

    always @(posedge clk) begin
        p8_reg <= p_out[1];
    end

    always @(posedge clk) begin
        cascade_op <= {{5{p8_reg[37]}}, p8_reg[37:19], {5{p8_reg[18]}}, p8_reg[18:0]};
        separate_dsp_op <= {{5{p[9] [37]}}, p[9][37:19], {5{p[9] [18]}}, p[9][18:0]};
        new_acc_fab <= cascade_op+separate_dsp_op;
        new_acc1 <= new_acc_fab+{23'd0, new_acc_fab[23], 6'd0};
        data_out <= {new_acc1[47:40], new_acc1[23:16]}; //truncating output to 8b
    end
endmodule: dsp_conv
