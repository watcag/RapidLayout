open_checkpoint /home/niansong/RapidWright/checkpoint/blockNum=80_placed.dcp
create_clock -period 1.000 -waveform {0.000 0.500} [get_nets clk];
place_design; route_design; report_timing
write_checkpoint -force -file /home/niansong/RapidWright/checkpoint/blockNum=80_routed.dcp
exit
