typedef unsigned int uint32_t;

const uint32_t SWITCH_ADDR = 0xfffff070;
const uint32_t DIG_ADDR = 0xfffff000;

// 拨码开关, 只有 24 位有效
typedef union __switch {
    struct
    {
        uint32_t B : 8;
        uint32_t A : 8;
        uint32_t : 8; // reserved
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

static inline __switch read()
{
    __switch val;
    asm volatile("lw %0, 0(%1)"
                 : "=r"(val) // 输出：把读取的值保存在`value`变量中
                 : "r"(SWITCH_ADDR) // 输入：内存地址`addr`
                 : "memory"); // 告诉编译器这个操作会影响内存
    return val;
}

static inline void write(__dig val)
{
    // cano
    asm volatile("sw %0, 0(%1)"
                 :
                 : "r"(val), "r"(DIG_ADDR)
                 : "memory");
}

static inline void exit()
{
    asm volatile(
        "ecall\n" //
        : // output
        : // input
        : "memory" // 破坏的寄存器
    );
}

#define ASSERT(cond) \
    if (!(cond)) {   \
        exit();      \
    }

static inline uint32_t decimal(uint32_t val)
{
    return val & 0b01111;
}

static inline uint32_t integer(uint32_t val)
{
    return val >> 4;
}

__dig add(__switch sw)
{
    uint32_t a_integer = integer(sw.A);
    uint32_t a_decimal = decimal(sw.A);
    ASSERT(a_decimal < 10);
    uint32_t b_integer = integer(sw.B);
    uint32_t b_decimal = decimal(sw.B);
    ASSERT(b_decimal < 10);
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
    ASSERT(a_decimal < 10);
    ASSERT(b_decimal < 10);

    __dig ret;
    ret._bits = 0;
    ret.integer = a_integer - b_integer;
    ret.decimal = a_decimal - b_decimal;
    if (ret.decimal < 0) {
        ret.integer -= 1;
        ret.decimal += 10;
    }
    return ret;
}

__dig mul(__switch sw)
{
    uint32_t a_integer = integer(sw.A);
    uint32_t a_decimal = decimal(sw.A);
    ASSERT(a_decimal == 0);
    uint32_t b_integer = integer(sw.B);
    uint32_t b_decimal = decimal(sw.B);
    ASSERT(b_decimal == 0);
    __dig ret;
    ret._bits = 0;
    ret.integer = a_integer << b_integer;
    return ret;
}

__dig div(__switch sw)
{
    uint32_t a_integer = integer(sw.A);
    uint32_t a_decimal = decimal(sw.A);
    ASSERT(a_decimal == 0);
    uint32_t b_integer = integer(sw.B);
    uint32_t b_decimal = decimal(sw.B);
    ASSERT(b_decimal == 0);
    __dig ret;
    ret._bits = a_integer >> b_integer;
    if (ret.decimal >= 10) {
        ret.integer += 1;
        ret.decimal -= 10;
    }
    return ret;
}

__dig lfsr32()
{
    static uint32_t lfsr = 0x12345678;
    uint32_t xor = ((lfsr >> 0) ^ (lfsr >> 2) ^ (lfsr >> 3) ^ (lfsr >> 5)) & 1;
    lfsr = (lfsr >> 1) | (xor << 31);
    __dig ret;
    ret._bits = lfsr;
    return ret;
}

int main(void)
{
    while (1) {
        __switch cur = read();
        __dig dig;
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
        case 0b101: /* lfsr */
            dig = lfsr32();
            break;
        default:
            break;
        }
        write(dig);
    }
}
