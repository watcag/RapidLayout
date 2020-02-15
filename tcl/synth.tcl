read_verilog ../verilog/addr_gen.v ../verilog/dsp_conv.v ../verilog/dsp_conv_top.v ../verilog/dsp_conv_chip.sv
set_property generic {NUMBER_OF_REG=0 Y=160} [current_fileset]
synth_design -mode out_of_context -part xcvu11p-fsgd2104-3-e -top dsp_conv_chip;
write_checkpoint -force -file /Users/zhangniansong/RapidWright/checkpoint/blockNum=160.dcp
write_edif -force -file /Users/zhangniansong/RapidWright/checkpoint/blockNum=160.edf
exit
