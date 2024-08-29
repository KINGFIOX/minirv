package test.hitsz.utils

class IROM(image: Array[Byte], user_base: Int, val size: Int) {
  private val irom: Array[Byte] = Array.fill(size)(0.toByte)
  Array.copy(image, 0, irom, 0, image.length)
  def fetch(addr: Int): Int = {
    val offset = addr - user_base
    (irom(offset) & 0x0_ff) | ((irom(offset + 1) & 0x0_ff) << 8) | ((irom(offset + 2) & 0x0_ff) << 16) | ((irom(offset + 3) & 0x0_ff) << 24)
  }
}
