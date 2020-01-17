read_verilog /home/niansong/RapidWright/src/verilog/addr_gen.v /home/niansong/RapidWright/src/verilog/dsp_conv.v /home/niansong/RapidWright/src/verilog/dsp_conv_top.v /home/niansong/RapidWright/src/verilog/dsp_conv_chip.sv
set_property generic Y=80 [current_fileset]
synth_design -mode out_of_context -part xcvu11p-fsgd2104-3-e -top dsp_conv_chip;
create_clock -period 1.000 -waveform {0.000 0.500} [get_nets clk];
place_design; route_design; 

