package rv32i

class BUS(code: Array[Byte]) {
  val dram = new DRAM(code, Settings.DRAM_BEGIN, Settings.DRAM_LEN)

  def load(addr: Int, size: Int): Int = {
    // if (Settings.DRAM_BEGIN <= addr) dram.load(addr, size) else 0
    addr match {
      case a if Settings.DRAM_BEGIN <= a && a < Settings.DRAM_BEGIN + Settings.DRAM_LEN => dram.load(a, size)
      //   case a: Long if Settings.BTN_BEGIN <= a && a < Settings.BTN_BEGIN + Settings.BTN_LEN => /* 访问按钮 */ dram.load(a, size)
      case _ => 0
    }
  }

  def store(addr: Int, size: Int, value: Int): Unit = {
    // if (Settings.DRAM_BEGIN <= addr) dram.store(addr, size, value)
    addr match {
      case a if Settings.DRAM_BEGIN <= a && a < Settings.DRAM_BEGIN + Settings.DRAM_LEN => dram.store(addr, size, value)
      case _ => /* 啥也不干 */
    }
  }
}
