package io.blackbox

import chisel3._
import chisel3.util._
import common.HasCoreParameter

class InstROMBundle extends Bundle with HasCoreParameter {
  val addr = Input(UInt(XLEN.W))
  val inst = Output(UInt(XLEN.W))
}

/** @brief
  */
class IROM extends BlackBox {
  val io = IO(new Bundle {
    val a   = Input(UInt(16.W))
    val spo = Output(UInt(32.W))
  })
}
