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
import Log.JALlog
import utils.ZeroExt

/* ---------- ---------- csr 控制 ---------- ---------- */

object CSR_op1_sel extends ChiselEnum {
  val csr_op1_X, csr_op1_ZIMM, csr_op1_RS1 = Value
}

class CSRUBundle extends Bundle with HasCSRRegFileParameter {
  val calc    = Output(CSRUOpType()) // 控制器
  val op1_sel = Output(CSR_op1_sel())
  val csr_reg = Output(UInt(NCSRbits.W)) // 这个就是 csr
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

class ALUOPBundle extends Bundle {
  // 控制信号
  val calc = Output(ALUOpType())
  val op1  = Output(ALU_op1_sel())
  val op2  = Output(ALU_op2_sel())
}

/* ---------- ---------- 控制信号线 ---------- ---------- */

class CUControlBundle extends Bundle {
  // 控制信号
  val alu    = new ALUOPBundle
  val op_mem = Output(MemUOpType())
  val wb_sel = Output(WB_sel())
  val npc_op = Output(NPCOpType())
  val bru_op = Output(BRUOpType())
  val csr    = new CSRUBundle
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
  io.ctrl.alu.calc := ALUOpType.alu_X
  io.ctrl.alu.op1  := ALU_op1_sel.alu_op1sel_ZERO
  io.ctrl.alu.op2  := ALU_op2_sel.alu_op2sel_ZERO
  io.ctrl.op_mem   := MemUOpType.mem_X
  io.ctrl.wb_sel   := WB_sel.wbsel_X
  io.ctrl.bru_op   := BRUOpType.bru_X

  // 立即数
  io.imm := 0.U

  // 寄存器
  io.rf.rs1_i := io.inst(19, 15)
  io.rf.rs2_i := io.inst(24, 20)
  io.rf.rd_i  := 0.U /* 默认是不写入 */

  /* ---------- store ---------- */

  // sw rs2, offset(rs1) 存的是 rs2, 但是计算的是 op1=rs1 和 op2=offset
  private def store_inst(op: MemUOpType.Type) = {
    io.ctrl.alu.calc := ALUOpType.alu_ADD // rs1 + sext(offset)
    io.ctrl.alu.op1  := ALU_op1_sel.alu_op1sel_RS1 // rs1
    io.ctrl.alu.op2  := ALU_op2_sel.alu_op2sel_IMM // offset
    io.ctrl.op_mem   := op
    io.imm           := SignExt(io.inst(31, 25) ## io.inst(11, 7))
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
    io.ctrl.alu.calc := ALUOpType.alu_ADD // rs1 + sext(offset)
    io.ctrl.alu.op1  := ALU_op1_sel.alu_op1sel_RS1 // rs1
    io.ctrl.alu.op2  := ALU_op2_sel.alu_op2sel_IMM // offset
    io.imm           := SignExt(io.inst(31, 20))
    io.ctrl.op_mem   := op
    io.ctrl.wb_sel   := WB_sel.wbsel_MEM
    io.rf.rd_i       := io.inst(11, 7)
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
    io.ctrl.alu.calc := op
    io.ctrl.alu.op1  := ALU_op1_sel.alu_op1sel_RS1
    io.ctrl.alu.op2  := ALU_op2_sel.alu_op2sel_RS2
    io.ctrl.wb_sel   := WB_sel.wbsel_ALU
    io.rf.rd_i       := io.inst(11, 7)
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
    io.ctrl.alu.calc := op
    io.ctrl.alu.op1  := ALU_op1_sel.alu_op1sel_RS1
    io.ctrl.alu.op2  := ALU_op2_sel.alu_op2sel_IMM
    io.imm           := SignExt(io.inst(31, 20))
    io.rf.rd_i       := io.inst(11, 7)
    io.ctrl.wb_sel   := WB_sel.wbsel_ALU
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

  private def Branch_inst(op: BRUOpType.Type) = {
    io.ctrl.bru_op := op
    io.imm         := SignExt(io.inst(31) ## io.inst(7) ## io.inst(30, 25) ## io.inst(11, 8) ## 0.U(1.W))
    io.ctrl.npc_op := NPCOpType.npc_BR
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

  when(io.inst === Instructions.JALR) {
    io.imm         := SignExt(io.inst(31, 20))
    io.ctrl.npc_op := NPCOpType.npc_JALR
    io.ctrl.wb_sel := WB_sel.wbsel_PC4
    io.rf.rd_i     := io.inst(11, 7)
  }

  /* ---------- JAL ---------- */

  when(io.inst === Instructions.JAL) {
    io.imm         := SignExt(io.inst(31) /* 20 */ ## io.inst(19, 12) /* 19:12 */ ## io.inst(20) /* 11 */ ## io.inst(30, 21) /* 10:1 */ ## 0.U(1.W) /* 0 */ )
    io.ctrl.npc_op := NPCOpType.npc_JAL
    io.ctrl.wb_sel := WB_sel.wbsel_PC4
    io.rf.rd_i     := io.inst(11, 7)
    // JALlog(
    //   SignExt(io.inst(31) /* 20 */ ## io.inst(19, 12) /* 19:12 */ ## io.inst(20) /* 11 */ ## io.inst(30, 21) /* 10:1 */ ## 0.U(1.W) /* 0 */ ),
    //   NPCOpType.npc_JAL,
    //   WB_sel.wbsel_PC4,
    //   io.inst(11, 7)
    // )
  }

  /* ---------- LUI ---------- */

  when(io.inst === Instructions.LUI) {
    io.ctrl.alu.calc := ALUOpType.alu_ADD
    io.ctrl.alu.op1  := ALU_op1_sel.alu_op1sel_ZERO /* 就是啥也不干 */
    io.ctrl.alu.op2  := ALU_op2_sel.alu_op2sel_IMM
    io.imm           := io.inst(31, 12) ## 0.U(12.W) /* 当然这个移位步骤可以移到 ALU(EXE-stage) */
    io.rf.rd_i       := io.inst(11, 7)
    io.ctrl.wb_sel   := WB_sel.wbsel_ALU
  }

  /* ---------- AUIPC ---------- */

  when(io.inst === Instructions.AUIPC) {
    io.ctrl.alu.calc := ALUOpType.alu_ADD
    io.ctrl.alu.op1  := ALU_op1_sel.alu_op1sel_PC
    io.ctrl.alu.op2  := ALU_op2_sel.alu_op2sel_IMM
    io.imm           := io.inst(31, 12) ## 0.U(12.W)
    io.rf.rd_i       := io.inst(11, 7)
    io.ctrl.wb_sel   := WB_sel.wbsel_ALU
  }

  /* ---------- CSR ---------- */

  io.ctrl.csr.calc    := CSRUOpType.csru_X
  io.ctrl.csr.op1_sel := CSR_op1_sel.csr_op1_X
  io.ctrl.csr.csr_reg := 0.U

  // csrrw rd, csr, rs1 => t=csr; csr=rs1; rd=t
  def CSR_inst(op: CSRUOpType.Type) = {
    io.ctrl.csr.calc    := op
    io.ctrl.csr.op1_sel := CSR_op1_sel.csr_op1_RS1
    io.ctrl.csr.csr_reg := io.inst(31, 20)
    io.rf.rd_i          := io.inst(11, 7)
    io.ctrl.wb_sel      := WB_sel.wbsel_CSR
  }
  when(io.inst === Instructions.CSRRW) {
    CSR_inst(CSRUOpType.csru_CSRRW)
  }
  when(io.inst === Instructions.CSRRS) {
    CSR_inst(CSRUOpType.csru_CSRRS)
  }
  when(io.inst === Instructions.CSRRC) {
    CSR_inst(CSRUOpType.csru_CSRRC)
  }

  /* ---------- CSRI ---------- */

  def CSRI_inst(op: CSRUOpType.Type) = {
    io.ctrl.csr.calc    := op
    io.ctrl.csr.op1_sel := CSR_op1_sel.csr_op1_ZIMM
    io.ctrl.csr.csr_reg := io.inst(31, 20)
    io.imm              := ZeroExt(io.inst(19, 15))
    io.rf.rd_i          := io.inst(11, 7)
    io.ctrl.wb_sel      := WB_sel.wbsel_CSR
  }
  when(io.inst === Instructions.CSRRWI) {
    CSRI_inst(CSRUOpType.csru_CSRRW)
  }
  when(io.inst === Instructions.CSRRSI) {
    CSRI_inst(CSRUOpType.csru_CSRRS)
  }
  when(io.inst === Instructions.CSRRCI) {
    CSRI_inst(CSRUOpType.csru_CSRRC)
  }

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
