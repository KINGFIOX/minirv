/** @brief
  *   这个文件定义了控制信号，实现了 Decoder
  */

package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter
import common.Instructions

object WB_sel extends ChiselEnum {
  val alu_X, alu_ADD, alu_SUB, alu_AND, alu_OR, alu_XOR, alu_SLL, alu_SRL, alu_SRA, alu_LT, alu_LTU, alu_GE, alu_GEU, alu_EQ, alu_NE, alu_JALR = Value
}

/** @brief
  *   解码，会生成一排控制信号。然后也会进行符号拓展操作
  */
class Decoder extends Module with HasCoreParameter {
  val io = IO(new Bundle {
    val inst = Input(UInt(XLEN.W))
    // output
  })

//   val csignals /* : List */ = ListLookup(
//     io.inst,
//     List(ALUOpType.alu_X, OP1_RS1, OP2_RS2, MEN_X, REN_X, WB_X, CSR_X), // default
//     Array(
//       // S-type
//       Instructions.SW -> List(ALUOpType.alu_X, OP1_RS1, OP2_IMS, MEN_S, REN_X, WB_X, CSR_X),
//       Instructions.SH -> List(ALUOpType.alu_X, OP1_RS1, OP2_IMS, MEN_S, REN_X, WB_X, CSR_X),
//       Instructions.SB -> List(ALUOpType.alu_X, OP1_RS1, OP2_IMS, MEN_S, REN_X, WB_X, CSR_X),
//       // R-type
//       Instructions.ADD  -> List(ALUOpType.alu_ADD, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU, CSR_X),
//       Instructions.SUB  -> List(ALUOpType.alu_SUB, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU, CSR_X),
//       Instructions.AND  -> List(ALUOpType.alu_AND, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU, CSR_X),
//       Instructions.OR   -> List(ALUOpType.alu_OR, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU, CSR_X),
//       Instructions.XOR  -> List(ALUOpType.alu_XOR, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU, CSR_X),
//       Instructions.SLL  -> List(ALUOpType.alu_SLL, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU, CSR_X),
//       Instructions.SRL  -> List(ALUOpType.alu_SRL, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU, CSR_X),
//       Instructions.SRA  -> List(ALUOpType.alu_SRA, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU, CSR_X),
//       Instructions.SLT  -> List(ALUOpType.alu_LT, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU, CSR_X),
//       Instructions.SLTU -> List(ALUOpType.alu_LTU, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU, CSR_X),
//       // I-type
//       Instructions.ADDI  -> List(ALUOpType.alu_ADD, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU, CSR_X),
//       Instructions.ANDI  -> List(ALUOpType.alu_AND, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU, CSR_X),
//       Instructions.ORI   -> List(ALUOpType.alu_OR, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU, CSR_X),
//       Instructions.XORI  -> List(ALUOpType.alu_XOR, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU, CSR_X),
//       Instructions.SLLI  -> List(ALUOpType.alu_SLL, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU, CSR_X),
//       Instructions.SRLI  -> List(ALUOpType.alu_SRL, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU, CSR_X),
//       Instructions.SRAI  -> List(ALUOpType.alu_SRA, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU, CSR_X),
//       Instructions.SLTI  -> List(ALUOpType.alu_LT, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU, CSR_X),
//       Instructions.SLTIU -> List(ALUOpType.alu_LTU, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU, CSR_X),
//       Instructions.JALR  -> List(ALUOpType.alu_JALR, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_PC, CSR_X),
//       Instructions.LB  -> List(ALUOpType.alu_ADD, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_MEM, CSR_X),
//       Instructions.LBU -> List(ALUOpType.alu_ADD, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_MEM, CSR_X),
//       Instructions.LH  -> List(ALUOpType.alu_ADD, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_MEM, CSR_X),
//       Instructions.LHU -> List(ALUOpType.alu_ADD, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_MEM, CSR_X),
//       Instructions.LW  -> List(ALUOpType.alu_ADD, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_MEM, CSR_X),
//       // B-type
//       Instructions.BEQ  -> List(ALUOpType.alu_EQ, OP1_RS1, OP2_RS2, MEN_X, REN_X, WB_X, CSR_X),
//       Instructions.BNE  -> List(ALUOpType.alu_NE, OP1_RS1, OP2_RS2, MEN_X, REN_X, WB_X, CSR_X),
//       Instructions.BGE  -> List(ALUOpType.alu_GE, OP1_RS1, OP2_RS2, MEN_X, REN_X, WB_X, CSR_X),
//       Instructions.BGEU -> List(ALUOpType.alu_GEU, OP1_RS1, OP2_RS2, MEN_X, REN_X, WB_X, CSR_X),
//       Instructions.BLT  -> List(ALUOpType.alu_LT, OP1_RS1, OP2_RS2, MEN_X, REN_X, WB_X, CSR_X),
//       Instructions.BLTU -> List(ALUOpType.alu_LTU, OP1_RS1, OP2_RS2, MEN_X, REN_X, WB_X, CSR_X),
//       // U-type
//       Instructions.LUI   -> List(ALUOpType.alu_ADD, OP1_X, OP2_IMU, MEN_X, REN_S, WB_ALU, CSR_X),
//       Instructions.AUIPC -> List(ALUOpType.alu_ADD, OP1_PC, OP2_IMU, MEN_X, REN_S, WB_ALU, CSR_X),
//       // J-type
//       Instructions.JAL -> List(ALUOpType.alu_ADD, OP1_PC, OP2_IMJ, MEN_X, REN_S, WB_PC, CSR_X),
//       // CSR
//       Instructions.CSRRW  -> List(ALU_COPY1, OP1_RS1, OP2_X, MEN_X, REN_S, WB_CSR, CSR_W),
//       Instructions.CSRRWI -> List(ALU_COPY1, OP1_IMZ, OP2_X, MEN_X, REN_S, WB_CSR, CSR_W),
//       Instructions.CSRRS  -> List(ALU_COPY1, OP1_RS1, OP2_X, MEN_X, REN_S, WB_CSR, CSR_S),
//       Instructions.CSRRSI -> List(ALU_COPY1, OP1_IMZ, OP2_X, MEN_X, REN_S, WB_CSR, CSR_S),
//       Instructions.CSRRC  -> List(ALU_COPY1, OP1_RS1, OP2_X, MEN_X, REN_S, WB_CSR, CSR_C),
//       Instructions.CSRRCI -> List(ALU_COPY1, OP1_IMZ, OP2_X, MEN_X, REN_S, WB_CSR, CSR_C),
//       Instructions.ECALL  -> List(ALUOpType.alu_X, OP1_X, OP2_X, MEN_X, REN_X, WB_X, CSR_E),
//       Instructions.MRET   -> List(ALUOpType.alu_X, OP1_X, OP2_X, MEN_X, REN_X, WB_X, CSR_E)
//     )
//   )

}
