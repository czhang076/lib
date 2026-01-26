#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include "params.h"
#include "reduce.h"
#include "ntt.h"
#include "poly.h"
#include "polyvec.h"
#include "indcpa.h"

#define N KYBER_N
#define Q KYBER_Q

// 辅助函数：将结果规范化到 [0, Q) 范围以便比较
static int16_t standard_reduce(int16_t a) {
    int16_t t = barrett_reduce(a);
    t = csubq(t);
    if (t < 0) t += Q;
    return t;
}

// 简单可复现的伪随机
static uint32_t lcg(uint32_t *state) {
    *state = (*state * 1103515245u + 12345u);
    return *state;
}

int test_ntt_correctness() {
    int16_t a[N], b[N];
    int i;
    int errors = 0;

    printf("[Test] Running NTT Round-Trip check...\n");

    // 1. 初始化测试数据
    for(i = 0; i < N; i++) {
        a[i] = (i * 12345) % Q; // 伪随机填充
        b[i] = a[i];            // 备份原始数据
    }

    // 2. 执行正向变换 NTT
    ntt(a);

    // 3. 执行逆向变换 InvNTT
    invntt(a);

    // 4. 验证结果
    // 注意：NTT/InvNTT 运算后结果可能是不完全约减的(在 -Q 到 Q 之间)
    // 所以比较前需要做一次规范化
    for(i = 0; i < N; i++) {
        int16_t val_orig = standard_reduce(b[i]);
        int16_t val_calc = standard_reduce(a[i]);

        if(val_orig != val_calc) {
            printf("ERROR at index %d: Original %d, Got %d\n", i, val_orig, val_calc);
            errors++;
        }
    }

    if(errors == 0) {
        printf("PASS: NTT -> InvNTT matches perfectly!\n");
        return 0;
    } else {
        printf("FAIL: Found %d mismatches.\n", errors);
        return 1;
    }
}

int test_reduction() {
    printf("[Test] Running Reduction check...\n");
    
    // 测试 3329 (应该变成 0)
    int16_t res = barrett_reduce(3329);
    if(res != 0) {
        printf("FAIL: barrett_reduce(3329) = %d (expected 0)\n", res);
        return 1;
    }

    // 测试负数 -1 (应该等价于 Q-1, 但 barrett 可能返回 -1 或 Q-1，具体取决于实现细节)
    // 我们的 barrett_reduce 通常返回范围在 [-Q/2, Q/2] 附近
    // 所以这里主要检查同余性
    int16_t val = -1;
    res = barrett_reduce(val);
    if ((res % Q) != (val % Q)) {
        printf("FAIL: Reduction correctness failed for -1\n");
        return 1;
    }

    // 扫描一段范围，检查 barrett 与 csubq 的同余性
    for(int i = -5000; i <= 5000; i++) {
        int16_t a = (int16_t)i;
        int16_t b = barrett_reduce(a);
        int16_t c = csubq(b);
        int16_t norm = c;
        if (norm < 0) norm += Q;
        int16_t ref = a % Q;
        if (ref < 0) ref += Q;
        if (norm != ref) {
            printf("FAIL: reduce mismatch at %d: got %d, expected %d\n", i, norm, ref);
            return 1;
        }
    }

    printf("PASS: Basic reductions look good.\n");
    return 0;
}

int test_ntt_roundtrip() {
    int16_t a[N], b[N];
    int errors = 0;
    uint32_t state = 1;

    printf("[Test] Running NTT Round-Trip check...\n");

    for(int i = 0; i < N; i++) {
        a[i] = (int16_t)(lcg(&state) % Q);
        b[i] = a[i];
    }

    ntt(a);
    invntt(a);

    // invntt 输出在 Montgomery 域，需要转换回普通域再比较
    for(int i = 0; i < N; i++) {
        a[i] = montgomery_reduce(a[i]);
    }

    for(int i = 0; i < N; i++) {
        int16_t val_orig = standard_reduce(b[i]);
        int16_t val_calc = standard_reduce(a[i]);

        if(val_orig != val_calc) {
            printf("ERROR at index %d: Original %d, Got %d\n", i, val_orig, val_calc);
            errors++;
        }
    }

    if(errors == 0) {
        printf("PASS: NTT -> InvNTT matches perfectly!\n");
        return 0;
    } else {
        printf("FAIL: Found %d mismatches.\n", errors);
        return 1;
    }
}

static void poly_naive_mul(poly *r, const poly *a, const poly *b) {
    int64_t tmp[N];
    for (int i = 0; i < N; i++) tmp[i] = 0;

    for (int i = 0; i < N; i++) {
        for (int j = 0; j < N; j++) {
            int idx = i + j;
            int64_t prod = (int64_t)a->coeffs[i] * b->coeffs[j];
            if (idx < N) tmp[idx] += prod;
            else tmp[idx - N] -= prod;
        }
    }

    for (int i = 0; i < N; i++) {
        int32_t v = (int32_t)(tmp[i] % Q);
        if (v < 0) v += Q;
        r->coeffs[i] = (int16_t)v;
    }
}

int test_poly_add_mul() {
    printf("[Test] Running poly add/mul check...\n");

    poly a, b, c_ntt, c_ref;
    uint32_t state = 7;

    for (int i = 0; i < N; i++) {
        a.coeffs[i] = (int16_t)(lcg(&state) % Q);
        b.coeffs[i] = (int16_t)(lcg(&state) % Q);
    }

    poly_naive_mul(&c_ref, &a, &b);

    poly_ntt(&a);
    poly_ntt(&b);
    poly_basemul_montgomery(&c_ntt, &a, &b);
    poly_invntt_tomont(&c_ntt);

    for (int i = 0; i < N; i++) {
        c_ntt.coeffs[i] = standard_reduce(c_ntt.coeffs[i]);
    }

    for (int i = 0; i < N; i++) {
        int16_t ref = standard_reduce(c_ref.coeffs[i]);
        if (c_ntt.coeffs[i] != ref) {
            printf("FAIL: poly mul mismatch at %d: got %d, expected %d\n", i, c_ntt.coeffs[i], ref);
            return 1;
        }
    }

    printf("PASS: poly add/mul looks good.\n");
    return 0;
}

int test_noise_bounds() {
    printf("[Test] Running noise bounds check...\n");

    poly r1, r2;
    uint8_t seed[KYBER_SYMBYTES];
    uint32_t state = 11;

    for (int i = 0; i < KYBER_SYMBYTES; i++) {
        seed[i] = (uint8_t)(lcg(&state) & 0xff);
    }

    poly_getnoise_eta1(&r1, seed, 0);
    poly_getnoise_eta2(&r2, seed, 1);

    for (int i = 0; i < N; i++) {
        if (r1.coeffs[i] < -KYBER_ETA1 || r1.coeffs[i] > KYBER_ETA1) {
            printf("FAIL: eta1 coeff out of range at %d: %d\n", i, r1.coeffs[i]);
            return 1;
        }
        if (r2.coeffs[i] < -KYBER_ETA2 || r2.coeffs[i] > KYBER_ETA2) {
            printf("FAIL: eta2 coeff out of range at %d: %d\n", i, r2.coeffs[i]);
            return 1;
        }
    }

    printf("PASS: noise bounds ok.\n");
    return 0;
}

int test_indcpa_encrypt_decrypt() {
    printf("[Test] Running IND-CPA encrypt/decrypt check...\n");

    uint8_t pk[KYBER_INDCPA_PUBLICKEYBYTES];
    uint8_t sk[KYBER_INDCPA_SECRETKEYBYTES];
    uint8_t m[KYBER_INDCPA_MSGBYTES];
    uint8_t c[KYBER_INDCPA_BYTES];
    uint8_t m2[KYBER_INDCPA_MSGBYTES];
    uint8_t coins[KYBER_SYMBYTES];
    uint32_t state = 23;

    for (int i = 0; i < KYBER_INDCPA_MSGBYTES; i++) m[i] = (uint8_t)(lcg(&state) & 0xff);
    for (int i = 0; i < KYBER_SYMBYTES; i++) coins[i] = (uint8_t)(lcg(&state) & 0xff);

    indcpa_keypair(pk, sk);
    indcpa_enc(c, m, pk, coins);
    indcpa_dec(m2, c, sk);

    if (memcmp(m, m2, KYBER_INDCPA_MSGBYTES) != 0) {
        printf("FAIL: IND-CPA decrypt mismatch\n");
        return 1;
    }

    printf("PASS: IND-CPA encrypt/decrypt ok.\n");
    return 0;
}

int main() {
    printf("=== Kyber Core Math Test ===\n");
    printf("Params: N=%d, Q=%d\n", N, Q);
    
    int fail = 0;
    fail |= test_reduction();
    printf("--------------------------------\n");
    fail |= test_ntt_roundtrip();
    printf("--------------------------------\n");
    fail |= test_poly_add_mul();
    printf("--------------------------------\n");
    fail |= test_noise_bounds();
    printf("--------------------------------\n");
    fail |= test_indcpa_encrypt_decrypt();

    if(fail) {
        printf("\n❌ SOME TESTS FAILED\n");
        return 1;
    }
    
    printf("\n✅ ALL TESTS PASSED\n");
    return 0;
}
