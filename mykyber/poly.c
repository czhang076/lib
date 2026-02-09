#include <stdint.h>
#include "params.h"
#include "poly.h"
#include "reduce.h"
#include "ntt.h"
#include "symmetric.h"
#include "cbd.h"

void poly_reduce(poly *r) {
  unsigned int i;
  for (i = 0; i < KYBER_N; i++) {
    r->coeffs[i] = barrett_reduce(r->coeffs[i]);
  }
}

void poly_csubq(poly *r) {
  unsigned int i;
  for (i = 0; i < KYBER_N; i++) {
    r->coeffs[i] = csubq(r->coeffs[i]);
  }
}

void poly_add(poly *r, const poly *a, const poly *b) {
  unsigned int i;
  for (i = 0; i < KYBER_N; i++) {
    r->coeffs[i] = a->coeffs[i] + b->coeffs[i];
  }
}

void poly_sub(poly *r, const poly *a, const poly *b) {
  unsigned int i;
  for (i = 0; i < KYBER_N; i++) {
    r->coeffs[i] = a->coeffs[i] - b->coeffs[i];
  }
}

void poly_compress(uint8_t r[KYBER_POLYCOMPRESSEDBYTES], const poly *a) {
  poly_tobytes(r, a);
}

void poly_decompress(poly *r, const uint8_t a[KYBER_POLYCOMPRESSEDBYTES]) {
  poly_frombytes(r, a);
}

void poly_tobytes(uint8_t r[KYBER_POLYBYTES], const poly *a) {
  unsigned int i;
  int16_t t0, t1;

  for (i = 0; i < KYBER_N / 2; i++) {
    t0 = a->coeffs[2 * i];
    t1 = a->coeffs[2 * i + 1];

    t0 += (t0 >> 15) & KYBER_Q;
    t1 += (t1 >> 15) & KYBER_Q;

    r[3 * i + 0] = (uint8_t)(t0 & 0xff);
    r[3 * i + 1] = (uint8_t)((t0 >> 8) | ((t1 & 0x0f) << 4));
    r[3 * i + 2] = (uint8_t)(t1 >> 4);
  }
}

void poly_frombytes(poly *r, const uint8_t a[KYBER_POLYBYTES]) {
  unsigned int i;

  for (i = 0; i < KYBER_N / 2; i++) {
    r->coeffs[2 * i]     = (int16_t)(a[3 * i + 0] | ((uint16_t)a[3 * i + 1] << 8)) & 0x0fff;
    r->coeffs[2 * i + 1] = (int16_t)((a[3 * i + 1] >> 4) | ((uint16_t)a[3 * i + 2] << 4)) & 0x0fff;
  }
}

void poly_frommsg(poly *r, const uint8_t msg[KYBER_INDCPA_MSGBYTES]) {
  unsigned int i, j;
  int16_t mask;

  for (i = 0; i < KYBER_INDCPA_MSGBYTES; i++) {
    for (j = 0; j < 8; j++) {
      mask = -(int16_t)((msg[i] >> j) & 1);
      r->coeffs[8 * i + j] = mask & ((KYBER_Q + 1) / 2);
    }
  }
}

void poly_tomsg(uint8_t msg[KYBER_INDCPA_MSGBYTES], const poly *r) {
  unsigned int i, j;
  int32_t t;

  for (i = 0; i < KYBER_INDCPA_MSGBYTES; i++) {
    msg[i] = 0;
    for (j = 0; j < 8; j++) {
      t = (int32_t)r->coeffs[8 * i + j];
      t += (t >> 15) & KYBER_Q;
      t <<= 1;
      t += 1665;
      t *= 80635;
      t >>= 28;
      t &= 1;
      msg[i] |= (uint8_t)(t << j);
    }
  }
}

void poly_getnoise_eta1(poly *r, const uint8_t seed[KYBER_SYMBYTES], uint8_t nonce) {
  uint8_t buf[KYBER_ETA1 * KYBER_N / 4];
  kyber_shake256_prf(buf, sizeof(buf), seed, nonce);
  poly_cbd_eta1(r, buf);
}

void poly_getnoise_eta2(poly *r, const uint8_t seed[KYBER_SYMBYTES], uint8_t nonce) {
  uint8_t buf[KYBER_ETA2 * KYBER_N / 4];
  kyber_shake256_prf(buf, sizeof(buf), seed, nonce);
  poly_cbd_eta2(r, buf);
}

void poly_ntt(poly *r) {
  (void)r;
}

void poly_invntt_tomont(poly *r) {
  (void)r;
}

void poly_mul_naive(poly *r, const poly *a, const poly *b) {
  unsigned int i, j;
  int32_t res[2 * KYBER_N] = {0};

  for(i = 0; i < KYBER_N; i++) {
    for(j = 0; j < KYBER_N; j++) {
       res[i + j] = (res[i+j] + (int32_t)a->coeffs[i] * b->coeffs[j]) % KYBER_Q;
    }
  }
  for(i = 0; i < KYBER_N; i++) {
     r->coeffs[i] = (int16_t)((res[i] - res[i + KYBER_N] + KYBER_Q) % KYBER_Q);
  }
}

void poly_basemul_montgomery(poly *r, const poly *a, const poly *b) {
  poly_mul_naive(r, a, b);
}

void poly_tomont(poly *r) {
  (void)r;
}
