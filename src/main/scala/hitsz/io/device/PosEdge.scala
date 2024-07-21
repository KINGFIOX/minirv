package hitsz.io.device

import chisel3._
import chisel3.util._

object PosEdge {
  def apply(sig: Bool): Bool = {
    sig & ~RegNext(sig)
  }
  def apply(sig: UInt): UInt = {
    sig & ~RegNext(sig)
  }
}
