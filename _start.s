    .section .text.entry
    .global  _start
_start:
    li       sp,0x10000  # (1 << 14) << 2
    call     main
