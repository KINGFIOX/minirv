/** @brief
  *   这个文件定义了控制信号，实现了 Decoder
  */

package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter
import common.Instructions

// /** @brief
//   *   这个只有 csr 需要
//   */
// object RS1OpType extends ChiselEnum {
//   val OP1_ZIMM, OP1_RS1 = Value
// }

/** @brief
  *   rs2 输入有 rs2 和 符号拓展立即数
  */
object OP2_sel extends ChiselEnum {
  val op2_SEXT, op2_RS2 = Value
}

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
    List(ALUOpType.alu_X, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_X), // default
    Array(
      // S-type
      Instructions.SW -> List(ALUOpType.alu_X, OP2_sel.op2_SEXT, MemUOpType.memu_SW, CSRUOpType.csru_X),
      Instructions.SH -> List(ALUOpType.alu_X, OP2_sel.op2_SEXT, MemUOpType.memu_SH, CSRUOpType.csru_X),
      Instructions.SB -> List(ALUOpType.alu_X, OP2_sel.op2_SEXT, MemUOpType.memu_SB, CSRUOpType.csru_X),
      // R-type
      Instructions.ADD  -> List(ALUOpType.alu_ADD, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.SUB  -> List(ALUOpType.alu_SUB, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.AND  -> List(ALUOpType.alu_AND, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.OR   -> List(ALUOpType.alu_OR, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.XOR  -> List(ALUOpType.alu_XOR, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.SLL  -> List(ALUOpType.alu_SLL, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.SRL  -> List(ALUOpType.alu_SRL, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.SRA  -> List(ALUOpType.alu_SRA, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.SLT  -> List(ALUOpType.alu_SLT, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.SLTU -> List(ALUOpType.alu_SLTU, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_X),
      // I-type
      Instructions.ADDI  -> List(ALUOpType.alu_ADD, OP2_sel.op2_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.ANDI  -> List(ALUOpType.alu_AND, OP2_sel.op2_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.ORI   -> List(ALUOpType.alu_OR, OP2_sel.op2_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.XORI  -> List(ALUOpType.alu_XOR, OP2_sel.op2_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.SLLI  -> List(ALUOpType.alu_SLL, OP2_sel.op2_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.SRLI  -> List(ALUOpType.alu_SRL, OP2_sel.op2_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.SRAI  -> List(ALUOpType.alu_SRA, OP2_sel.op2_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.SLTI  -> List(ALUOpType.alu_SLT, OP2_sel.op2_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.SLTIU -> List(ALUOpType.alu_SLTU, OP2_sel.op2_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.JALR  -> List(ALUOpType.alu_JALR, OP2_sel.op2_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X),
      // load
      Instructions.LB  -> List(ALUOpType.alu_ADD, OP2_sel.op2_SEXT, MemUOpType.memu_LB, CSRUOpType.csru_X),
      Instructions.LBU -> List(ALUOpType.alu_ADD, OP2_sel.op2_SEXT, MemUOpType.memu_LBU, CSRUOpType.csru_X),
      Instructions.LH  -> List(ALUOpType.alu_ADD, OP2_sel.op2_SEXT, MemUOpType.memu_LH, CSRUOpType.csru_X),
      Instructions.LHU -> List(ALUOpType.alu_ADD, OP2_sel.op2_SEXT, MemUOpType.memu_LHU, CSRUOpType.csru_X),
      Instructions.LW  -> List(ALUOpType.alu_ADD, OP2_sel.op2_SEXT, MemUOpType.memu_LW, CSRUOpType.csru_X),
      // B-type
      Instructions.BEQ  -> List(ALUOpType.alu_X, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.BNE  -> List(ALUOpType.alu_X, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.BGE  -> List(ALUOpType.alu_X, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.BGEU -> List(ALUOpType.alu_X, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.BLT  -> List(ALUOpType.alu_X, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.BLTU -> List(ALUOpType.alu_X, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_X),
      // U-type
      Instructions.LUI   -> List(ALUOpType.alu_ADD, OP2_sel.op2_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X),
      Instructions.AUIPC -> List(ALUOpType.alu_ADD, OP2_sel.op2_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X),
      // J-type
      Instructions.JAL -> List(ALUOpType.alu_ADD, OP2_sel.op2_SEXT, MemUOpType.memu_X, CSRUOpType.csru_X),
      // CSR
      Instructions.CSRRW  -> List(ALUOpType.alu_X, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_CSRRW),
      Instructions.CSRRWI -> List(ALUOpType.alu_X, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_CSRRW),
      Instructions.CSRRS  -> List(ALUOpType.alu_X, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_CSRRS),
      Instructions.CSRRSI -> List(ALUOpType.alu_X, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_CSRRS),
      Instructions.CSRRC  -> List(ALUOpType.alu_X, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_CSRRC),
      Instructions.CSRRCI -> List(ALUOpType.alu_X, OP2_sel.op2_RS2, MemUOpType.memu_X, CSRUOpType.csru_CSRRC)
      // // 环境
      // Instructions.ECALL -> List(ALUOpType.alu_X, OP2_X, MEN_X, REN_X, WB_X, CSRUOpType.csru_X),
      // Instructions.MRET  -> List(ALUOpType.alu_X, OP2_X, MEN_X, REN_X, WB_X, CSRUOpType.csru_X)
    )
  )

}
