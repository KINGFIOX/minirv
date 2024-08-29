OUTPUT_ARCH(riscv)
ENTRY(_start)
BASE_ADDRESS = 0x00000000;

SECTIONS
{
    . = BASE_ADDRESS;
    stext = .;
    .text : {
        *(.text.entry)
        *(.text)
    }

    . = ALIGN(4K);
    etext = .;
    sdata = .;
    .data : {
        *(.data)
    }

    . = ALIGN(4K);
    edata = .;
}