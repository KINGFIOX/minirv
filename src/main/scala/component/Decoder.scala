/** @brief
  *   这个文件定义了控制信号，实现了 Decoder
  */

package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter
import common.Instructions

/* ---------- ---------- alu 的两端 mux ---------- ---------- */

object OP1_sel extends ChiselEnum {
  val op1sel_ZERO, op1sel_RS1, op1sel_PC = Value
}

/** @brief
  *   rs2 输入有 rs2 和 符号拓展立即数
  */
object OP2_sel extends ChiselEnum {
  val op2sel_ZERO, op2sel_SEXT, op2sel_RS2 = Value
}

// 因此
// csr 的时候, 两边都是 0
// branch 的时候，两端都是 0

/* ---------- ---------- alu 的两端 mux ---------- ---------- */

/** @brief
  *   解码，会生成一排控制信号。然后也会进行符号拓展操作
  */
class Decoder extends Module with HasCoreParameter {
  val io = IO(new Bundle {
    val inst = Input(UInt(XLEN.W))
    // output
  })

  val csignals /* : List */ = ListLookup(
    io.inst,
    List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_X, BRUOpType.bru_X), // default
    Array(
      // store
      Instructions.SW -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.memu_SW, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.SH -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.memu_SH, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.SB -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.memu_SB, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_4, BRUOpType.bru_X),
      // load
      Instructions.LB  -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.memu_LB, CSRUOpType.csru_X, WB_sel.wbsel_MEM, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.LBU -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.memu_LBU, CSRUOpType.csru_X, WB_sel.wbsel_MEM, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.LH  -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.memu_LH, CSRUOpType.csru_X, WB_sel.wbsel_MEM, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.LHU -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.memu_LHU, CSRUOpType.csru_X, WB_sel.wbsel_MEM, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.LW  -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.memu_LW, CSRUOpType.csru_X, WB_sel.wbsel_MEM, NPCOpType.npc_4, BRUOpType.bru_X),
      // R-type
      Instructions.ADD  -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_RS1, OP2_sel.op2sel_RS2, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.SUB  -> List(ALUOpType.alu_SUB, OP1_sel.op1sel_RS1, OP2_sel.op2sel_RS2, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.AND  -> List(ALUOpType.alu_AND, OP1_sel.op1sel_RS1, OP2_sel.op2sel_RS2, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.OR   -> List(ALUOpType.alu_OR, OP1_sel.op1sel_RS1, OP2_sel.op2sel_RS2, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.XOR  -> List(ALUOpType.alu_XOR, OP1_sel.op1sel_RS1, OP2_sel.op2sel_RS2, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.SLL  -> List(ALUOpType.alu_SLL, OP1_sel.op1sel_RS1, OP2_sel.op2sel_RS2, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.SRL  -> List(ALUOpType.alu_SRL, OP1_sel.op1sel_RS1, OP2_sel.op2sel_RS2, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.SRA  -> List(ALUOpType.alu_SRA, OP1_sel.op1sel_RS1, OP2_sel.op2sel_RS2, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.SLT  -> List(ALUOpType.alu_SLT, OP1_sel.op1sel_RS1, OP2_sel.op2sel_RS2, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.SLTU -> List(ALUOpType.alu_SLTU, OP1_sel.op1sel_RS1, OP2_sel.op2sel_RS2, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
      // I-type
      Instructions.ADDI  -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.ANDI  -> List(ALUOpType.alu_AND, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.ORI   -> List(ALUOpType.alu_OR, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.XORI  -> List(ALUOpType.alu_XOR, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.SLLI  -> List(ALUOpType.alu_SLL, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.SRLI  -> List(ALUOpType.alu_SRL, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.SRAI  -> List(ALUOpType.alu_SRA, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.SLTI  -> List(ALUOpType.alu_SLT, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.SLTIU -> List(ALUOpType.alu_SLTU, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
      // B-type 分支判断不交给 alu, 因此 alu 的输入直接是 0
      Instructions.BEQ  -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_BR, BRUOpType.bru_BEQ),
      Instructions.BNE  -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_BR, BRUOpType.bru_BNE),
      Instructions.BGE  -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_BR, BRUOpType.bru_BGE),
      Instructions.BGEU -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_BR, BRUOpType.bru_BGEU),
      Instructions.BLT  -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_BR, BRUOpType.bru_BLT),
      Instructions.BLTU -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_BR, BRUOpType.bru_BLTU),
      // jalr   // pc = (rs1 + sext(offset)) & ~1
      Instructions.JALR -> List(ALUOpType.alu_X, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_PC4, NPCOpType.npc_JALR, BRUOpType.bru_X),
      // jal
      Instructions.JAL -> List(ALUOpType.alu_X, OP1_sel.op1sel_PC, OP2_sel.op2sel_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_PC4, NPCOpType.npc_JAL, BRUOpType.bru_X), // 写回 pc4
      // U-type
      Instructions.LUI   -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X), // 立即数 + 0
      Instructions.AUIPC -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_PC, OP2_sel.op2sel_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X), // 立即数 + PC
      // CSR
      Instructions.CSRRW  -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.memu_X, CSRUOpType.csru_CSRRW, WB_sel.wbsel_CSR, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.CSRRWI -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.memu_X, CSRUOpType.csru_CSRRW, WB_sel.wbsel_CSR, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.CSRRS  -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.memu_X, CSRUOpType.csru_CSRRS, WB_sel.wbsel_CSR, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.CSRRSI -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.memu_X, CSRUOpType.csru_CSRRS, WB_sel.wbsel_CSR, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.CSRRC  -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.memu_X, CSRUOpType.csru_CSRRC, WB_sel.wbsel_CSR, NPCOpType.npc_4, BRUOpType.bru_X),
      Instructions.CSRRCI -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.memu_X, CSRUOpType.csru_CSRRC, WB_sel.wbsel_CSR, NPCOpType.npc_4, BRUOpType.bru_X)
      // // 环境
      // Instructions.ECALL -> List(ALUOpType.alu_X, OP2_X, MEN_X, REN_X, WB_X, CSRUOpType.csru_X),
      // Instructions.MRET  -> List(ALUOpType.alu_X, OP2_X, MEN_X, REN_X, WB_X, CSRUOpType.csru_X)
    )
  )

}

object Decoder extends App {
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new Decoder,
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
