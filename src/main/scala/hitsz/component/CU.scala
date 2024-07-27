/** @brief
  *   这个文件定义了控制信号，实现了 control unit, control unit 集成了立即数输出的功能
  */

package hitsz.component

import chisel3._
import chisel3.util._
import hitsz.common.HasCoreParameter
import hitsz.common.Instructions
import hitsz.utils.SignExt
import hitsz.common.HasRegFileParameter
import hitsz.utils.ZeroExt

/* ---------- ---------- csr 控制 ---------- ---------- */

object CSR_op1_sel extends ChiselEnum {
  val csr_op1_X, csr_op1_ZIMM, csr_op1_RS1 = Value
}

/* ---------- ---------- alu 和他的 opN_sel 的控制 ---------- ---------- */

object ALU_op1_sel extends ChiselEnum {
  val alu_op1sel_ZERO, alu_op1sel_RS1, alu_op1sel_PC = Value
}

/** @brief
  *   rs2 输入有 rs2 和 符号拓展立即数
  */
object ALU_op2_sel extends ChiselEnum {
  val alu_op2sel_ZERO, alu_op2sel_IMM, alu_op2sel_RS2 = Value
}

class ALUOPBundle extends Bundle with HasCoreParameter {
  // 控制信号
  val calc    = Output(ALUOpType())
  val op1_sel = Output(ALU_op1_sel())
  val op2_sel = Output(ALU_op2_sel())
}

/* ---------- ---------- 控制信号线 ---------- ---------- */

class WBBundle extends Bundle {
  val sel = Output(WB_sel())
  val wen = Output(Bool())
}

/** @brief
  *   解码，会生成一排控制信号。然后也会进行符号拓展操作, 输出寄存器的编号
  */
class CU extends Module with HasCoreParameter with HasRegFileParameter {
  val io = IO(new Bundle {
    val inst     = Input(UInt(XLEN.W)) // input
    val alu_ctrl = new ALUOPBundle
    val bru_op   = Output(BRUOpType())
    val jmp_op   = Output(JMPOpType())
    val wb       = new WBBundle
    val mem      = Output(MemUOpType())
    val imm      = Output(UInt(XLEN.W))
  })

  /* ---------- default ---------- */

  // alu
  io.alu_ctrl.calc    := ALUOpType.alu_X
  io.alu_ctrl.op1_sel := ALU_op1_sel.alu_op1sel_ZERO
  io.alu_ctrl.op2_sel := ALU_op2_sel.alu_op2sel_ZERO

  io.imm := 0.U

  /* ---------- store ---------- */

  io.mem    := MemUOpType.mem_X
  io.wb.sel := WB_sel.wbsel_X
  io.wb.wen := false.B

  // sw rs2, offset(rs1) 存的是 rs2, 但是计算的是 op1=rs1 和 op2=offset
  private def store_inst(op: MemUOpType.Type) = {
    io.alu_ctrl.calc    := ALUOpType.alu_ADD // rs1 + sext(offset)
    io.alu_ctrl.op1_sel := ALU_op1_sel.alu_op1sel_RS1 // rs1
    io.alu_ctrl.op2_sel := ALU_op2_sel.alu_op2sel_IMM // offset
    io.mem              := op
    io.imm              := SignExt(io.inst(31, 25) ## io.inst(11, 7))
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
    io.alu_ctrl.calc    := ALUOpType.alu_ADD // rs1 + sext(offset)
    io.alu_ctrl.op1_sel := ALU_op1_sel.alu_op1sel_RS1 // rs1
    io.alu_ctrl.op2_sel := ALU_op2_sel.alu_op2sel_IMM // offset
    io.imm              := SignExt(io.inst(31, 20))
    io.mem              := op
    io.wb.sel           := WB_sel.wbsel_MEM
    io.wb.wen           := true.B
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
    io.alu_ctrl.calc    := op
    io.alu_ctrl.op1_sel := ALU_op1_sel.alu_op1sel_RS1
    io.alu_ctrl.op2_sel := ALU_op2_sel.alu_op2sel_RS2
    io.wb.sel           := WB_sel.wbsel_ALU
    io.wb.wen           := true.B
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

  private def Imm_inst(op: ALUOpType.Type) = {
    io.alu_ctrl.calc    := op
    io.alu_ctrl.op1_sel := ALU_op1_sel.alu_op1sel_RS1
    io.alu_ctrl.op2_sel := ALU_op2_sel.alu_op2sel_IMM
    io.imm              := SignExt(io.inst(31, 20))
    io.wb.sel           := WB_sel.wbsel_ALU
    io.wb.wen           := true.B
  }
  when(io.inst === Instructions.ADDI) {
    Imm_inst(ALUOpType.alu_ADD)
  }
  when(io.inst === Instructions.ANDI) {
    Imm_inst(ALUOpType.alu_AND)
  }
  when(io.inst === Instructions.ORI) {
    Imm_inst(ALUOpType.alu_OR)
  }
  when(io.inst === Instructions.XORI) {
    Imm_inst(ALUOpType.alu_XOR)
  }
  when(io.inst === Instructions.SLLI) {
    Imm_inst(ALUOpType.alu_SLL)
  }
  when(io.inst === Instructions.SRLI) {
    Imm_inst(ALUOpType.alu_SRL)
  }
  when(io.inst === Instructions.SRAI) {
    Imm_inst(ALUOpType.alu_SRA)
  }
  when(io.inst === Instructions.SLTI) {
    Imm_inst(ALUOpType.alu_SLT)
  }
  when(io.inst === Instructions.SLTIU) {
    Imm_inst(ALUOpType.alu_SLTU)
  }

  /* ---------- Branch ---------- */

  io.bru_op := BRUOpType.bru_X

  // branch 指令, 我让先直接传到 branch 中, 然后 branch 再与 if_ 联系
  private def Branch_inst(op: BRUOpType.Type) = {
    io.bru_op := op
    io.imm    := SignExt(io.inst(31) ## io.inst(7) ## io.inst(30, 25) ## io.inst(11, 8) ## 0.U(1.W))
  }

  when(io.inst === Instructions.BEQ) {
    Branch_inst(BRUOpType.bru_BEQ)
  }
  when(io.inst === Instructions.BNE) {
    Branch_inst(BRUOpType.bru_BNE)
  }
  when(io.inst === Instructions.BGE) {
    Branch_inst(BRUOpType.bru_BGE)
  }
  when(io.inst === Instructions.BGEU) {
    Branch_inst(BRUOpType.bru_BGEU)
  }
  when(io.inst === Instructions.BLT) {
    Branch_inst(BRUOpType.bru_BLT)
  }
  when(io.inst === Instructions.BLTU) {
    Branch_inst(BRUOpType.bru_BLTU)
  }

  /* ---------- JALR ---------- */

  // jmp
  io.jmp_op := JMPOpType.jmp_X

  when(io.inst === Instructions.JALR) {
    io.imm := SignExt(io.inst(31, 20))
    // io.ctrl.npc_op := NPCOpType.npc_JALR
    io.jmp_op := JMPOpType.jmp_JALR
    io.wb.sel := WB_sel.wbsel_PC4
    io.wb.wen := true.B
  }

  /* ---------- JAL ---------- */

  when(io.inst === Instructions.JAL) {
    io.imm := SignExt(io.inst(31) /* 20 */ ## io.inst(19, 12) /* 19:12 */ ## io.inst(20) /* 11 */ ## io.inst(30, 21) /* 10:1 */ ## 0.U(1.W) /* 0 */ )
    // io.ctrl.npc_op := NPCOpType.npc_JAL
    io.jmp_op := JMPOpType.jmp_JAL
    io.wb.sel := WB_sel.wbsel_PC4
    io.wb.wen := true.B
    // JALlog(
    //   SignExt(io.inst(31) /* 20 */ ## io.inst(19, 12) /* 19:12 */ ## io.inst(20) /* 11 */ ## io.inst(30, 21) /* 10:1 */ ## 0.U(1.W) /* 0 */ ),
    //   NPCOpType.npc_JAL,
    //   WB_sel.wbsel_PC4,
    //   io.inst(11, 7)
    // )
  }

  /* ---------- LUI ---------- */

  when(io.inst === Instructions.LUI) {
    io.alu_ctrl.calc    := ALUOpType.alu_ADD
    io.alu_ctrl.op1_sel := ALU_op1_sel.alu_op1sel_ZERO /* 就是啥也不干 */
    io.alu_ctrl.op2_sel := ALU_op2_sel.alu_op2sel_IMM
    io.imm              := io.inst(31, 12) ## 0.U(12.W) /* 当然这个移位步骤可以移到 ALU(EXE-stage) */
    io.wb.sel           := WB_sel.wbsel_ALU
    io.wb.wen           := true.B
  }

  /* ---------- AUIPC ---------- */

  when(io.inst === Instructions.AUIPC) {
    io.alu_ctrl.calc    := ALUOpType.alu_ADD
    io.alu_ctrl.op1_sel := ALU_op1_sel.alu_op1sel_PC
    io.alu_ctrl.op2_sel := ALU_op2_sel.alu_op2sel_IMM
    io.imm              := io.inst(31, 12) ## 0.U(12.W)
    io.wb.sel           := WB_sel.wbsel_ALU
    io.wb.wen           := true.B
  }

  // /* ---------- CSR ---------- */

  // io.ctrl.csr.calc    := CSRUOpType.csru_X
  // io.ctrl.csr.op1_sel := CSR_op1_sel.csr_op1_X
  // io.ctrl.csr.csr_reg := 0.U

  // // csrrw rd, csr, rs1 => t=csr; csr=rs1; rd=t
  // def CSR_inst(op: CSRUOpType.Type) = {
  //   io.ctrl.csr.calc    := op
  //   io.ctrl.csr.op1_sel := CSR_op1_sel.csr_op1_RS1
  //   io.ctrl.csr.csr_reg := io.inst(31, 20)
  // }
  // when(io.inst === Instructions.CSRRW) {
  //   CSR_inst(CSRUOpType.csru_CSRRW)
  // }
  // when(io.inst === Instructions.CSRRS) {
  //   CSR_inst(CSRUOpType.csru_CSRRS)
  // }
  // when(io.inst === Instructions.CSRRC) {
  //   CSR_inst(CSRUOpType.csru_CSRRC)
  // }

  // /* ---------- CSRI ---------- */

  // def CSRI_inst(op: CSRUOpType.Type) = {
  //   io.ctrl.csr.calc    := op
  //   io.ctrl.csr.op1_sel := CSR_op1_sel.csr_op1_ZIMM
  //   io.ctrl.csr.csr_reg := io.inst(31, 20)
  //   io.imm              := ZeroExt(io.inst(19, 15))
  // }
  // when(io.inst === Instructions.CSRRWI) {
  //   CSRI_inst(CSRUOpType.csru_CSRRW)
  // }
  // when(io.inst === Instructions.CSRRSI) {
  //   CSRI_inst(CSRUOpType.csru_CSRRS)
  // }
  // when(io.inst === Instructions.CSRRCI) {
  //   CSRI_inst(CSRUOpType.csru_CSRRC)
  // }

  // val csignals /* : List */ = ListLookup(
  //   io.inst,
  //   List(ALUOpType.alu_X, OP1_sel.op1sel_ZERO, OP2_sel.op2sel_ZERO, MemUOpType.mem_X, CSRUOpType.csru_X, WB_sel.wbsel_X, NPCOpType.npc_X, BRUOpType.bru_X), // default
  //   Array(
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

object CUTest extends App {
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new Module {
      val io = IO(new Bundle {
        val inst    = Input(UInt(32.W))
        val is_addi = Output(Bool())
      })
      io.is_addi := false.B
      when(io.inst === Instructions.ADDI) {
        io.is_addi := true.B
      }
    },
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
