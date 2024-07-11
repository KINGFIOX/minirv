package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter

object SEXTUOpType extends ChiselEnum {
  val sextu_X, sextu_NORMAL, sextu_SHIFT, sextu_STORE, sextu_BRANCH, sextu_U, sextu_J = Value
}

class SEXTU extends Module with HasCoreParameter {
  val io = IO(new Bundle {
    val inst = Input(UInt(XLEN.W))
    val op   = Input(SEXTUOpType())
    val ext  = Output(UInt(XLEN.W))
  })

  // TODO
}
