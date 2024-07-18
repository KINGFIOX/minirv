// Generated by CIRCT firtool-1.62.0
module Bridge(
  input         clock,
                reset,
  input  [31:0] io_cpu_addr,
  output [31:0] io_cpu_rdata,
  input  [3:0]  io_cpu_wen,
  input  [31:0] io_cpu_wdata,
  output [31:0] io_dev_0_addr,
  output [3:0]  io_dev_0_wen,
  output [31:0] io_dev_0_wdata,
  input  [31:0] io_dev_0_rdata,
  output [31:0] io_dev_1_addr,
  output [3:0]  io_dev_1_wen,
  output [31:0] io_dev_1_wdata,
  input  [31:0] io_dev_1_rdata,
  output [31:0] io_dev_2_addr,
  output [3:0]  io_dev_2_wen,
  output [31:0] io_dev_2_wdata,
  input  [31:0] io_dev_2_rdata,
  output [31:0] io_dev_3_addr,
  output [3:0]  io_dev_3_wen,
  output [31:0] io_dev_3_wdata,
  input  [31:0] io_dev_3_rdata,
  output [31:0] io_dev_4_addr,
  output [3:0]  io_dev_4_wen,
  output [31:0] io_dev_4_wdata,
  input  [31:0] io_dev_4_rdata
);

  wire within_range_0 = io_cpu_addr < 32'h1000;
  wire within_range_1 = (|(io_cpu_addr[31:12])) & io_cpu_addr < 32'h1004;
  wire within_range_2 = io_cpu_addr > 32'h105F & io_cpu_addr < 32'h1063;
  wire within_range_3 = io_cpu_addr > 32'h106F & io_cpu_addr < 32'h1073;
  wire within_range_4 = io_cpu_addr > 32'h1077 & io_cpu_addr < 32'h1079;
  assign io_cpu_rdata =
    (within_range_0 ? io_dev_0_rdata : 32'h0) | (within_range_1 ? io_dev_1_rdata : 32'h0)
    | (within_range_2 ? io_dev_2_rdata : 32'h0)
    | (within_range_3 ? io_dev_3_rdata : 32'h0)
    | (within_range_4 ? io_dev_4_rdata : 32'h0);
  assign io_dev_0_addr = io_cpu_addr;
  assign io_dev_0_wen = {2'h0, io_cpu_wen[1:0] & {2{within_range_0}}};
  assign io_dev_0_wdata = io_cpu_wdata;
  assign io_dev_1_addr = io_cpu_addr;
  assign io_dev_1_wen = {2'h0, io_cpu_wen[1:0] & {2{within_range_1}}};
  assign io_dev_1_wdata = io_cpu_wdata;
  assign io_dev_2_addr = io_cpu_addr;
  assign io_dev_2_wen = {2'h0, io_cpu_wen[1:0] & {2{within_range_2}}};
  assign io_dev_2_wdata = io_cpu_wdata;
  assign io_dev_3_addr = io_cpu_addr;
  assign io_dev_3_wen = {2'h0, io_cpu_wen[1:0] & {2{within_range_3}}};
  assign io_dev_3_wdata = io_cpu_wdata;
  assign io_dev_4_addr = io_cpu_addr;
  assign io_dev_4_wen = {2'h0, io_cpu_wen[1:0] & {2{within_range_4}}};
  assign io_dev_4_wdata = io_cpu_wdata;
endmodule

