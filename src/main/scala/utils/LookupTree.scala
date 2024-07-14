package utils

import chisel3._
import chisel3.util._

object LookupTree {

  /** @brief
    *
    * @param key
    *   RegMap 中 传入地址
    * @param mapping
    *   地址表与内容对应的表
    * @return
    */
  def apply[T <: Data](key: UInt, mapping: Iterable[(UInt, T)]): T =
    Mux1H(mapping.map(p /* p 是元组 */ => (p._1 /* a */ === key, p._2 /* r */ )))
  // Mux1H(in : Iterable[(Bool, T)])
  // 其中这个 Iterable[(Bool, T)] 这种 Bool 维度中, 只有一个 true.B 其他都是 false.B
  // 然后就能快速的找到对应的 T, 返回 T
}

object LookupTreeDefault {

  /** @brief
    *   含有默认值的 LookupTree
    *
    * @param key
    * @param default
    * @param mapping
    * @return
    */
  def apply[T <: Data](key: UInt, default: T, mapping: Iterable[(UInt, T)]): T =
    MuxLookup(key, default)(mapping.toSeq)
}
