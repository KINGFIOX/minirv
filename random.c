/* ---------- ---------- random.h ---------- ---------- */

typedef unsigned int uint32_t;

#define SWITCH_ADDR 0xfffff070
#define DIG_ADDR 0xfffff000

// 拨码开关, 只有 24 位有效
typedef union __switch {
    struct
    {
        uint32_t B : 8;
        uint32_t A : 8;
        uint32_t : 5; // reserved
        uint32_t op : 3;
    };
    uint32_t _bits : 32;
} __switch;

// 数码管, 可以表示 8 个数字
typedef union __dig {
    /* data */
    struct
    {
        uint32_t decimal : 16;
        uint32_t integer : 16;
    };
    uint32_t _bits : 32;
} __dig;

/* ---------- ---------- random.c ---------- ---------- */

__switch read()
{
    __switch val;
    // memory map io
    asm volatile("lw %0, 0(%1)"
                 : "=r"(val) // 输出：把读取的值保存在`value`变量中
                 : "r"(SWITCH_ADDR) // 输入：内存地址`addr`
                 : "memory"); // 告诉编译器这个操作会影响内存
    return val;
}

void write(__dig val)
{
    if (val.integer <= 15) {
        // < 10 : 保持原样
        if (val.integer >= 10) {
            uint32_t one = val.integer - 10;
            val.integer = 0b10000 | one;
        }
    }

    asm volatile("sw %0, 0(%1)"
                 :
                 : "r"(val), "r"(DIG_ADDR)
                 : "memory");
}

uint32_t decimal(uint32_t val)
{
    return val & 0b01111;
}

uint32_t integer(uint32_t val)
{
    return (val >> 4) & 0b01111;
}

__dig add(__switch sw)
{
    uint32_t a_integer = integer(sw.A);
    uint32_t a_decimal = decimal(sw.A);
    uint32_t b_integer = integer(sw.B);
    uint32_t b_decimal = decimal(sw.B);
    __dig ret;
    ret._bits = 0;
    ret.integer = a_integer + b_integer;
    ret.decimal = a_decimal + b_decimal;
    if (ret.decimal >= 10) {
        ret.integer += 1;
        ret.decimal -= 10;
    }
    return ret;
}

__dig sub(__switch sw)
{
    uint32_t a_integer;
    uint32_t a_decimal;
    uint32_t b_integer;
    uint32_t b_decimal;
    if (sw.A >= sw.B) {
        a_integer = integer(sw.A);
        a_decimal = decimal(sw.A);
        b_integer = integer(sw.B);
        b_decimal = decimal(sw.B);
    } else {
        a_integer = integer(sw.B);
        a_decimal = decimal(sw.B);
        b_integer = integer(sw.A);
        b_decimal = decimal(sw.A);
    }

    __dig ret;
    ret._bits = 0;
    ret.integer = a_integer - b_integer;
    ret.decimal = a_decimal - b_decimal;
    if (ret.decimal >= 10) {
        ret.integer -= 1;
        ret.decimal += 10;
    }
    return ret;
}

__dig mul(__switch sw)
{
    // uint32_t b_decimal = decimal(sw.B);
    __dig ret;
    ret._bits = 0;
    ret.integer = integer(sw.A);
    ret.decimal = decimal(sw.A);
    for (uint32_t i = 0; i < integer(sw.B); i++) {
        ret.integer = ret.integer << 1;
        ret.decimal = ret.decimal << 1;
        if (ret.decimal >= 10) {
            ret.integer += 1;
            ret.decimal -= 10;
        }
    }
    return ret;
}

__dig div(__switch sw)
{
    uint32_t b_integer = integer(sw.B);
    __dig ret;
    ret._bits = 0;
    ret.integer = integer(sw.A);
    ret.decimal = decimal(sw.A);
    ret.integer = ret.integer >> b_integer;
    ret.decimal = ret.decimal >> b_integer;
    return ret;
}

__dig lfsr32(uint32_t seed)
{
    for (uint32_t i = 0; i < 100000; i++) {
        asm volatile("add zero, zero, zero"
                     :
                     :
                     : "memory");
    }
    uint32_t xor = ((seed >> 0) ^ (seed >> 2) ^ (seed >> 3) ^ (seed >> 5)) & 1;
    seed = (seed >> 1) | (xor << 31);
    __dig ret;
    ret._bits = seed;
    return ret;
}

int main(void)
{
    uint32_t seed = 0x07210487;
    while (1) {
        __switch cur = read();
        volatile __dig dig;
        switch (cur.op) {
        case 0b001: /* add */
            dig = add(cur);
            break;
        case 0b010: /* sub */
            dig = sub(cur);
            break;
        case 0b011: /* A * 2^B */
            dig = mul(cur);
            break;
        case 0b100: /* A / 2^B */
            dig = div(cur);
            break;
        case 0b101: /* seed */
            dig = lfsr32(seed);
            seed = dig._bits;
            break;
        default:
            break;
        }
        write(dig);
    }
}
