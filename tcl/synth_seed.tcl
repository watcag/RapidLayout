read_verilog ../src/verilog/addr_gen.v ../src/verilog/dsp_conv.v ../src/verilog/dsp_conv_top.v ../src/verilog/dsp_conv_chip.sv
set_property generic Y=1 [current_fileset]
synth_design -mode out_of_context -part xcvu11p-fsgd2104-3-e -top dsp_conv_chip;
write_checkpoint -force -file /Users/zhangniansong/RapidWright/checkpoint/seed.dcp
exit
