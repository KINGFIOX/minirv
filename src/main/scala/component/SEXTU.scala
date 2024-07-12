package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter

object SEXTUOpType extends ChiselEnum {
  val sextu_X, sextu_ALU, sextu_SHIFT, sextu_STORE, sextu_BRANCH, sextu_U, sextu_J = Value
}

class SEXTU extends Module with HasCoreParameter {
  val io = IO(new Bundle {
    val inst = Input(UInt(XLEN.W))
    val op   = Input(SEXTUOpType())
    val ext  = Output(UInt(XLEN.W))
  })

  io.ext := 0.U

  switch(io.op) {
    is(SEXTUOpType.sextu_X) {
      io.ext := 0.U
    }
    is(SEXTUOpType.sextu_ALU) {
      val imm = io.inst(31, 20)
      io.ext := Cat(Fill(20, imm(11)), imm)
    }
    is(SEXTUOpType.sextu_SHIFT) {
      val shamt = io.inst(25, 20) // len = 6
      io.ext := Cat(Fill(26, shamt(5)), shamt)
    }
    is(SEXTUOpType.sextu_STORE) {
      val imm = Cat(io.inst(31, 25), io.inst(11, 7))
      io.ext := Cat(Fill(20, imm(11)), imm)
    }
    is(SEXTUOpType.sextu_BRANCH) {
      val imm = Cat(io.inst(31) /* 12 */, io.inst(7) /* 11 */, io.inst(30, 25) /* 10..5 */, io.inst(11, 8) /* 4..1 */, 0.U(1.W) /* 0 */ )
      io.ext := Cat(Fill(19, imm(12)), imm /* len = 13 */ )
    }
    is(SEXTUOpType.sextu_U) {
      io.ext := Cat(io.inst(31, 12), 0.U(12.W))
    }
    is(SEXTUOpType.sextu_J) {
      val imm = Cat(io.inst(31) /* 20 */, io.inst(19, 12) /* 19..12 */, io.inst(20) /* 11 */, io.inst(30, 21) /* 10..1 */, 0.U(1.W) /* 0 */ )
      io.ext := Cat(imm /* 21 */, 0.U(11.W))
    }
  }

}

object SEXTU extends App {
  val s = _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new SEXTU,
    args = Array("--target-dir", "generated"),
    firtoolOpts = Array(
      "--strip-debug-info",
      "-disable-all-randomization"
    )
  )
}
