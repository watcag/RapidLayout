open_checkpoint /home/dell/RapidLayout/checkpoint/blockNum=1_placed.dcp
create_clock -period 1.000 -waveform {0.000 0.500} [get_nets clk];
startgroup
create_pblock {pblock_name.dut}
resize_pblock {pblock_name.dut} -add CLOCKREGION_X0Y0:CLOCKREGION_X1Y0
add_cells_to_pblock {pblock_name.dut} -top
endgroup
set_property CONTAIN_ROUTING true [get_pblocks pblock_name.dut]
place_design; route_design; report_timing
write_checkpoint -force -file /home/dell/RapidLayout/checkpoint/blockNum=1_routed.dcp
write_edf -force -file /home/dell/RapidLayout/checkpoint/blockNum=1_routed.edf
exit
