#ifndef PARAMS_H
#define PARAMS_H

/* * 1. 基础常量定义 
 * KYBER_N: 多项式系数个数 (256)
 * KYBER_Q: 模数 (3329)
 */
#define KYBER_N 16
#define KYBER_Q 3329

/* * 2. 通用尺寸定义
 * SYMBYTES: 哈希输出、种子、消息的字节长度 (32 bytes = 256 bits)
 * SSBYTES:  最终协商出的共享密钥长度 (32 bytes)
 */
#define KYBER_SYMBYTES 32
#define KYBER_SSBYTES  32

/* * 3. Kyber-768 固定参数 (Level 3)
 * 仅支持 KYBER-768，不提供安全等级切换
 * K:    多项式向量维度 (3)
 * ETA1: 密钥生成的噪声参数 (2)
 * ETA2: 加密过程的噪声参数 (2)
 * DU:   向量 u 的压缩位数 (10 bits)
 * DV:   多项式 v 的压缩位数 (4 bits)
 */
#define KYBER_K 2
#define KYBER_ETA1 2
#define KYBER_ETA2 2
#define KYBER_DU 12
#define KYBER_DV 12

/* * 4. 派生数据结构大小 (字节单位)
 * POLYBYTES: 一个未压缩多项式占用的字节数 (12 bits * 256 / 8 = 384 bytes)
 */
#define KYBER_POLYBYTES		24 // 12 * 16 / 8
#define KYBER_POLYVECBYTES	(KYBER_K * KYBER_POLYBYTES)

/* * 5. 压缩后的大小计算 (这是 Kyber 密文小的关键)
 * POLYCOMPRESSEDBYTES:    多项式 v (dv=4) 压缩后大小 -> 4 * 256 / 8 = 128 bytes
 * POLYVECCOMPRESSEDBYTES: 向量 u (du=10) 压缩后大小 -> K * (10 * 256 / 8) = 3 * 320 = 960 bytes
 */
#define KYBER_POLYCOMPRESSEDBYTES    KYBER_POLYBYTES
#define KYBER_POLYVECCOMPRESSEDBYTES KYBER_POLYVECBYTES

/* * 6. IND-CPA 基础公钥加密参数
 * INDCPA_BYTES: 密文大小 = 压缩后的向量 u + 压缩后的多项式 v
 */
#define KYBER_INDCPA_MSGBYTES       KYBER_SYMBYTES
#define KYBER_INDCPA_PUBLICKEYBYTES (KYBER_POLYVECBYTES + KYBER_SYMBYTES)
#define KYBER_INDCPA_SECRETKEYBYTES (KYBER_POLYVECBYTES)
#define KYBER_INDCPA_BYTES          (KYBER_POLYVECCOMPRESSEDBYTES + KYBER_POLYCOMPRESSEDBYTES)

/* * 7. IND-CCA KEM 最终接口参数
 * PUBLICKEYBYTES:  等同于 CPA 公钥
 * SECRETKEYBYTES:  CPA私钥 + CPA公钥 + H(pk) + z (FO变换需要保存这些)
 * CIPHERTEXTBYTES: 等同于 CPA 密文
 */
#define KYBER_PUBLICKEYBYTES  KYBER_INDCPA_PUBLICKEYBYTES
#define KYBER_SECRETKEYBYTES  (KYBER_INDCPA_SECRETKEYBYTES + KYBER_INDCPA_PUBLICKEYBYTES + 2*KYBER_SYMBYTES)
#define KYBER_CIPHERTEXTBYTES KYBER_INDCPA_BYTES

#endif
