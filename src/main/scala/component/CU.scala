/** @brief
  *   这个文件定义了控制信号，实现了 control unit, control unit 集成了立即数输出的功能
  */

package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter
import common.Instructions
import utils.SignExt
import common.HasRegFileParameter
import component.OP1_sel.{op1sel_PC => op1sel_PC}

/* ---------- ---------- alu 的两端 mux ---------- ---------- */

object OP1_sel extends ChiselEnum {
  val op1sel_ZERO, op1sel_RS1, op1sel_PC = Value
}

/** @brief
  *   rs2 输入有 rs2 和 符号拓展立即数
  */
object OP2_sel extends ChiselEnum {
  val op2sel_ZERO, op2sel_IMM, op2sel_RS2 = Value
}

/* ---------- ---------- 控制信号线 ---------- ---------- */

class CUControlBundle extends Bundle {
  // 控制信号
  val alu_op  = Output(ALUOpType())
  val op1_sel = Output(OP1_sel())
  val op2_sel = Output(OP2_sel())
  val op_mem  = Output(MemUOpType())
  val csr_op  = Output(CSRUOpType())
  val wb_sel  = Output(WB_sel())
  val npc_op  = Output(NPCOpType())
  val bru_op  = Output(BRUOpType())
}

/** @brief
  *   这里 _i 表示 index
  */
class CURegFileBundle extends Bundle with HasRegFileParameter {
  val rs1_i = Output(UInt(NRRegbits.W))
  val rs2_i = Output(UInt(NRRegbits.W))
  val rd_i  = Output(UInt(NRRegbits.W))
}

/** @brief
  *   解码，会生成一排控制信号。然后也会进行符号拓展操作, 输出寄存器的编号
  */
class CU extends Module with HasCoreParameter with HasRegFileParameter {
  val io = IO(new Bundle {
    val inst = Input(UInt(XLEN.W))
    val ctrl = new CUControlBundle // 控制线
    val imm  = Output(UInt(XLEN.W)) // 立即数: 正常情况下 SignExt, CSR 的时候 ZeroExt
    val rf   = new CURegFileBundle
  })

  // 最常见的还是 pc + 4
  io.ctrl.npc_op := NPCOpType.npc_4

  /* ---------- default ---------- */

  // 控制信号
  io.ctrl.alu_op  := ALUOpType.alu_X
  io.ctrl.op1_sel := OP1_sel.op1sel_ZERO
  io.ctrl.op2_sel := OP2_sel.op2sel_ZERO
  io.ctrl.op_mem  := MemUOpType.mem_X
  io.ctrl.csr_op  := CSRUOpType.csru_X
  io.ctrl.wb_sel  := WB_sel.wbsel_X
  io.ctrl.bru_op  := BRUOpType.bru_X

  // 立即数
  io.imm := 0.U

  // 寄存器
  io.rf.rs1_i := io.inst(19, 15)
  io.rf.rs2_i := io.inst(24, 20)
  io.rf.rd_i  := 0.U /* 默认是不写入 */

  /* ---------- store ---------- */

  // sw rs2, offset(rs1) 存的是 rs2, 但是计算的是 op1=rs1 和 op2=offset
  private def store_inst(op: MemUOpType.Type) = {
    io.ctrl.alu_op  := ALUOpType.alu_ADD // rs1 + sext(offset)
    io.ctrl.op1_sel := OP1_sel.op1sel_RS1 // rs1
    io.ctrl.op2_sel := OP2_sel.op2sel_IMM // offset
    io.ctrl.op_mem  := op
    io.imm          := SignExt(io.inst(31, 25) ## io.inst(11, 7))
  }
  when(io.inst === Instructions.SW) {
    store_inst(MemUOpType.mem_SW)
  }
  when(io.inst === Instructions.SH) {
    store_inst(MemUOpType.mem_SH)
  }
  when(io.inst === Instructions.SB) {
    store_inst(MemUOpType.mem_SB)
  }

  /* ---------- load ---------- */

  // lw rd, offset(rs1)
  private def load_inst(op: MemUOpType.Type) = {
    io.ctrl.alu_op  := ALUOpType.alu_ADD // rs1 + sext(offset)
    io.ctrl.op1_sel := OP1_sel.op1sel_RS1 // rs1
    io.ctrl.op2_sel := OP2_sel.op2sel_IMM // offset
    io.imm          := SignExt(io.inst(31, 20))
    io.ctrl.op_mem  := op
    io.ctrl.wb_sel  := WB_sel.wbsel_MEM
    io.rf.rd_i      := io.inst(11, 7)
  }
  when(io.inst === Instructions.LB) {
    load_inst(MemUOpType.mem_LB)
  }
  when(io.inst === Instructions.LBU) {
    load_inst(MemUOpType.mem_LBU)
  }
  when(io.inst === Instructions.LH) {
    load_inst(MemUOpType.mem_LH)
  }
  when(io.inst === Instructions.LHU) {
    load_inst(MemUOpType.mem_LHU)
  }
  when(io.inst === Instructions.LW) {
    load_inst(MemUOpType.mem_LW)
  }

  /* ---------- R-type ---------- */

  // add rd, rs1, rs2
  private def R_inst(op: ALUOpType.Type) = {
    io.ctrl.alu_op  := op
    io.ctrl.op1_sel := OP1_sel.op1sel_RS1
    io.ctrl.op2_sel := OP2_sel.op2sel_RS2
    io.ctrl.wb_sel  := WB_sel.wbsel_ALU
    io.rf.rd_i      := io.inst(11, 7)
  }
  when(io.inst === Instructions.ADD) {
    R_inst(ALUOpType.alu_ADD)
  }
  when(io.inst === Instructions.SUB) {
    R_inst(ALUOpType.alu_SUB)
  }
  when(io.inst === Instructions.AND) {
    R_inst(ALUOpType.alu_AND)
  }
  when(io.inst === Instructions.OR) {
    R_inst(ALUOpType.alu_OR)
  }
  when(io.inst === Instructions.XOR) {
    R_inst(ALUOpType.alu_XOR)
  }
  when(io.inst === Instructions.SLL) {
    R_inst(ALUOpType.alu_SLL)
  }
  when(io.inst === Instructions.SRL) {
    R_inst(ALUOpType.alu_SRL)
  }
  when(io.inst === Instructions.SRA) {
    R_inst(ALUOpType.alu_SRA)
  }
  when(io.inst === Instructions.SLT) {
    R_inst(ALUOpType.alu_SLT)
  }
  when(io.inst === Instructions.SLTU) {
    R_inst(ALUOpType.alu_SLTU)
  }

  /* ---------- I-type ---------- */

  private def I_inst(op: ALUOpType.Type) = {
    io.ctrl.alu_op  := op
    io.ctrl.op1_sel := OP1_sel.op1sel_RS1
    io.ctrl.op2_sel := OP2_sel.op2sel_IMM
    io.imm          := io.inst(31, 20)
    io.rf.rd_i      := io.inst(11, 7)
    io.ctrl.wb_sel  := WB_sel.wbsel_ALU
  }
  when(io.inst === Instructions.ADDI) {
    I_inst(ALUOpType.alu_ADD)
  }
  when(io.inst === Instructions.ANDI) {
    I_inst(ALUOpType.alu_AND)
  }
  when(io.inst === Instructions.ORI) {
    I_inst(ALUOpType.alu_OR)
  }
  when(io.inst === Instructions.XORI) {
    I_inst(ALUOpType.alu_XOR)
  }
  when(io.inst === Instructions.SLLI) {
    I_inst(ALUOpType.alu_SLL)
  }
  when(io.inst === Instructions.SRLI) {
    I_inst(ALUOpType.alu_SRL)
  }
  when(io.inst === Instructions.SRAI) {
    I_inst(ALUOpType.alu_SRA)
  }
  when(io.inst === Instructions.SLTI) {
    I_inst(ALUOpType.alu_SLT)
  }
  when(io.inst === Instructions.SLTIU) {
    I_inst(ALUOpType.alu_SLTU)
  }

  /* ---------- Branch ---------- */

  private def B_inst(op: BRUOpType.Type) = {
    io.ctrl.bru_op := op
    io.imm         := SignExt(io.inst(31) ## io.inst(7) ## io.inst(30, 25) ## io.inst(11, 8) ## 0.U(1.W))
    io.ctrl.npc_op := NPCOpType.npc_BR
  }

  when(io.inst === Instructions.BEQ) {
    B_inst(BRUOpType.bru_BEQ)
  }
  when(io.inst === Instructions.BNE) {
    B_inst(BRUOpType.bru_BNE)
  }
  when(io.inst === Instructions.BGE) {
    B_inst(BRUOpType.bru_BGE)
  }
  when(io.inst === Instructions.BGEU) {
    B_inst(BRUOpType.bru_BGEU)
  }
  when(io.inst === Instructions.BLT) {
    B_inst(BRUOpType.bru_BLT)
  }
  when(io.inst === Instructions.BLTU) {
    B_inst(BRUOpType.bru_BLTU)
  }

  // val csignals /* : List */ = ListLookup(
  //   io.inst,
  //   List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_X, BRUOpType.bru_X), // default
  //   Array(
  //     // jalr   // pc = (rs1 + sext(offset)) & ~1
  //     Instructions.JALR -> List(ALUOpType.alu_X, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_PC4, NPCOpType.npc_JALR, BRUOpType.bru_X),
  //     // jal
  //     Instructions.JAL -> List(ALUOpType.alu_X, OP1_sel.op1sel_PC, OP2_sel.op2sel_SEXT, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_PC4, NPCOpType.npc_JAL, BRUOpType.bru_X), // 写回 pc4
  //     // U-type
  //     Instructions.LUI   -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_SEXT, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X), // 立即数 + 0
  //     Instructions.AUIPC -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_PC, OP2_sel.op2sel_SEXT, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X), // 立即数 + PC
  //     // CSR
  //     Instructions.CSRRW  -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.mem_X, CSRUOpType.csru_CSRRW, WB_sel.wbsel_CSR, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.CSRRWI -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.mem_X, CSRUOpType.csru_CSRRW, WB_sel.wbsel_CSR, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.CSRRS  -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.mem_X, CSRUOpType.csru_CSRRS, WB_sel.wbsel_CSR, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.CSRRSI -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.mem_X, CSRUOpType.csru_CSRRS, WB_sel.wbsel_CSR, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.CSRRC  -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.mem_X, CSRUOpType.csru_CSRRC, WB_sel.wbsel_CSR, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.CSRRCI -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.mem_X, CSRUOpType.csru_CSRRC, WB_sel.wbsel_CSR, NPCOpType.npc_4, BRUOpType.bru_X)
  //     // // 环境
  //     // Instructions.ECALL -> List(ALUOpType.alu_X, OP2_X, MEN_X, REN_X, WB_X, CSRUOpType.csru_X),
  //     // Instructions.MRET  -> List(ALUOpType.alu_X, OP2_X, MEN_X, REN_X, WB_X, CSRUOpType.csru_X)
  //   )
  // )

}

object CU extends App {
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new CU,
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
