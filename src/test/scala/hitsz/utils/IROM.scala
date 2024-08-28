package test.hitsz.utils

class IROM(user: Array[Byte], user_base: Int) {
  def fetch(addr: Int): Int = {
    val offset = addr - user_base
    (user(offset) & 0x0_ff) | ((user(offset + 1) & 0x0_ff) << 8) | ((user(offset + 2) & 0x0_ff) << 16) | ((user(offset + 3) & 0x0_ff) << 24)
  }
}
