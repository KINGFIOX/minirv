package rv32i

class DRAM(code: Array[Byte], val DRAM_BEGIN: Int, val len: Int /* 大小 */ ) {
  val dram: Array[Byte] = Array.copyOf(code, len)

  def load(addr: Int, size: Int): Int = {
    val index = (addr - DRAM_BEGIN) // dram 的 index
    size match {
      case 8 => { dram(index) & 0x0ff }
      case 16 => { (dram(index) & 0x0ff) | ((dram(index + 1) & 0x0ff) << 8) }
      case 32 => { (dram(index) & 0x0ff) | ((dram(index + 1) & 0x0ff) << 8) | ((dram(index + 2) & 0x0ff) << 16) | ((dram(index + 3) & 0x0ff) << 24) }
      case _ => 0
    }
  }

  def store(addr: Int, size: Int, value: Int): Unit = {
    val index = addr - DRAM_BEGIN // dram 的 index
    size match {
      case 8 => { dram(index) = (value & 0x0ff).toByte }
      case 16 => {
        dram(index)     = (value & 0x0ff).toByte
        dram(index + 1) = ((value >> 8) & 0x0ff).toByte
      }
      case 32 => {
        dram(index)     = (value & 0x0ff).toByte
        dram(index + 1) = ((value >> 8) & 0x0ff).toByte
        dram(index + 2) = ((value >> 16) & 0x0ff).toByte
        dram(index + 3) = ((value >> 24) & 0x0ff).toByte
      }
      case _ =>
    }
  }
}
