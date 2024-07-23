package hitsz.io.trace

import chisel3._
import chisel3.util._
import hitsz.common.HasCoreParameter
import chisel3.util.experimental.loadMemoryFromFile
import hitsz.io.HasSocParameter
import firrtl.annotations.MemoryLoadFileType
import chisel3.util.experimental.loadMemoryFromFileInline

class InstROMBundle extends Bundle with HasCoreParameter {
  val addr = Input(UInt(XLEN.W))
  val inst = Output(UInt(XLEN.W))
}

/** @brief
  */
class IROM(user_path: String, kernel_path: String, ty: MemoryLoadFileType = MemoryLoadFileType.Hex) extends Module with HasCoreParameter with HasSocParameter {
  val io = IO(new Bundle {
    val a   = Input(UInt(XLEN.W))
    val spo = Output(UInt(XLEN.W))
  })

  /* ---------- user ---------- */

  val user = Mem((1 << addrBits_verilator) >> dataBytesBits, UInt(XLEN.W))
  loadMemoryFromFileInline(user, user_path, ty)

  /* ---------- kernel ---------- */

  val kernel = Mem((1 << addrBits_verilator) >> dataBytesBits, UInt(XLEN.W))
  loadMemoryFromFileInline(kernel, kernel_path, ty)

  /* ---------- output ---------- */

  val u_index = io.a >> dataBytesBits

  val k_index = (io.a >> dataBytesBits) - KERNEL_BEGIN.U

  io.spo := MuxCase(
    0.U,
    Seq(
      (io.a < KERNEL_BEGIN.U)  -> user(u_index),
      (KERNEL_BEGIN.U <= io.a) -> kernel(k_index)
    )
  )

}
