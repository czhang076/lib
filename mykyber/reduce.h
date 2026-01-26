#ifndef REDUCE_H
#define REDUCE_H

#include <stdint.h>
#include "params.h"

/* * QINV = -q^{-1} mod 2^16
 * q = 3329
 */
#define QINV -3327

/*
 * montgomery_reduce:
 * 计算 a * R^(-1) mod q，其中 R = 2^16
 * 用于 NTT 乘法后的归约
 */
int16_t montgomery_reduce(int32_t a);

/*
 * barrett_reduce:
 * 计算 a mod q
 * 这是一个通用的模约减函数
 */
int16_t barrett_reduce(int16_t a);

/*
 * csubq:
 * 条件减法，确保结果落在 [0, q) 或接近范围
 */
int16_t csubq(int16_t a);

#endif
