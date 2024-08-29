coe: random.bin
	python3 bin2coe.py
	
bin: random.c
	riscv32-none-elf-gcc -march=rv32ed -mabi=ilp32e -nostdlib -Ofast -c random.c -o random.o
	riscv32-none-elf-as -march=rv32ed -mabi=ilp32e _start.s -o _start.o
	riscv32-none-elf-ld -T linker.ld _start.o random.o -o random.elf
	riscv32-none-elf-objcopy -O binary random.elf random.bin

dump: random.elf
	riscv32-none-elf-objdump -d random.elf > random.dump

chisel:
	sbt 'runMain hitsz.Main'