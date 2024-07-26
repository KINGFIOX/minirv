package hitsz.pipeline

import chisel3._
import chisel3.util._
import hitsz.common.HasCoreParameter
import hitsz.common.HasRegFileParameter
import hitsz.component.RFRead
import hitsz.component.CUControlBundle

/** @brief
  *   这些都是输出
  */
class ID2EXEBundle extends Bundle with HasCoreParameter with HasRegFileParameter {
  val rf = new Bundle {
    val read = Output(new RFRead)
    val write = new Bundle {
      val rd_i = UInt(NRRegbits.W)
      // val wen  = Bool() // 这个 wen 信号现在放在了 cu.ctrl.wb_wen 中
    }
  }
  val cu = new Bundle {
    val ctrl = new CUControlBundle // 控制线
    val imm  = UInt(XLEN.W) // 立即数: 正常情况下 SignExt, CSR 的时候 ZeroExt
  }
  val pc = UInt(XLEN.W) // 当前指令的地址
}

object ID2EXEBundle {
  def apply(ctrl: CUControlBundle, imm: UInt, inst: UInt, rf_read_rs1_v: UInt, rf_read_rs2_v: UInt, pc: UInt): ID2EXEBundle = {
    val id2exe = Wire(Flipped(new ID2EXEBundle))
    id2exe.cu.ctrl       := ctrl
    id2exe.cu.imm        := imm
    id2exe.rf.write.rd_i := inst(11, 7)
    id2exe.rf.read.rs1_i := inst(19, 15)
    id2exe.rf.read.rs2_i := inst(24, 20)
    id2exe.rf.read.rs1_v := rf_read_rs1_v
    id2exe.rf.read.rs2_v := rf_read_rs2_v
    id2exe.pc            := pc
    id2exe
  }
}
