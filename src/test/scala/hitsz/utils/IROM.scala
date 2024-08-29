package test.hitsz.utils

class IROM(user: Array[Byte], user_base: Int) {
  def fetch(addr: Int): Int = {
    if (user_base <= addr && addr < user_base + user.length) {
      val offset = addr - user_base
      (user(offset) & 0x0_ff) | ((user(offset + 1) & 0x0_ff) << 8) | ((user(offset + 2) & 0x0_ff) << 16) | ((user(offset + 3) & 0x0_ff) << 24)
    } else {
      return 0
    }
  }
}
