all: bin trap dump

bin: fib.c
	riscv32-unknown-linux-gnu-gcc -S fib.c -march=rv32e -mabi=ilp32e
	riscv32-unknown-linux-gnu-gcc -Wl,-Ttext=0x0 -nostdlib -march=rv32e -mabi=ilp32e -o fib fib.s
	riscv32-unknown-linux-gnu-objcopy -O binary fib fib.bin

emu:
	sbt "runMain rv32i.Main"

dump: bin
	riscv32-unknown-linux-gnu-objdump -d fib > fib.dump
	riscv32-unknown-linux-gnu-objdump -d trap_handle > trap_handle.dump

trap: fib.c
	riscv32-unknown-linux-gnu-gcc -Wl,-Ttext=0x0 -nostdlib -march=rv32e -mabi=ilp32e -o trap_handle trap_handle.s
	riscv32-unknown-linux-gnu-objcopy -O binary trap_handle trap_handle.bin

clean:
	rm -f fib
	rm -f fib.bin
	rm -f fib.s
	rm -f fib.dump
	rm -f a.out
	rm -f trap_handle
	rm -f trap_handle.bin
