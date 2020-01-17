read_verilog ../verilog/addr_gen.v ../verilog/dsp_conv.v ../verilog/dsp_conv_top.v ../verilog/dsp_conv_chip.sv
set_property generic Y=80 [current_fileset]
synth_design -mode out_of_context -part xcvu11p-fsgd2104-3-e -top dsp_conv_chip;
write_checkpoint -force -file /Users/zhangniansong/RapidWright/checkpoint/blockNum=80.dcp
exit
