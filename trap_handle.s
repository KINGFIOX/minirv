    .file          "trap_handle.asm"
    .option        nopic
    .attribute     arch, "rv32i2p1_f2p2_d2p2_zicsr2p0"
    .attribute     unaligned_access, 0
    .attribute     stack_align, 16

    .text
    .align         2
    .globl         HANDLE
    .type          HANDLE, @function
HANDLE:
.LFB0:
    .cfi_startproc
    csrrwi         zero, 0x300, 0                                 # Disable interrupt

    addi           sp, sp, -24
    sw             s0, 20(sp)
    sw             t0, 16(sp)
    sw             t1, 12(sp)
    sw             t2, 8(sp)
    sw             t3, 4(sp)
    sw             t4, 0(sp)

    li             t0, 0x0000000b                                 # Machine external interrupt
    li             t1, 0x8000000b                                 # ECALL from M-mode

    csrrwi         t2, 0x342, 0                                   # Read MCAUSE
    beq            t2, t0, ECALL_HANDLER
    beq            t2, t1, EXTERNAL_INTR_HANDLER
    jal            zero, OTHER_TRAPS_HANDLER
    .cfi_endproc
.LFE0:
    .size          HANDLE, .-HANDLE


###########################################################
# Ecall Handler
###########################################################
    .align         2
    .globl         ECALL_HANDLER
    .type          ECALL_HANDLER, @function
ECALL_HANDLER:
.LFB1:
    .cfi_startproc
    lui            s0, 0xFFFFF

    sw             a0, 0x60(s0)                                   # Write LEDs
    sw             a0, 0x00(s0)                                   # Write 7-seg LEDs

    jal            zero, TRAP_RET
    .cfi_endproc
.LFE1:
    .size          ECALL_HANDLER, .-ECALL_HANDLER

###########################################################
# External Interrupts Handler
###########################################################

    .align         2
    .globl         EXTERNAL_INTR_HANDLER
    .type          EXTERNAL_INTR_HANDLER, @function
EXTERNAL_INTR_HANDLER:
.LFB2:
    .cfi_startproc
    lui            s0, 0xFFFFF

    li             t0, 0x11111111
    li             t1, 0x11111111
    li             t2, 0x99999999
    li             t4, 0xF

.LOOP:
    sw             t0, 0x60(s0)                                   # Write LEDs
    sw             t0, 0x00(s0)                                   # Write 7-seg LEDs

    li             t3, 0
.DELAY:
    addi           t3, t3, 1
    blt            t3, t4, .DELAY

    add            t0, t0, t1
    bne            t2, t0, .LOOP

    jal            zero, TRAP_RET
    .cfi_endproc
.LFE2:
    .size          EXTERNAL_INTR_HANDLER, .-EXTERNAL_INTR_HANDLER

###########################################################
# Other Traps Handler
###########################################################

    .align         2
    .globl         OTHER_TRAPS_HANDLER
    .type          OTHER_TRAPS_HANDLER, @function
OTHER_TRAPS_HANDLER:
.LFB3:
    .cfi_startproc
    lui            s0, 0xFFFFF

    ori            t0, zero, -1
    sw             t0, 0x60(s0)                                   # Write LEDs
    sw             t0, 0x00(s0)                                   # Write 7-seg LEDs

    jal            zero, TRAP_RET
    .cfi_endproc
.LFE3:
    .size          OTHER_TRAPS_HANDLER, .-OTHER_TRAPS_HANDLER

###########################################################
# trap ret
###########################################################

    .align         2
    .globl         TRAP_RET
    .type          TRAP_RET, @function
TRAP_RET:
.LFB4:
    .cfi_startproc
    lw             s0, 20(sp)
    lw             t0, 16(sp)
    lw             t1, 12(sp)
    lw             t2, 8(sp)
    lw             t3, 4(sp)
    lw             t4, 0(sp)
    addi           sp, sp, 24

    csrrwi         zero, 0x300, 0x8                               # Enable interrupt
    uret                                                          # Should be mret, however RARS only supports uret.
    .cfi_endproc
.LFE4:
    .size          TRAP_RET, .-TRAP_RET

.ident "GCC:
    (GNU)          13.3.0"
    .section       .note.GNU-stack,"",@progbits
