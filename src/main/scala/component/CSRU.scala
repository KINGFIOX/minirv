/** @brief
  *   放到 EXE 阶段
  */

package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter
import utils.MaskedRegMap
import utils.ZeroExt
import utils.GenMask

/* ---------- ---------- 规定一些常量 ---------- ---------- */

/** @brief
  *   有几个 CSR 寄存器
  */
trait HasCSRRegFileParameter {
  val NCSRbits = 12
  val NCSRReg  = (1 << NCSRbits)
}

/** @brief
  *   暂时只支持 ecall
  */
trait HasExceptionNO {
  // def instrAddrMisaligned = 0
  // def instrAccessFault    = 1
  // def illegalInstr        = 2
  // def breakPoint          = 3
  // def loadAddrMisaligned  = 4
  // def loadAccessFault     = 5
  // def storeAddrMisaligned = 6
  // def storeAccessFault    = 7
  def ecallU = 8
  // def ecallS              = 9
  def ecallM = 11
  // def instrPageFault = 12
  // def loadPageFault  = 13
  // def storePageFault = 15

  val ExcPriority = Seq(
    // breakPoint, // TODO: different BP has different priority
    // instrPageFault,
    // instrAccessFault,
    // illegalInstr,
    // instrAddrMisaligned,
    ecallM,
    // ecallS,
    ecallU
    // storeAddrMisaligned,
    // loadAddrMisaligned,
    // storePageFault,
    // loadPageFault,
    // storeAccessFault,
    // loadAccessFault
  )
}

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

/* ---------- ---------- csr ---------- ---------- */

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

  val mstatusWMask = (~ZeroExt(
    (
      GenMask(XLEN - 1) | // SD is read-only
        GenMask(XLEN - 2, 23) | // res.
        GenMask(16, 15) | // XS is read-only
        GenMask(6) | // res.
        GenMask(4) | // WPRI
        GenMask(2) | // WPRI
        GenMask(0) // WPRI
    )
  )).asUInt

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

/* ---------- ---------- 一些辅助的数据结构 ---------- ---------- */

private class MstatusStruct extends Bundle with HasCoreParameter with HasCSRRegFileParameter {
  val sd = Output(UInt(1.W))

  val pad1 = if (XLEN == 64) Output(UInt(27.W)) else null
  val sxl  = if (XLEN == 64) Output(UInt(2.W)) else null
  val uxl  = if (XLEN == 64) Output(UInt(2.W)) else null
  val pad0 = if (XLEN == 64) Output(UInt(9.W)) else Output(UInt(8.W))

  val tsr  = Output(UInt(1.W))
  val tw   = Output(UInt(1.W))
  val tvm  = Output(UInt(1.W))
  val mxr  = Output(UInt(1.W))
  val sum  = Output(UInt(1.W))
  val mprv = Output(UInt(1.W))
  val xs   = Output(UInt(2.W))
  val fs   = Output(UInt(2.W))
  val mpp  = Output(UInt(2.W))
  val hpp  = Output(UInt(2.W))
  val spp  = Output(UInt(1.W))
  val pie  = new Priv
  val ie   = new Priv
}

/** @brief
  *   特权级别，只能有 1 个是 1
  */
private class Priv extends Bundle {
  val m = Output(Bool())
  val h = Output(Bool())
  val s = Output(Bool())
  val u = Output(Bool())
}
