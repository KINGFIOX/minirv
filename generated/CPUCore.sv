// Generated by CIRCT firtool-1.62.0
// Standard header to adapt well known macros for prints and assertions.

// Users can define 'PRINTF_COND' to add an extra gate to prints.
`ifndef PRINTF_COND_
`ifdef PRINTF_COND
`define PRINTF_COND_ (`PRINTF_COND)
`else  // PRINTF_COND
`define PRINTF_COND_ 1
`endif  // PRINTF_COND
`endif  // not def PRINTF_COND_

module IF (
    input         clock,
    reset,
    output [31:0] io_irom_addr,
    input  [31:0] io_irom_inst,
    output [31:0] io_out_inst,
    io_out_pc_4,
    input  [31:0] io_in_imm,
    input         io_in_br_flag,
    input  [ 2:0] io_in_op,
    input  [31:0] io_in_rs1_v
);

  reg  [31:0] pc;
  wire [31:0] _pc_T = pc + 32'h4;
  always @(posedge clock) begin
    if (reset) pc <= 32'h0;
    else begin
      automatic
      logic [7:0][31:0]
      _GEN = {
        {pc},
        {pc},
        {pc},
        {io_in_rs1_v + io_in_imm & 32'hFFFFFFFE},
        {pc + io_in_imm},
        {io_in_br_flag ? pc + io_in_rs1_v : pc},
        {_pc_T},
        {pc}
      };
      pc <= _GEN[io_in_op];
    end
  end  // always @(posedge)
  assign io_irom_addr = pc;
  assign io_out_inst  = io_irom_inst;
  assign io_out_pc_4  = _pc_T;
endmodule

module CU (
    input  [31:0] io_inst,
    output [ 3:0] io_ctrl_alu_op,
    output [ 1:0] io_ctrl_op1_sel,
    io_ctrl_op2_sel,
    output [ 3:0] io_ctrl_op_mem,
    output [ 2:0] io_ctrl_wb_sel,
    io_ctrl_npc_op,
    io_ctrl_bru_op,
    output [31:0] io_imm,
    output [ 4:0] io_rf_rs1_i,
    io_rf_rs2_i,
    io_rf_rd_i
);

  wire [9:0] _GEN = {io_inst[14:12], io_inst[6:0]};
  wire _GEN_0 = _GEN == 10'h123;
  wire _GEN_1 = _GEN == 10'hA3;
  wire _GEN_2 = _GEN == 10'h23;
  wire _GEN_3 = _GEN == 10'h3;
  wire _GEN_4 = _GEN == 10'h203;
  wire _GEN_5 = _GEN == 10'h83;
  wire _GEN_6 = _GEN == 10'h283;
  wire _GEN_7 = _GEN == 10'h103;
  wire _GEN_8 = _GEN_7 | _GEN_6 | _GEN_5 | _GEN_4 | _GEN_3 | _GEN_2 | _GEN_1 | _GEN_0;
  wire [16:0] _GEN_9 = {io_inst[31:25], io_inst[14:12], io_inst[6:0]};
  wire _GEN_10 = _GEN_9 == 17'h33;
  wire _GEN_11 = _GEN_10 | _GEN_8;
  wire _GEN_12 = _GEN_9 == 17'h8033;
  wire _GEN_13 = _GEN_9 == 17'h3B3;
  wire _GEN_14 = _GEN_9 == 17'h333;
  wire _GEN_15 = _GEN_9 == 17'h233;
  wire _GEN_16 = _GEN_9 == 17'hB3;
  wire _GEN_17 = _GEN_9 == 17'h2B3;
  wire _GEN_18 = _GEN_9 == 17'h82B3;
  wire _GEN_19 = _GEN_9 == 17'h133;
  wire _GEN_20 = _GEN_9 == 17'h1B3;
  wire        _GEN_21 =
    _GEN_20 | _GEN_19 | _GEN_18 | _GEN_17 | _GEN_16 | _GEN_15 | _GEN_14 | _GEN_13
    | _GEN_12 | _GEN_10;
  wire _GEN_22 = _GEN == 10'h13;
  wire _GEN_23 = _GEN == 10'h393;
  wire _GEN_24 = _GEN == 10'h313;
  wire _GEN_25 = _GEN == 10'h213;
  wire [15:0] _GEN_26 = {io_inst[31:26], io_inst[14:12], io_inst[6:0]};
  wire _GEN_27 = _GEN_26 == 16'h93;
  wire _GEN_28 = _GEN_26 == 16'h293;
  wire _GEN_29 = _GEN_26 == 16'h4293;
  wire _GEN_30 = _GEN == 10'h113;
  wire _GEN_31 = _GEN == 10'h193;
  wire _GEN_32 = _GEN == 10'h63;
  wire _GEN_33 = _GEN == 10'hE3;
  wire _GEN_34 = _GEN == 10'h2E3;
  wire _GEN_35 = _GEN == 10'h3E3;
  wire _GEN_36 = _GEN == 10'h263;
  wire _GEN_37 = _GEN == 10'h363;
  wire _GEN_38 = _GEN == 10'h67;
  wire _GEN_39 = io_inst[6:0] == 7'h6F;
  wire _GEN_40 = io_inst[6:0] == 7'h37;
  wire _GEN_41 = io_inst[6:0] == 7'h17;
  wire _GEN_42 = _GEN_41 | _GEN_40;
  assign io_ctrl_alu_op =
    _GEN_42
      ? 4'h1
      : _GEN_31
          ? 4'hA
          : _GEN_30
              ? 4'h9
              : _GEN_29
                  ? 4'h8
                  : _GEN_28
                      ? 4'h7
                      : _GEN_27
                          ? 4'h6
                          : _GEN_25
                              ? 4'h5
                              : _GEN_24
                                  ? 4'h4
                                  : _GEN_23
                                      ? 4'h3
                                      : _GEN_22
                                          ? 4'h1
                                          : _GEN_20
                                              ? 4'hA
                                              : _GEN_19
                                                  ? 4'h9
                                                  : _GEN_18
                                                      ? 4'h8
                                                      : _GEN_17
                                                          ? 4'h7
                                                          : _GEN_16
                                                              ? 4'h6
                                                              : _GEN_15
                                                                  ? 4'h5
                                                                  : _GEN_14
                                                                      ? 4'h4
                                                                      : _GEN_13
                                                                          ? 4'h3
                                                                          : _GEN_12
                                                                              ? 4'h2
                                                                              : {3'h0,
                                                                                 _GEN_11};
  assign io_ctrl_op1_sel =
    _GEN_41
      ? 2'h2
      : _GEN_40
          ? 2'h0
          : {1'h0,
             _GEN_31 | _GEN_30 | _GEN_29 | _GEN_28 | _GEN_27 | _GEN_25 | _GEN_24 | _GEN_23
               | _GEN_22 | _GEN_20 | _GEN_19 | _GEN_18 | _GEN_17 | _GEN_16 | _GEN_15
               | _GEN_14 | _GEN_13 | _GEN_12 | _GEN_11};
  assign io_ctrl_op2_sel =
    _GEN_41 | _GEN_40 | _GEN_31 | _GEN_30 | _GEN_29 | _GEN_28 | _GEN_27 | _GEN_25
    | _GEN_24 | _GEN_23 | _GEN_22
      ? 2'h1
      : _GEN_21 ? 2'h2 : {1'h0, _GEN_8};
  assign io_ctrl_op_mem =
    _GEN_7
      ? 4'h3
      : _GEN_6
          ? 4'h5
          : _GEN_5
              ? 4'h2
              : _GEN_4
                  ? 4'h4
                  : _GEN_3 ? 4'h1 : _GEN_2 ? 4'h6 : _GEN_1 ? 4'h7 : {_GEN_0, 3'h0};
  assign io_ctrl_wb_sel =
    _GEN_42
      ? 3'h1
      : _GEN_39 | _GEN_38
          ? 3'h4
          : _GEN_31 | _GEN_30 | _GEN_29 | _GEN_28 | _GEN_27 | _GEN_25 | _GEN_24 | _GEN_23
            | _GEN_22 | _GEN_21
              ? 3'h1
              : _GEN_7 | _GEN_6 | _GEN_5 | _GEN_4 | _GEN_3 ? 3'h3 : 3'h0;
  assign io_ctrl_npc_op =
    _GEN_39
      ? 3'h3
      : _GEN_38
          ? 3'h4
          : _GEN_37 | _GEN_36 | _GEN_35 | _GEN_34 | _GEN_33 | _GEN_32 ? 3'h2 : 3'h1;
  assign io_ctrl_bru_op =
    _GEN_37
      ? 3'h2
      : _GEN_36
          ? 3'h1
          : _GEN_35 ? 3'h4 : _GEN_34 ? 3'h3 : _GEN_33 ? 3'h6 : _GEN_32 ? 3'h5 : 3'h0;
  assign io_imm =
    _GEN_41
      ? {io_inst[31:12], 12'h0}
      : _GEN_40
          ? {io_inst[31:12], 12'h0}
          : _GEN_39
              ? {{12{io_inst[31]}}, io_inst[19:12], io_inst[20], io_inst[30:21], 1'h0}
              : _GEN_38
                  ? {{20{io_inst[31]}}, io_inst[31:20]}
                  : _GEN_37
                      ? {{20{io_inst[31]}},
                         io_inst[7],
                         io_inst[30:25],
                         io_inst[11:8],
                         1'h0}
                      : _GEN_36
                          ? {{20{io_inst[31]}},
                             io_inst[7],
                             io_inst[30:25],
                             io_inst[11:8],
                             1'h0}
                          : _GEN_35
                              ? {{20{io_inst[31]}},
                                 io_inst[7],
                                 io_inst[30:25],
                                 io_inst[11:8],
                                 1'h0}
                              : _GEN_34
                                  ? {{20{io_inst[31]}},
                                     io_inst[7],
                                     io_inst[30:25],
                                     io_inst[11:8],
                                     1'h0}
                                  : _GEN_33
                                      ? {{20{io_inst[31]}},
                                         io_inst[7],
                                         io_inst[30:25],
                                         io_inst[11:8],
                                         1'h0}
                                      : _GEN_32
                                          ? {{20{io_inst[31]}},
                                             io_inst[7],
                                             io_inst[30:25],
                                             io_inst[11:8],
                                             1'h0}
                                          : _GEN_31
                                              ? {20'h0, io_inst[31:20]}
                                              : _GEN_30
                                                  ? {20'h0, io_inst[31:20]}
                                                  : _GEN_29
                                                      ? {20'h0, io_inst[31:20]}
                                                      : _GEN_28
                                                          ? {20'h0, io_inst[31:20]}
                                                          : _GEN_27
                                                              ? {20'h0, io_inst[31:20]}
                                                              : _GEN_25
                                                                  ? {20'h0,
                                                                     io_inst[31:20]}
                                                                  : _GEN_24
                                                                      ? {20'h0,
                                                                         io_inst[31:20]}
                                                                      : _GEN_23
                                                                          ? {20'h0,
                                                                             io_inst[31:20]}
                                                                          : _GEN_22
                                                                              ? {20'h0,
                                                                                 io_inst[31:20]}
                                                                              : _GEN_7
                                                                                  ? {{20{io_inst[31]}},
                                                                                     io_inst[31:20]}
                                                                                  : _GEN_6
                                                                                      ? {{20{io_inst[31]}},
                                                                                         io_inst[31:20]}
                                                                                      : _GEN_5
                                                                                          ? {{20{io_inst[31]}},
                                                                                             io_inst[31:20]}
                                                                                          : _GEN_4
                                                                                              ? {{20{io_inst[31]}},
                                                                                                 io_inst[31:20]}
                                                                                              : _GEN_3
                                                                                                  ? {{20{io_inst[31]}},
                                                                                                     io_inst[31:20]}
                                                                                                  : _GEN_2
                                                                                                      ? {{20{io_inst[31]}},
                                                                                                         io_inst[31:25],
                                                                                                         io_inst[11:7]}
                                                                                                      : _GEN_1
                                                                                                          ? {{20{io_inst[31]}},
                                                                                                             io_inst[31:25],
                                                                                                             io_inst[11:7]}
                                                                                                          : _GEN_0
                                                                                                              ? {{20{io_inst[31]}},
                                                                                                                 io_inst[31:25],
                                                                                                                 io_inst[11:7]}
                                                                                                              : 32'h0;
  assign io_rf_rs1_i = io_inst[19:15];
  assign io_rf_rs2_i = io_inst[24:20];
  assign io_rf_rd_i =
    _GEN_41
      ? io_inst[11:7]
      : _GEN_40
          ? io_inst[11:7]
          : _GEN_39
              ? io_inst[11:7]
              : _GEN_38
                  ? io_inst[11:7]
                  : _GEN_31
                      ? io_inst[11:7]
                      : _GEN_30
                          ? io_inst[11:7]
                          : _GEN_29
                              ? io_inst[11:7]
                              : _GEN_28
                                  ? io_inst[11:7]
                                  : _GEN_27
                                      ? io_inst[11:7]
                                      : _GEN_25
                                          ? io_inst[11:7]
                                          : _GEN_24
                                              ? io_inst[11:7]
                                              : _GEN_23
                                                  ? io_inst[11:7]
                                                  : _GEN_22
                                                      ? io_inst[11:7]
                                                      : _GEN_20
                                                          ? io_inst[11:7]
                                                          : _GEN_19
                                                              ? io_inst[11:7]
                                                              : _GEN_18
                                                                  ? io_inst[11:7]
                                                                  : _GEN_17
                                                                      ? io_inst[11:7]
                                                                      : _GEN_16
                                                                          ? io_inst[11:7]
                                                                          : _GEN_15
                                                                              ? io_inst[11:7]
                                                                              : _GEN_14
                                                                                  ? io_inst[11:7]
                                                                                  : _GEN_13
                                                                                      ? io_inst[11:7]
                                                                                      : _GEN_12
                                                                                          ? io_inst[11:7]
                                                                                          : _GEN_10
                                                                                              ? io_inst[11:7]
                                                                                              : _GEN_7
                                                                                                  ? io_inst[11:7]
                                                                                                  : _GEN_6
                                                                                                      ? io_inst[11:7]
                                                                                                      : _GEN_5
                                                                                                          ? io_inst[11:7]
                                                                                                          : _GEN_4
                                                                                                              ? io_inst[11:7]
                                                                                                              : _GEN_3
                                                                                                                  ? io_inst[11:7]
                                                                                                                  : 5'h0;
endmodule

module ALU (
    input  [31:0] io_op1_v,
    io_op2_v,
    input  [ 3:0] io_alu_op,
    output [31:0] io_out
);

  wire [62:0] _io_out_T_16 = {31'h0, io_op1_v} << io_op2_v[4:0];
  wire [15:0][31:0] _GEN = {
    {32'h0},
    {32'h0},
    {32'h0},
    {32'h0},
    {32'h0},
    {{31'h0, io_op1_v < io_op2_v}},
    {{31'h0, $signed(io_op1_v) < $signed(io_op2_v)}},
    {$signed($signed(io_op1_v) >>> io_op2_v[4:0])},
    {io_op1_v >> io_op2_v[4:0]},
    {_io_out_T_16[31:0]},
    {io_op1_v ^ io_op2_v},
    {io_op1_v | io_op2_v},
    {io_op1_v & io_op2_v},
    {io_op1_v - io_op2_v},
    {io_op1_v + io_op2_v},
    {32'h0}
  };
  assign io_out = _GEN[io_alu_op];
endmodule

module BRU (
    input  [31:0] io_in_rs1_v,
    io_in_rs2_v,
    input  [ 2:0] io_in_bru_op,
    output        io_br_flag
);

  wire _GEN = io_in_bru_op == 3'h6 & io_in_rs1_v != io_in_rs2_v;
  wire [7:0] _GEN_0 = {
    {_GEN},
    {_GEN},
    {io_in_rs1_v == io_in_rs2_v},
    {io_in_rs1_v >= io_in_rs2_v},
    {$signed(io_in_rs1_v) >= $signed(io_in_rs2_v)},
    {io_in_rs1_v < io_in_rs2_v},
    {$signed(io_in_rs1_v) < $signed(io_in_rs2_v)},
    {_GEN}
  };
  assign io_br_flag = _GEN_0[io_in_bru_op];
endmodule

module MemU (
    input         clock,
    reset,
    input  [ 3:0] io_in_op,
    input  [31:0] io_in_addr,
    io_in_wdata,
    output [31:0] io_bus_addr,
    input  [31:0] io_bus_rdata,
    output [ 3:0] io_bus_wen,
    output [31:0] io_bus_wdata,
    io_out_rdata
);

  wire _GEN = io_in_op == 4'h1;
  wire [3:0][7:0] _GEN_0 = {
    {io_bus_rdata[31:24]}, {io_bus_rdata[23:16]}, {io_bus_rdata[15:8]}, {io_bus_rdata[7:0]}
  };
  wire [7:0] _GEN_1 = _GEN_0[io_in_addr[1:0]];
  wire _GEN_2 = io_in_op == 4'h2;
  wire _GEN_3 = io_in_addr[1:0] == 2'h2;
  wire _GEN_4 = ~(|(io_in_addr[1:0])) | _GEN_3;
  wire [7:0] _GEN_5 = _GEN_0[io_in_addr[1:0]+2'h1];
  wire _GEN_6 = io_in_op == 4'h3;
  wire _GEN_7 = io_in_op == 4'h4;
  wire _GEN_8 = io_in_op == 4'h5;
  wire _GEN_9 = io_in_op == 4'h6;
  wire _GEN_10 = io_in_op == 4'h7;
  wire _GEN_11 = io_in_op == 4'h8;
`ifndef SYNTHESIS
  always @(posedge clock) begin
    automatic logic _GEN_12 = (|io_in_op) & ~_GEN;
    automatic logic _GEN_13 = _GEN_12 & ~_GEN_2;
    automatic logic _GEN_14 = _GEN_13 & ~_GEN_6 & ~_GEN_7;
    automatic logic _GEN_15 = _GEN_14 & ~_GEN_8 & ~_GEN_9;
    if ((`PRINTF_COND_) & _GEN_12 & _GEN_2 & ~_GEN_4 & ~reset)
      $fwrite(32'h80000002, "Unaligned memory access at %x\n", io_in_addr);
    if ((`PRINTF_COND_) & _GEN_13 & _GEN_6 & (|(io_in_addr[1:0])) & ~reset)
      $fwrite(32'h80000002, "Unaligned memory access at %x\n", io_in_addr);
    if ((`PRINTF_COND_) & _GEN_14 & _GEN_8 & ~_GEN_4 & ~reset)
      $fwrite(32'h80000002, "Unaligned memory access at %x\n", io_in_addr);
    if ((`PRINTF_COND_) & _GEN_15 & _GEN_10
          & (io_in_addr[1:0] == 2'h1 | (&(io_in_addr[1:0]))) & ~reset)
      $fwrite(32'h80000002, "Unaligned memory access at %x\n", io_in_addr);
    if ((`PRINTF_COND_) & _GEN_15 & ~_GEN_10 & _GEN_11 & (|(io_in_addr[1:0])) & ~reset)
      $fwrite(32'h80000002, "Unaligned memory access at %x\n", io_in_addr);
  end  // always @(posedge)
`endif  // not def SYNTHESIS
  assign io_bus_addr = {io_in_addr[31:2], 2'h0};
  assign io_bus_wen =
    ~(|io_in_op) | _GEN | _GEN_2 | _GEN_6 | _GEN_7 | _GEN_8
      ? 4'h0
      : _GEN_9
          ? 4'h1 << io_in_addr[1:0]
          : _GEN_10
              ? (_GEN_3 ? 4'hC : (|(io_in_addr[1:0])) ? 4'h0 : 4'h3)
              : {4{_GEN_11 & ~(|(io_in_addr[1:0]))}};
  assign io_bus_wdata = io_in_wdata;
  assign io_out_rdata =
    (|io_in_op)
      ? (_GEN
           ? {{24{_GEN_1[7]}}, _GEN_1}
           : _GEN_2
               ? (_GEN_4 ? {{16{_GEN_5[7]}}, _GEN_5, _GEN_1} : 32'h0)
               : _GEN_6
                   ? ((|(io_in_addr[1:0])) ? 32'h0 : io_bus_rdata)
                   : _GEN_7
                       ? {24'h0, _GEN_1}
                       : _GEN_8 & _GEN_4
                           ? {16'h0, _GEN_0[io_in_addr[1:0] + 2'h1], _GEN_1}
                           : 32'h0)
      : 32'h0;
endmodule

// VCS coverage exclude_file
module _rf_32x32 (
    input  [ 4:0] R0_addr,
    input         R0_en,
    R0_clk,
    output [31:0] R0_data,
    input  [ 4:0] R1_addr,
    input         R1_en,
    R1_clk,
    output [31:0] R1_data,
    input  [ 4:0] W0_addr,
    input         W0_en,
    W0_clk,
    input  [31:0] W0_data,
    input  [ 4:0] W1_addr,
    input         W1_en,
    W1_clk,
    input  [31:0] W1_data,
    input  [ 4:0] W2_addr,
    input         W2_en,
    W2_clk,
    input  [31:0] W2_data
);

  reg [31:0] Memory[0:31];
  always @(posedge W0_clk) begin
    if (W0_en & 1'h1) Memory[W0_addr] <= W0_data;
    if (W1_en & 1'h1) Memory[W1_addr] <= W1_data;
    if (W2_en & 1'h1) Memory[W2_addr] <= W2_data;
  end  // always @(posedge)
  assign R0_data = R0_en ? Memory[R0_addr] : 32'bx;
  assign R1_data = R1_en ? Memory[R1_addr] : 32'bx;
endmodule

module CPUCore (
    input         clock,
    reset,
    output [31:0] io_inst_rom_addr,
    input  [31:0] io_inst_rom_inst,
    output [31:0] io_bus_addr,
    input  [31:0] io_bus_rdata,
    output [ 3:0] io_bus_wen,
    output [31:0] io_bus_wdata
);

  wire [31:0] _mem__io_out_rdata;
  wire        _bru__io_br_flag;
  wire [31:0] _alu__io_out;
  wire [31:0] __rf_ext_R0_data;
  wire [31:0] __rf_ext_R1_data;
  wire [ 3:0] _cu__io_ctrl_alu_op;
  wire [ 1:0] _cu__io_ctrl_op1_sel;
  wire [ 1:0] _cu__io_ctrl_op2_sel;
  wire [ 3:0] _cu__io_ctrl_op_mem;
  wire [ 2:0] _cu__io_ctrl_wb_sel;
  wire [ 2:0] _cu__io_ctrl_npc_op;
  wire [ 2:0] _cu__io_ctrl_bru_op;
  wire [31:0] _cu__io_imm;
  wire [ 4:0] _cu__io_rf_rs1_i;
  wire [ 4:0] _cu__io_rf_rs2_i;
  wire [ 4:0] _cu__io_rf_rd_i;
  wire [31:0] _if__io_out_inst;
  wire [31:0] _if__io_out_pc_4;
  wire        _if__io_in_rs1_v_T = _cu__io_rf_rs1_i == 5'h0;
  wire        _mem__io_in_wdata_T = _cu__io_rf_rs2_i == 5'h0;
  wire        _GEN = _cu__io_ctrl_wb_sel == 3'h0;
  wire        _GEN_0 = _cu__io_ctrl_wb_sel == 3'h1;
  wire        _GEN_1 = _cu__io_rf_rd_i == 5'h0;
  wire        _GEN_2 = _cu__io_ctrl_wb_sel == 3'h2;
  wire        _GEN_3 = _cu__io_ctrl_wb_sel == 3'h3;
  IF if_ (
      .clock        (clock),
      .reset        (reset),
      .io_irom_addr (io_inst_rom_addr),
      .io_irom_inst (io_inst_rom_inst),
      .io_out_inst  (_if__io_out_inst),
      .io_out_pc_4  (_if__io_out_pc_4),
      .io_in_imm    (_cu__io_imm),
      .io_in_br_flag(_bru__io_br_flag),
      .io_in_op     (_cu__io_ctrl_npc_op),
      .io_in_rs1_v  (_if__io_in_rs1_v_T ? 32'h0 : __rf_ext_R1_data)
  );
  CU cu_ (
      .io_inst        (_if__io_out_inst),
      .io_ctrl_alu_op (_cu__io_ctrl_alu_op),
      .io_ctrl_op1_sel(_cu__io_ctrl_op1_sel),
      .io_ctrl_op2_sel(_cu__io_ctrl_op2_sel),
      .io_ctrl_op_mem (_cu__io_ctrl_op_mem),
      .io_ctrl_wb_sel (_cu__io_ctrl_wb_sel),
      .io_ctrl_npc_op (_cu__io_ctrl_npc_op),
      .io_ctrl_bru_op (_cu__io_ctrl_bru_op),
      .io_imm         (_cu__io_imm),
      .io_rf_rs1_i    (_cu__io_rf_rs1_i),
      .io_rf_rs2_i    (_cu__io_rf_rs2_i),
      .io_rf_rd_i     (_cu__io_rf_rd_i)
  );
  _rf_32x32 _rf_ext (
      .R0_addr(_cu__io_rf_rs2_i),
      .R0_en  (1'h1),
      .R0_clk (clock),
      .R0_data(__rf_ext_R0_data),
      .R1_addr(_cu__io_rf_rs1_i),
      .R1_en  (1'h1),
      .R1_clk (clock),
      .R1_data(__rf_ext_R1_data),
      .W0_addr(_cu__io_rf_rd_i),
      .W0_en  (~(_GEN | _GEN_0 | _GEN_2 | _GEN_3) & _cu__io_ctrl_wb_sel == 3'h4),
      .W0_clk (clock),
      .W0_data(_GEN_1 ? 32'h0 : _if__io_out_pc_4),
      .W1_addr(_cu__io_rf_rd_i),
      .W1_en  (~(_GEN | _GEN_0 | _GEN_2) & _GEN_3),
      .W1_clk (clock),
      .W1_data(_GEN_1 ? 32'h0 : _mem__io_out_rdata),
      .W2_addr(_cu__io_rf_rd_i),
      .W2_en  (~_GEN & _GEN_0),
      .W2_clk (clock),
      .W2_data(_GEN_1 ? 32'h0 : _alu__io_out)
  );
  ALU alu_ (
      .io_op1_v
      ((_cu__io_ctrl_op1_sel != 2'h1 | _if__io_in_rs1_v_T ? 32'h0 : __rf_ext_R1_data)
       | (_cu__io_ctrl_op1_sel == 2'h2 ? _if__io_out_pc_4 - 32'h4 : 32'h0)),
      .io_op2_v
      ((_cu__io_ctrl_op2_sel == 2'h1 ? _cu__io_imm : 32'h0)
       | (_cu__io_ctrl_op2_sel != 2'h2 | _mem__io_in_wdata_T ? 32'h0 : __rf_ext_R0_data)),
      .io_alu_op(_cu__io_ctrl_alu_op),
      .io_out(_alu__io_out)
  );
  BRU bru_ (
      .io_in_rs1_v (_if__io_in_rs1_v_T ? 32'h0 : __rf_ext_R1_data),
      .io_in_rs2_v (_mem__io_in_wdata_T ? 32'h0 : __rf_ext_R0_data),
      .io_in_bru_op(_cu__io_ctrl_bru_op),
      .io_br_flag  (_bru__io_br_flag)
  );
  MemU mem_ (
      .clock       (clock),
      .reset       (reset),
      .io_in_op    (_cu__io_ctrl_op_mem),
      .io_in_addr  (_alu__io_out),
      .io_in_wdata (_mem__io_in_wdata_T ? 32'h0 : __rf_ext_R0_data),
      .io_bus_addr (io_bus_addr),
      .io_bus_rdata(io_bus_rdata),
      .io_bus_wen  (io_bus_wen),
      .io_bus_wdata(io_bus_wdata),
      .io_out_rdata(_mem__io_out_rdata)
  );
endmodule

