// Generated by CIRCT firtool-1.62.0
module NPC(
  input         clock,
                reset,
  input  [31:0] io_pc,
  input  [2:0]  io_op,
  input  [31:0] io_addr,
  input         io_br_flag,
  output [31:0] io_npc,
                io_pc4
);

  wire [31:0]      _io_npc_T = io_pc + 32'h4;
  wire [7:0][31:0] _GEN =
    {{io_pc},
     {io_pc},
     {32'h1C090000},
     {io_addr},
     {io_addr + io_pc},
     {io_br_flag ? io_addr + io_pc : io_pc},
     {_io_npc_T},
     {io_pc}};
  assign io_npc = _GEN[io_op];
  assign io_pc4 = _io_npc_T;
endmodule

