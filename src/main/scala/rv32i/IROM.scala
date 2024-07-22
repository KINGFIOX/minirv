package rv32i

/** @brief
  *   irom, 只读。约束: USER_BEGIN < KERNEL_BEGIN
  *
  * @param user
  * @param USER_BEGIN
  *   用户程序的开始地址
  * @param kernel
  * @param KERNEL_BEGIN
  *   内存程序的开始地址
  */
class IROM(user: Array[Byte], USER_BEGIN: Int, kernel: Array[Byte], KERNEL_BEGIN: Int) {
  def fetch(addr: Int): Int = {
    if (addr == 0x270) { throw new IllegalArgumentException("Invalid address: 0x270") }
    if (addr >= USER_BEGIN && addr < KERNEL_BEGIN) {
      val index = addr - USER_BEGIN
      (user(index) & 0x0_ff) | ((user(index + 1) & 0x0_ff) << 8) | ((user(index + 2) & 0x0_ff) << 16) | ((user(index + 3) & 0x0_ff) << 24)
    } else if (addr >= KERNEL_BEGIN) {
      val index = addr - KERNEL_BEGIN
      (kernel(index) & 0x0_ff) | ((kernel(index + 1) & 0x0_ff) << 8) | ((kernel(index + 2) & 0x0_ff) << 16) | ((kernel(index + 3) & 0x0_ff) << 24)
    } else {
      0
    }
  }
}
