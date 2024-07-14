package utils

import chisel3._
import chisel3.util._
import common.HasCoreParameter

object MaskedRegMap extends HasCoreParameter {
  /* ---------- 定义常量 ---------- */
  def Unwritable                 = null
  def NoSideEffect: UInt => UInt = (x => x) // 没有副作用: 恒等变换
  def WritableMask               = Fill(XLEN, true.B) /* 可以写的掩码, 全部是 1, 表示: 该 csr 是可以写的 */
  def UnwritableMask             = 0.U(XLEN.W) /* 不可写: 0.U */

  /* ----------  ---------- */

  /** @brief
    *   生成一个 entry
    *
    * @param key
    *   addr
    * @param value
    *   reg wmask wfn rmask
    */
  def apply(addr: Int, reg: UInt, wmask: UInt = WritableMask, wfn: UInt => UInt = (x => x), rmask: UInt = WritableMask) = (addr, (reg, wmask, wfn, rmask))

  /** @brief
    *   返回一根导线, 这个导线
    */
  def isIllegalAddr(mapping: Map[Int, (UInt, UInt, UInt => UInt, UInt)], addr: UInt): Bool = {
    val illegalAddr   = Wire(Bool())
    val chiselMapping = mapping.map { case (a, (r, wm, w, rm)) => (a.U, r, wm, w, rm) } // 展平 map
    illegalAddr := LookupTreeDefault(addr, true.B, chiselMapping.map { case (a, r, wm, w, rm) => (a, false.B) }) // 如果没找着: is_illegal_addr = true
    illegalAddr
  }

  /** @param mapping
    * @param raddr
    * @param rdata
    *   这个应该是一个 Output
    * @param waddr
    * @param wen
    * @param wdata
    */
  def generate(mapping: Map[Int, (UInt, UInt, UInt => UInt, UInt)], raddr: UInt, rdata: UInt, waddr: UInt, wen: Bool, wdata: UInt): Unit = {
    val chiselMapping = mapping.map { case (a, (r, wm, w, rm)) => (a.U, r, wm, w, rm) }
    rdata := LookupTree(raddr, chiselMapping.map { case (a, r, wm, w, rm) => (a, r & rm) })
    chiselMapping.map { case (a, r, wm, w, rm) =>
      if (w != null && wm != UnwritableMask) when(wen && waddr === a) { r := w(MaskData(r, wdata, wm)) }
    }
  }
  def generate(mapping: Map[Int, (UInt, UInt, UInt => UInt, UInt)], addr: UInt, rdata: UInt, wen: Bool, wdata: UInt): Unit = generate(mapping, /* raddr: */ addr, rdata, /* waddr: */ addr, wen, wdata)
}
