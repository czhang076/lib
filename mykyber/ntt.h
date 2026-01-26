#ifndef NTT_H
#define NTT_H

#include <stdint.h>
#include "params.h"

/* * Zeta 表 (参考实现常量)
 * 必须在 ntt.c 中定义
 */
extern const int16_t zetas[128];

/*
 * ntt: 正向数论变换
 * 输入: 系数形式的多项式
 * 输出: NTT 域的多项式 (系数被打乱)
 */
void ntt(int16_t r[256]);

/*
 * invntt: 逆向数论变换
 * 输入: NTT 域的多项式
 * 输出: 系数形式的多项式 (需要额外乘以 1/N 因子)
 */
void invntt(int16_t r[256]);

/*
 * basemul: 基乘法
 * 在 NTT 域中计算两个一次多项式的乘积
 * r = a * b mod (x^2 - zeta)
 * 这是 Kyber 乘法的最小原子操作
 */
void basemul(int16_t r[2], const int16_t a[2], const int16_t b[2], int16_t zeta);

#endif
