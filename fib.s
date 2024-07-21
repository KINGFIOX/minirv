	.file	"fib.c"
	.option nopic
	.attribute arch, "rv32e1p9"
	.attribute unaligned_access, 0
	.attribute stack_align, 4
	.text
	.align	2
	.globl	main
	.type	main, @function
main:
.LFB0:
	.cfi_startproc
	addi	sp,sp,-16
	.cfi_def_cfa_offset 16
	sw	ra,12(sp)
	sw	s0,8(sp)
	.cfi_offset 1, -4
	.cfi_offset 8, -8
	addi	s0,sp,16
	.cfi_def_cfa 8, 0
	li	a0,10
	call	fib
	sw	a0,-16(s0)
#APP
# 6 "fib.c" 1
	ecall
# 0 "" 2
#NO_APP
	lw	a5,-16(s0)
	mv	a0,a5
	lw	ra,12(sp)
	.cfi_restore 1
	lw	s0,8(sp)
	.cfi_restore 8
	.cfi_def_cfa 2, 16
	addi	sp,sp,16
	.cfi_def_cfa_offset 0
	jr	ra
	.cfi_endproc
.LFE0:
	.size	main, .-main
	.align	2
	.globl	fib
	.type	fib, @function
fib:
.LFB1:
	.cfi_startproc
	addi	sp,sp,-16
	.cfi_def_cfa_offset 16
	sw	ra,12(sp)
	sw	s0,8(sp)
	sw	s1,4(sp)
	.cfi_offset 1, -4
	.cfi_offset 8, -8
	.cfi_offset 9, -12
	addi	s0,sp,16
	.cfi_def_cfa 8, 0
	sw	a0,-16(s0)
	lw	a5,-16(s0)
	beq	a5,zero,.L4
	lw	a4,-16(s0)
	li	a5,1
	bne	a4,a5,.L5
.L4:
	lw	a5,-16(s0)
	j	.L6
.L5:
	lw	a5,-16(s0)
	addi	a5,a5,-1
	mv	a0,a5
	call	fib
	mv	s1,a0
	lw	a5,-16(s0)
	addi	a5,a5,-2
	mv	a0,a5
	call	fib
	mv	a5,a0
	add	a5,s1,a5
.L6:
	mv	a0,a5
	lw	ra,12(sp)
	.cfi_restore 1
	lw	s0,8(sp)
	.cfi_restore 8
	.cfi_def_cfa 2, 16
	lw	s1,4(sp)
	.cfi_restore 9
	addi	sp,sp,16
	.cfi_def_cfa_offset 0
	jr	ra
	.cfi_endproc
.LFE1:
	.size	fib, .-fib
	.ident	"GCC: (GNU) 13.3.0"
	.section	.note.GNU-stack,"",@progbits
