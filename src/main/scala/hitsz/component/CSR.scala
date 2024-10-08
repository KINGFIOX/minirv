package hitsz.component

import chisel3._
import chisel3.util._
import hitsz.common.HasCoreParameter

/* ---------- ---------- 规定一些常量 ---------- ---------- */

/** @brief
  *   有几个 CSR 寄存器
  */
trait HasCSRRegFileParameter {
  val NCSRbits = 12
  val NCSRReg  = (1 << NCSRbits)
}

/** @brief
  *   * 下面这些是 MRW 的, 目前只实现这三个
  */
trait HasCSRConst {

  val MSTATUS = 0x300

  /** @brief
    */

  val MEPC = 0x341

  /** @brief
    *   在本课程中，只需考虑M模式的外部中断和系统调用两种情况，即MCAUSE的有效值要么是 32'h8000000b，要么是32'h0000000b。
    */

  val MCAUSE = 0x342

}

object MCAUSE_CONSTS {
  val ECALL = 0x0000_000b
  val INT   = 0x8000_000b
}

/* ---------- ---------- csr ALU ---------- ---------- */

object CSR_op1_sel extends ChiselEnum {
  val csr_op1_X, csr_op1_ZIMM, csr_op1_RS1 = Value
}

/** @brief
  *   csr alu bundle
  */
class CSRALUBundle extends Bundle with HasCSRRegFileParameter {
  val calc    = Output(CSRALUOpType()) // 控制 计算器
  val op1_sel = Output(CSR_op1_sel())
  val csr_i   = Output(UInt(NCSRbits.W)) // 第 csr_i 个 csr 寄存器
}

object CSRALUOpType extends ChiselEnum {
  val csru_X, csru_CSRRW, csru_CSRRS, csru_CSRRC = Value
}

/** @brief
  *   wb 阶段, 应该对 csr 怎么操作
  */
object CSRWbStage extends ChiselEnum {
  val csr_wb_X, csr_wb_ALU, csr_wb_ECALL = Value
}

/* ---------- ---------- csr 寄存器 ---------- ---------- */

class CSRRegFile extends HasCoreParameter with HasCSRRegFileParameter with HasCSRConst {

  /* ---------- 定义一些数据结构 ---------- */

  private val mstatus = RegInit(0x0000_0008.U(XLEN.W)) // 默认为开启中断
  private val mcause  = RegInit(0.U(XLEN.W)) // 0x8000_000b 或 0x0
  private val mepc    = RegInit(0.U(XLEN.W))

  def read(csr_i: UInt): UInt = {
    printf("accessing csr=%x\n", csr_i)
    // 对应的 csr 寄存器
    val csr = MuxCase(
      0.U, // ERROR! 出现错误
      Seq(
        (csr_i === MSTATUS.asUInt) -> mstatus,
        (csr_i === MCAUSE.asUInt)  -> mcause,
        (csr_i === MEPC.asUInt)    -> mepc
      )
    )
    csr
  }

  def write(csr_i: UInt, wdata: UInt) = {
    printf("accessing csr=%x\n", csr_i)
    switch(csr_i) {
      is(MSTATUS.asUInt) {
        val mstatusMask = 0x0000_0008.U(XLEN.W) // 只有 mie(中断开关) , 才能写入
        mstatus := (mstatus & ~mstatusMask) | (wdata & mstatusMask)
      }
      is(MCAUSE.asUInt) {
        mcause := wdata
      }
      is(MEPC.asUInt) {
        mepc := wdata
      }
    }
  }

}
