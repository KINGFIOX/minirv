main:
    li   sp, 65536
    addi sp,sp,-32
    li   t5,119603200
    li   a2,-4096
    sw   s0,28(sp)
    sw   s1,24(sp)
    addi t5,t5,1159
    addi a2,a2,112
    li   a7,14680064
    li   a6,2097152
    li   t1,4194304
    li   t3,6291456
    li   t4,8388608
    li   t6,10485760
    li   a3,9
    li   a0,5
    li   a1,-4096
    j    .L15
.L2:
    beq  a4,t1,.L20
    beq  a4,t3,.L21
    beq  a4,t4,.L22
    beq  a4,t6,.L23
.L8:
    lw   a5,12(sp)
    srli a4,a5,16
    addi t0,a4,-10
    slli t0,t0,16
    srli t0,t0,16
    bgtu t0,a0,.L14
    ori  a4,t0,16
.L14:
    slli a5,a5,16
    slli a4,a4,16
    srli a5,a5,16
    or   a5,a5,a4
    sw   a5, 0(a1)
.L15:
    lw   a5, 0(a2)
    and  a4,a5,a7
    bne  a4,a6,.L2
    srli t0,a5,8
    andi t0,t0,0xff
    andi a4,t0,15
    andi t2,a5,15
    andi a5,a5,255
    srli t0,t0,4
    srli a5,a5,4
    add  a4,a4,t2
    add  a5,a5,t0
    bleu a4,a3,.L7
    addi a5,a5,1
    addi a4,a4,-10
.L7:
    slli a4,a4,16
    srli a4,a4,16
    slli a5,a5,16
    or   a4,a4,a5
    sw   a4,12(sp)
    j    .L8
.L22:
    srli a4,a5,8
    andi a4,a4,0xff
    andi a5,a5,255
    srli a5,a5,4
    andi t0,a4,15
    sra  t0,t0,a5
    srli a4,a4,4
    slli t0,t0,16
    sra  a5,a4,a5
    srli t0,t0,16
    slli a5,a5,16
    or   a5,t0,a5
    sw   a5,12(sp)
    j    .L8
.L20:
    srli t2,a5,8
    andi t2,t2,0xff
    andi s1,a5,0xff
    srli a5,a5,4
    srli t0,t2,4
    andi a4,t2,15
    andi a5,a5,15
    andi s0,s1,15
    bleu s1,t2,.L6
    mv   s1,a4
    mv   t2,t0
    mv   a4,s0
    mv   t0,a5
    mv   s0,s1
    mv   a5,t2
.L6:
    sub  a4,a4,s0
    slli a4,a4,16
    srli a4,a4,16
    sub  a5,t0,a5
    bleu a4,a3,.L7
    addi a5,a5,-1
    addi a4,a4,10
    j    .L7
.L21:
    srli a4,a5,8
    andi a4,a4,0xff
    srli a5,a5,4
    srli t0,a4,4
    andi a5,a5,15
    andi a4,a4,15
.L11:
    slli a4,a4,1
    slli a4,a4,16
    srli a4,a4,16
    addi a5,a5,-1
    slli t0,t0,1
    bleu a4,a3,.L10
    addi t0,t0,1
    addi a4,a4,-10
.L10:
    bne  a5,zero,.L11
    slli a4,a4,16
    srli a4,a4,16
    slli t0,t0,16
    or   a4,a4,t0
    sw   a4,12(sp)
    j    .L8
.L23:
    li   a5,98304
    addi a5,a5,1696
.L13:
    nop
    addi a5,a5,-1
    bne  a5,zero,.L13
    srli a4,t5,3
    srli a5,t5,2
    xor  a5,a5,a4
    xor  a5,a5,t5
    srli a4,t5,5
    xor  a5,a5,a4
    slli a5,a5,31
    srli t5,t5,1
    or   t5,a5,t5
    sw   t5,12(sp)
    j    .L8
read:
    li   a0,-4096
    addi a0,a0,112
    lw   a0, 0(a0)
    ret
write:
    srli a5,a0,16
    addi a4,a5,-10
    slli a4,a4,16
    srli a4,a4,16
    li   a3,5
    bgtu a4,a3,.L26
    ori  a5,a4,16
.L26:
    slli a0,a0,16
    slli a5,a5,16
    srli a0,a0,16
    li   a4,-4096
    or   a0,a0,a5
    sw   a0, 0(a4)
    ret
decimal:
    andi a0,a0,15
    ret
integer:
    srli a0,a0,4
    andi a0,a0,15
    ret
adddd:
    srli a5,a0,8
    andi a5,a5,0xff
    andi a0,a0,0xff
    andi a3,a0,15
    andi a4,a5,15
    add  a4,a4,a3
    srli a5,a5,4
    srli a0,a0,4
    li   a3,9
    add  a5,a5,a0
    bleu a4,a3,.L30
    addi a5,a5,1
    addi a4,a4,-10
.L30:
    slli a0,a4,16
    slli a5,a5,16
    srli a0,a0,16
    or   a0,a0,a5
    ret
subtract:
    srli a3,a0,8
    andi a4,a0,0xff
    andi a3,a3,0xff
    srli a0,a3,4
    andi a5,a3,15
    andi a1,a4,15
    srli a2,a4,4
    bleu a4,a3,.L32
    mv   a3,a5
    mv   a4,a0
    mv   a5,a1
    mv   a0,a2
    mv   a1,a3
    mv   a2,a4
.L32:
    sub  a5,a5,a1
    slli a5,a5,16
    srli a5,a5,16
    li   a4,9
    sub  a0,a0,a2
    bleu a5,a4,.L33
    addi a0,a0,-1
    addi a5,a5,10
.L33:
    slli a5,a5,16
    srli a5,a5,16
    slli a0,a0,16
    or   a0,a5,a0
    ret
multiply:
    srli a5,a0,8
    andi a5,a5,0xff
    andi a0,a0,0xff
    srli a4,a0,4
    li   a3,9
    srli a0,a5,4
    andi a5,a5,15
.L36:
    slli a5,a5,1
    slli a5,a5,16
    srli a5,a5,16
    addi a4,a4,-1
    slli a0,a0,1
    bleu a5,a3,.L35
    addi a0,a0,1
    addi a5,a5,-10
.L35:
    bne  a4,zero,.L36
    slli a5,a5,16
    srli a5,a5,16
    slli a0,a0,16
    or   a0,a5,a0
    ret
divide:
    srli a5,a0,8
    andi a5,a5,0xff
    andi a0,a0,0xff
    srli a4,a0,4
    andi a0,a5,15
    sra  a0,a0,a4
    srli a5,a5,4
    sra  a5,a5,a4
    slli a0,a0,16
    slli a5,a5,16
    srli a0,a0,16
    or   a0,a0,a5
    ret
lfsr32:
    li   a5,98304
    addi a5,a5,1696
.L40:
    nop
    addi a5,a5,-1
    bne  a5,zero,.L40
    lw   a5,0(a0)
    srli a3,a5,3
    srli a4,a5,2
    xor  a4,a4,a3
    xor  a4,a4,a5
    srli a3,a5,5
    xor  a4,a4,a3
    slli a4,a4,31
    srli a5,a5,1
    or   a5,a5,a4
    sw   a5,0(a0)
    mv   a0,a5
    ret