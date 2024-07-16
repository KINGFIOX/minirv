object ENV {
  // 定义: 是输出到 vivado, 还是 verilator
}

/** @brief
  *   .sv 文件要输出到 cdp-tests/mySoC/miniRV_SoC.v
  */
object Hello {
  def main(args: Array[String]) = {
    val x = 12
    val y = 3
    val q = (x + y - 1) / y;
    println(s"Ceil division of $x by $y is $q")
  }
}
