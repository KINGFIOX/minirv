int fib(int n);

int main()
{
    int ret = fib(10);
    asm volatile("ecall");
    return ret;
}

int fib(int n)
{
    if (n == 0 || n == 1)
    {
        return n;
    }
    else
    {
        return (fib(n - 1) + fib(n - 2));
    }
}