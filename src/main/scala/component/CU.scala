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
  *   解码，会生成一排控制信号。然后也会进行符号拓展操作, 输出寄存器的编号
  */
class CU extends Module with HasCoreParameter with HasRegFileParameter {
  val io = IO(new Bundle {
    val inst = Input(UInt(XLEN.W))
    // 控制信号
    val alu_op  = Output(ALUOpType())
    val op1_sel = Output(OP1_sel())
    val op2_sel = Output(OP2_sel())
    val op_mem  = Output(MemUOpType())
    val csr_op  = Output(CSRUOpType())
    val wb_sel  = Output(WB_sel())
    val npc_op  = Output(NPCOpType())
    val bru_op  = Output(BRUOpType())
    // 立即数: 正常情况下 SignExt, CSR 的时候 ZeroExt
    val imm = Output(UInt(XLEN.W))
    // 寄存器编号
    val rs1 = Output(UInt(NRRegbits.W))
    val rs2 = Output(UInt(NRRegbits.W))
    val rd  = Output(UInt(NRRegbits.W))
    //
  })

  // 最常见的还是 pc + 4
  io.npc_op := NPCOpType.npc_4

  /* ---------- default ---------- */
  // FIXME 测试: 1. reset 的时候才会 default
  // 2. 还是每个周期都会有 default
  io.alu_op  := ALUOpType.alu_X
  io.op1_sel := OP1_sel.op1sel_ZERO
  io.op2_sel := OP2_sel.op2sel_ZERO
  io.op_mem  := MemUOpType.mem_X
  io.csr_op  := CSRUOpType.csru_X
  io.wb_sel  := WB_sel.wbsel_X
  io.bru_op  := BRUOpType.bru_X
  io.imm     := 0.U

  io.rs1 := io.inst(19, 15)
  io.rs2 := io.inst(24, 20)

  // 这里设计是: 不写入就是 写入 x0
  io.rd := 0.U /* 默认是不写入 */

  /* ---------- store ---------- */
  private def store(op_mem_type: MemUOpType.Type) = {
    io.alu_op  := ALUOpType.alu_ADD // rs1 + sext(offset)
    io.op1_sel := OP1_sel.op1sel_RS1
    io.op2_sel := OP2_sel.op2sel_SEXT
    io.op_mem  := op_mem_type
    io.imm     := SignExt(io.inst(31, 25) ## io.inst(11, 7))
  }
  when(io.inst === Instructions.SW) {
    store(MemUOpType.mem_SW)
  }
  when(io.inst === Instructions.SH) {
    store(MemUOpType.mem_SH)
  }
  when(io.inst === Instructions.SB) {
    store(MemUOpType.mem_SB)
  }

  /* ---------- load ---------- */

  // val csignals /* : List */ = ListLookup(
  //   io.inst,
  //   List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_X, BRUOpType.bru_X), // default
  //   Array(
  //     // store
  //     Instructions.SW -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.mem_SW, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.SH -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.mem_SH, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.SB -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.mem_SB, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_4, BRUOpType.bru_X),
  //     // load
  //     Instructions.LB  -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.mem_LB, CSRUOpType.csru_X, WB_sel.wbsel_MEM, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.LBU -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.mem_LBU, CSRUOpType.csru_X, WB_sel.wbsel_MEM, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.LH  -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.mem_LH, CSRUOpType.csru_X, WB_sel.wbsel_MEM, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.LHU -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.mem_LHU, CSRUOpType.csru_X, WB_sel.wbsel_MEM, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.LW  -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.mem_LW, CSRUOpType.csru_X, WB_sel.wbsel_MEM, NPCOpType.npc_4, BRUOpType.bru_X),
  //     // R-type
  //     Instructions.ADD  -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_RS1, OP2_sel.op2sel_RS2, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.SUB  -> List(ALUOpType.alu_SUB, OP1_sel.op1sel_RS1, OP2_sel.op2sel_RS2, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.AND  -> List(ALUOpType.alu_AND, OP1_sel.op1sel_RS1, OP2_sel.op2sel_RS2, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.OR   -> List(ALUOpType.alu_OR, OP1_sel.op1sel_RS1, OP2_sel.op2sel_RS2, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.XOR  -> List(ALUOpType.alu_XOR, OP1_sel.op1sel_RS1, OP2_sel.op2sel_RS2, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.SLL  -> List(ALUOpType.alu_SLL, OP1_sel.op1sel_RS1, OP2_sel.op2sel_RS2, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.SRL  -> List(ALUOpType.alu_SRL, OP1_sel.op1sel_RS1, OP2_sel.op2sel_RS2, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.SRA  -> List(ALUOpType.alu_SRA, OP1_sel.op1sel_RS1, OP2_sel.op2sel_RS2, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.SLT  -> List(ALUOpType.alu_SLT, OP1_sel.op1sel_RS1, OP2_sel.op2sel_RS2, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.SLTU -> List(ALUOpType.alu_SLTU, OP1_sel.op1sel_RS1, OP2_sel.op2sel_RS2, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
  //     // I-type
  //     Instructions.ADDI  -> List(ALUOpType.alu_ADD, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.ANDI  -> List(ALUOpType.alu_AND, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.ORI   -> List(ALUOpType.alu_OR, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.XORI  -> List(ALUOpType.alu_XOR, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.SLLI  -> List(ALUOpType.alu_SLL, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.SRLI  -> List(ALUOpType.alu_SRL, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.SRAI  -> List(ALUOpType.alu_SRA, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.SLTI  -> List(ALUOpType.alu_SLT, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
  //     Instructions.SLTIU -> List(ALUOpType.alu_SLTU, OP1_sel.op1sel_RS1, OP2_sel.op2sel_SEXT, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_ALU, NPCOpType.npc_4, BRUOpType.bru_X),
  //     // B-type 分支判断不交给 alu, 因此 alu 的输入直接是 0
  //     Instructions.BEQ  -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_BR, BRUOpType.bru_BEQ),
  //     Instructions.BNE  -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_BR, BRUOpType.bru_BNE),
  //     Instructions.BGE  -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_BR, BRUOpType.bru_BGE),
  //     Instructions.BGEU -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_BR, BRUOpType.bru_BGEU),
  //     Instructions.BLT  -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_BR, BRUOpType.bru_BLT),
  //     Instructions.BLTU -> List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_BR, BRUOpType.bru_BLTU),
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
