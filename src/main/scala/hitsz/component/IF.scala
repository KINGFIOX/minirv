/** @brief
  *   其实这个东西应该是: NPC + PC
  */

package hitsz.component

import chisel3._
import chisel3.util._
import hitsz.common.HasCoreParameter
import hitsz.common.HasECALLParameter
import hitsz.io.verilator.InstROMBundle

/** @brief
  *   IF -> ID 之间连接的线
  */
class IF2IDBundle extends Bundle with HasCoreParameter {
  val inst  = Output(UInt(XLEN.W))
  val pc    = Output(UInt(XLEN.W))
  val valid = Output(Bool())
}

/** @brief
  *   这个记录的是: 无条件跳转
  */
object JMPOpType extends ChiselEnum {
  val jmp_X, jmp_JAL, jmp_JALR, jmp_ECALL, jmp_ERET = Value
}

/** @brief
  *   IF 阶段的输入信号
  */
class IFBundle extends Bundle with HasCoreParameter {
  val imm     = Input(UInt(XLEN.W)) // br/jal 是 offset
  val br_flag = Input(Bool())
  val rs1_v   = Input(UInt(XLEN.W)) // jalr 就是绝对地址
}

class JMPBundle extends Bundle with HasCoreParameter {
  val op    = Output(JMPOpType())
  val imm   = Output(UInt(XLEN.W))
  val pc    = Output(UInt(XLEN.W))
  val rs1_v = Output(UInt(XLEN.W))
  val valid = Output(Bool())
}

object JMPBundle {
  def apply(op: JMPOpType.Type, imm: UInt, pc: UInt, rs1_v: UInt, valid: Bool): JMPBundle = {
    val jmp = Wire(Flipped(new JMPBundle))
    jmp.op    := op
    jmp.imm   := imm
    jmp.pc    := pc
    jmp.rs1_v := rs1_v
    jmp.valid := valid
    jmp
  }
}

class BRU2IFBundle extends Bundle with HasCoreParameter {
  val pc      = Output(UInt(XLEN.W))
  val imm     = Output(UInt(XLEN.W))
  val br_flag = Output(Bool())
  val valid   = Output(Bool())
}

class IF extends Module with HasCoreParameter with HasECALLParameter {
  val io = IO(new Bundle {
    val irom = Flipped(new InstROMBundle)
    val out  = new IF2IDBundle // 取指令输出, pc4 输出
    // val in   = new IFBundle
    val jmp = Flipped(new JMPBundle)
    val br  = Flipped(new BRU2IFBundle)
    val ld_hazard = new Bundle {
      val happened = Input(Bool())
      val valid    = Input(Bool())
      val id_pc    = Input(UInt(XLEN.W))
    }
    // val debug =
  })

  /* ---------- pc_cur ---------- */

  private val pc = RegInit(UInt(XLEN.W), 0.U) // pc = 0

  io.irom.addr := pc

  io.out.inst := io.irom.inst

  io.out.pc := pc

  io.out.valid := true.B // 默认是 true

  when(io.jmp.op =/= JMPOpType.jmp_X && io.jmp.valid) {
    io.out.valid := false.B
    io.out.inst  := NOP.U
    // printf()
  }

  when(io.br.br_flag && io.br.valid) {
    io.out.valid := false.B
    io.out.inst  := NOP.U
  }

  when(io.ld_hazard.happened && io.ld_hazard.valid) {
    io.out.valid := true.B
    io.irom.addr := io.ld_hazard.id_pc
    io.out.pc    := io.ld_hazard.id_pc
  }

  /* ---------- pc_next ---------- */

  private val npc = MuxCase(
    pc + 4.U,
    Seq(
      // 控制冒险
      (io.br.br_flag && io.br.valid, io.br.pc + io.br.imm),
      (io.jmp.op === JMPOpType.jmp_JAL && io.jmp.valid, io.jmp.pc + io.jmp.imm),
      (io.jmp.op === JMPOpType.jmp_JALR && io.jmp.valid, (io.jmp.rs1_v + io.jmp.imm) & ~1.U(XLEN.W)),
      (io.jmp.op === JMPOpType.jmp_ECALL && io.jmp.valid, ECALL_ADDRESS.U),
      // TODO ERET
      // 数据冒险
      (io.ld_hazard.happened && io.ld_hazard.valid, pc)
    )
  )

  pc := npc

}

object IF extends App {
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new IF,
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
