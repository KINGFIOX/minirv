/** @brief
  *   放到 EXE 阶段
  */

package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter
import common.HasCSRRegFileParameter
import utils.MaskedRegMap

trait HasCSRConst {
  // Machine Information Registers
  val Mvendorid = 0xf11
  val Marchid   = 0xf12
  val Mimpid    = 0xf13
  val Mhartid   = 0xf14

  // Machine Trap Setup
  val Mstatus    = 0x300
  val Misa       = 0x301
  val Medeleg    = 0x302
  val Mideleg    = 0x303
  val Mie        = 0x304
  val Mtvec      = 0x305
  val Mcounteren = 0x306

  // Machine Trap Handling
  val Mscratch = 0x340
  val Mepc     = 0x341
  val Mcause   = 0x342
  val Mtval    = 0x343
  val Mip      = 0x344

  // Machine Memory Protection
  // TBD
  val Pmpcfg0     = 0x3a0
  val Pmpcfg1     = 0x3a1
  val Pmpcfg2     = 0x3a2
  val Pmpcfg3     = 0x3a3
  val PmpaddrBase = 0x3b0
}

object CSRUOpType extends ChiselEnum {
  val csru_X, csru_CSRRW, csru_CSRRS, csru_CSRRC /* TODO ecall */ = Value
}

/** @brief
  *   用于 CSR 操作的模块
  */
class CSRU extends Module with HasCoreParameter with HasCSRRegFileParameter with HasCSRConst {
  val io = IO(new Bundle {
    val op  = Input(CSRUOpType())
    val csr = Input(UInt(NCSRbits.W))
    val rs1 = Input(UInt(XLEN.W)) // Mux(控制信号 , zimm, rs1)
    val out = Output(UInt(XLEN.W)) // 读取出来 CSR 的值, rd 由 controller 控制
  })

  /* ---------- 初始化 csr 寄存器堆 ---------- */

  val csr_regfile = Mem(NCSRReg, UInt(XLEN.W))

  val mapping = Map(
    // Machine Information Registers
    MaskedRegMap(Mvendorid, csr_regfile(Mvendorid), 0.U, MaskedRegMap.Unwritable),
    MaskedRegMap(Marchid, csr_regfile(Marchid), 0.U, MaskedRegMap.Unwritable),
    MaskedRegMap(Mimpid, csr_regfile(Mimpid), 0.U, MaskedRegMap.Unwritable),
    MaskedRegMap(Mhartid, csr_regfile(Mhartid), 0.U, MaskedRegMap.Unwritable),

    // Machine Trap Setup
    // MaskedRegMap(Mstatus, csr_regfile(Mstatus), mstatusWMask, mstatusUpdateSideEffect),
    MaskedRegMap(Misa, csr_regfile(Misa)), // now MXL, EXT is not changeable
    MaskedRegMap(Medeleg, csr_regfile(Medeleg), "hbbff".U(64.W)),
    MaskedRegMap(Mideleg, csr_regfile(Mideleg), "h222".U(64.W)),
    MaskedRegMap(Mie, csr_regfile(Mie)),
    MaskedRegMap(Mtvec, csr_regfile(Mtvec)),
    MaskedRegMap(Mcounteren, csr_regfile(Mcounteren))
  )

  io.out := 0.U

  /* ----------  ---------- */

  switch(io.op) {
    is(CSRUOpType.csru_X) {
      io.out := 0.U
    }
    is(CSRUOpType.csru_CSRRW) { // csr read & write
      val t = csr_regfile.read(io.csr)
      csr_regfile.write(io.csr, io.rs1)
      io.out := t
    }
    is(CSRUOpType.csru_CSRRS) {
      val t = csr_regfile.read(io.csr)
      csr_regfile.write(io.csr, t | io.rs1)
      io.out := t
    }
    is(CSRUOpType.csru_CSRRC) {
      val t = csr_regfile.read(io.csr)
      csr_regfile.write(io.csr, t & ~io.rs1)
      io.out := t
    }
  }
}
