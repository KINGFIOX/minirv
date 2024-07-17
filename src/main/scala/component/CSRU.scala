/** @brief
  *   放到 EXE 阶段
  */

package component

import chisel3._
import chisel3.util._
import common.HasCoreParameter

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
    *   在本课程中，只需考虑M模式的外部中断和系统调用两种情况，即MCAUSE的有效值要么是32'h8000000b，要么是32'h0000000b。
    */

  val MCAUSE = 0x342
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

  // 注意, 我们这里的 cpu 没有抵御风险的能力, 如果出现了非法访问的情况就直接挂掉, 只能按下复位键

  val mstatus = RegInit(0.U(XLEN.W)) // 机器模式的状态寄存器

}
