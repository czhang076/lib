#include <stdint.h>
#include "params.h"
#include "reduce.h"
#include "ntt.h"

/*
 * zetas: Kyber 标准的 128 个 Montgomery 域 NTT 常量
 * 与参考实现一致的顺序
 */
const int16_t zetas[128] = {
    -1044,  -758,  -359, -1517,  1493,  1422,   287,   202,
     -171,   622,  1577,   182,   962, -1202, -1474,  1468,
        573, -1325,   264,   383,  -829,  1458, -1602,  -130,
     -681,  1017,   732,   608, -1542,   411,  -205, -1571,
     1223,   652,  -552,  1015, -1293,  1491,  -282, -1544,
        516,    -8,  -320,  -666, -1618, -1162,   126,  1469,
     -853,   -90,  -271,   830,   107, -1421,  -247,  -951,
     -398,   961, -1508,  -725,   448, -1065,   677, -1275,
    -1103,   430,   555,   843, -1251,   871,  1550,   105,
        422,   587,   177,  -235,  -291,  -460,  1574,  1653,
     -246,   778,  1159,  -147,  -777,  1483,  -602,  1119,
    -1590,   644,  -872,   349,   418,   329,  -156,   -75,
        817,  1097,   603,   610,  1322, -1285, -1465,   384,
    -1215,  -136,  1218, -1335,  -874,   220, -1187, -1659,
    -1185, -1530, -1278,   794, -1510,  -854,  -870,   478,
     -108,  -308,   996,   991,   958, -1460,  1522,  1628
};

static inline int16_t fqmul(int16_t a, int16_t b) {
    return montgomery_reduce((int32_t)a * b);
}

/*
 * ntt: 正向变换 (Cooley-Tukey 算法)
 * 逻辑：多项式被不断拆分，每一层使用一个 zeta 进行蝴蝶运算
 */
void ntt(int16_t r[256]) {
    unsigned int len, start, j, k;
    int16_t t, zeta;

    k = 1;
    // len 从 128 减半直到 2
    for(len = 128; len >= 2; len >>= 1) {
        for(start = 0; start < 256; start = j + len) {
            zeta = zetas[k++];
            for(j = start; j < start + len; j++) {
                // Cooley-Tukey 蝴蝶运算
                t = fqmul(zeta, r[j + len]);
                r[j + len] = r[j] - t;
                r[j] = r[j] + t;
            }
        }
    }
}

/*
 * invntt: 逆向变换 (Gentleman-Sande 算法)
 * 逻辑：与 NTT 相反，这里 len 从 2 增加到 128
 * 注意：通常最后需要乘以 1/256，但为了保持精度，Kyber 里的因子略有不同
 */
void invntt(int16_t r[256]) {
    unsigned int start, len, j, k;
    int16_t t, zeta;
    // f = 1441 是 Montgomery 域中的 1/128 (用于抵消系数膨胀)
    const int16_t f = 1441; 

    k = 127;
    for(len = 2; len <= 128; len <<= 1) {
        for(start = 0; start < 256; start = j + len) {
            // 使用 zetas 的逆序（与 Kyber 参考实现一致）
            zeta = zetas[k--];
            for(j = start; j < start + len; j++) {
                // Gentleman-Sande 蝴蝶运算
                t = r[j];
                r[j] = barrett_reduce(t + r[j + len]);
                r[j + len] = r[j + len] - t;
                r[j + len] = fqmul(zeta, r[j + len]);
            }
        }
    }

    // 最后的缩放处理
    for(j = 0; j < 256; j++) {
        r[j] = fqmul(r[j], f);
    }
}

/*
 * basemul: 在 NTT 域中做乘法
 * * 原理：Kyber 的 NTT 并不是拆到单个数字，而是拆到 2x2 的小结构
 * 我们要计算 (a0 + a1*x) * (b0 + b1*x) mod (x^2 - zeta)
 */
void basemul(int16_t r[2], const int16_t a[2], const int16_t b[2], int16_t zeta) {
    // r[0] = a[0]b[0] + a[1]b[1] * zeta
    r[0] = fqmul(a[1], b[1]);
    r[0] = fqmul(r[0], zeta);
    r[0] += fqmul(a[0], b[0]);

    // r[1] = a[0]b[1] + a[1]b[0]
    r[1] = fqmul(a[0], b[1]);
    r[1] += fqmul(a[1], b[0]);
}
