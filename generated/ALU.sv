// Generated by CIRCT firtool-1.62.0
module ALU (
    input         clock,
    reset,
    input  [31:0] io_rs1,
    io_rs2,
    input  [ 3:0] io_alu_op,
    output [31:0] io_out
);

  wire [62:0] _io_out_T_16 = {31'h0, io_rs1} << io_rs2[4:0];
  wire [15:0][31:0] _GEN = {
    {32'h0},
    {32'h0},
    {32'h0},
    {32'h0},
    {io_rs1 + io_rs2 & 32'hFFFFFFFE},
    {{31'h0, io_rs1 < io_rs2}},
    {{31'h0, $signed(io_rs1) < $signed(io_rs2)}},
    {$signed($signed(io_rs1) >>> io_rs2[4:0])},
    {io_rs1 >> io_rs2[4:0]},
    {_io_out_T_16[31:0]},
    {io_rs1 ^ io_rs2},
    {io_rs1 | io_rs2},
    {io_rs1 & io_rs2},
    {io_rs1 - io_rs2},
    {io_rs1 + io_rs2},
    {32'h0}
  };
  assign io_out = _GEN[io_alu_op];
endmodule

