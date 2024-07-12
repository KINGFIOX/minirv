package component

import chisel3._
import chisel3.util._

object ZEXTUOpType extends ChiselEnum {
  val zextu_X, zextu_ZEXT = Value
}

class ZEXTU extends Module {}
