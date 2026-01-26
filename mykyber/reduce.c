#include <stdint.h>
#include "params.h"
#include "reduce.h"

/*
 * Montgomery reduction
 * 输入: 32位整数 a
 * 输出: 16位整数，结果等于 a * 2^{-16} mod q
 * 范围: 结果在 [-q, q) 之间
 */
int16_t montgomery_reduce(int32_t a)
{
    int16_t t;

    // 1. 计算 t = a * QINV mod 2^16
    //    这里的乘法是 16位 * 16位 (截断)，这步非常快
    t = (int16_t)a * QINV;

    // 2. 计算 (a - t*q) / 2^16
    //    如果 t 是正确计算的，a - t*q 必然能被 2^16 整除
    //    右移 16 位相当于除以 2^16
    t = (a - (int32_t)t * KYBER_Q) >> 16;

    return t;
}

/*
 * Barrett reduction
 * 输入: 16位整数 a
 * 输出: 16位整数，结果等于 a mod q
 * 范围: 结果在 [-q/2, q/2] 附近
 */
int16_t barrett_reduce(int16_t a)
{
    int16_t t;
    const int16_t v = ((1U << 26) + KYBER_Q / 2) / KYBER_Q;

    t = ((int32_t)v * a + (1 << 25)) >> 26;
    t *= KYBER_Q;
    return a - t;
}

/*
 * csubq:
 * 条件减法，确保结果接近标准范围
 */
int16_t csubq(int16_t a)
{
    a -= KYBER_Q;
    a += (a >> 15) & KYBER_Q;
    return a;
}
