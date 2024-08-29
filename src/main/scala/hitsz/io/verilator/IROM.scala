package hitsz.io.verilator

import chisel3._
import chisel3.util._
import hitsz.common.HasCoreParameter
import hitsz.io.HasSocParameter
import chisel3.util.experimental.loadMemoryFromFile

class InstROMBundle extends Bundle with HasCoreParameter {
  val addr = Input(UInt(XLEN.W))
  val inst = Output(UInt(XLEN.W))
}

// /** @brief
//   */
// class IROM(user: String) extends Module with HasSocParameter with HasCoreParameter {
//   val io = IO(new Bundle {
//     val a   = Input(UInt(16.W))
//     val spo = Output(UInt(32.W))
//   })

//   val size   = verilator_dramLens * dataBytes
//   val memory = Mem(size, UInt(8.W))
//   loadMemoryFromFile(memory, user)

//   val addr = io.a << dataBytesBits
//   io.spo := memory(addr + 3.U) ## memory(addr + 2.U) ## memory(addr + 1.U) ## memory(addr)

// }

class IROM extends BlackBox {
  val io = IO(new Bundle {
    val a   = Input(UInt(16.W))
    val spo = Output(UInt(32.W))
  })
}
