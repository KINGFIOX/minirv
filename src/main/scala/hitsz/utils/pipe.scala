package hitsz.utils

import chisel3._
import chisel3.util._
import hitsz.component.CUControlBundle
import hitsz.common.HasCoreParameter

/** 注意: left -> right, 这是单向的
  */
object pipe {
  def apply[T <: Data](left: T, right: T, wen: Bool) = {
    right := RegEnable(left, wen)
  }
}

class ID2EXE extends Bundle with HasCoreParameter {
  val ctrl = new CUControlBundle // 控制线
  val imm  = Output(UInt(XLEN.W)) // 立即数: 正常情况下 SignExt, CSR 的时候 ZeroExt
}

object pipeline extends App {
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new Module {
      val io = IO(new Bundle {
        val a = Flipped(new ID2EXE)
        val b = new ID2EXE
      })
      pipe(io.a, io.b, true.B)
    },
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
