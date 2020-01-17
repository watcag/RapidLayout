read_verilog /home/niansong/RapidWright/src/verilog/addr_gen.v /home/niansong/RapidWright/src/verilog/dsp_conv.v /home/niansong/RapidWright/src/verilog/dsp_conv_top.v /home/niansong/RapidWright/src/verilog/dsp_conv_chip.sv
set_property generic Y=480
synth_design -mode out_of_context -part xcvu11p-fsgd2104-3-e -top dsp_conv_chip;
read_xdc /home/niansong/RapidWright/src/verilog/dsp_conv_chip.xdc
opt_design;
place_design;
route_design;
exit
