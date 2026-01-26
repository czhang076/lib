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
#if (KYBER_POLYCOMPRESSEDBYTES == 128)
  unsigned int i, j;
  int16_t u;
  uint32_t d0;
  uint8_t t[8];

  for (i = 0; i < KYBER_N / 8; i++) {
    for (j = 0; j < 8; j++) {
      u = a->coeffs[8 * i + j];
      u += (u >> 15) & KYBER_Q;
      d0 = (uint32_t)u << 4;
      d0 += 1665;
      d0 *= 80635;
      d0 >>= 28;
      t[j] = d0 & 0x0f;
    }

    r[0] = (uint8_t)(t[0] | (t[1] << 4));
    r[1] = (uint8_t)(t[2] | (t[3] << 4));
    r[2] = (uint8_t)(t[4] | (t[5] << 4));
    r[3] = (uint8_t)(t[6] | (t[7] << 4));
    r += 4;
  }
#else
#error "Unsupported KYBER_POLYCOMPRESSEDBYTES"
#endif
}

void poly_decompress(poly *r, const uint8_t a[KYBER_POLYCOMPRESSEDBYTES]) {
#if (KYBER_POLYCOMPRESSEDBYTES == 128)
  unsigned int i;
  for (i = 0; i < KYBER_N / 8; i++) {
    r->coeffs[8 * i + 0] = (int16_t)(((a[0] & 0x0f) * KYBER_Q + 8) >> 4);
    r->coeffs[8 * i + 1] = (int16_t)(((a[0] >> 4) * KYBER_Q + 8) >> 4);
    r->coeffs[8 * i + 2] = (int16_t)(((a[1] & 0x0f) * KYBER_Q + 8) >> 4);
    r->coeffs[8 * i + 3] = (int16_t)(((a[1] >> 4) * KYBER_Q + 8) >> 4);
    r->coeffs[8 * i + 4] = (int16_t)(((a[2] & 0x0f) * KYBER_Q + 8) >> 4);
    r->coeffs[8 * i + 5] = (int16_t)(((a[2] >> 4) * KYBER_Q + 8) >> 4);
    r->coeffs[8 * i + 6] = (int16_t)(((a[3] & 0x0f) * KYBER_Q + 8) >> 4);
    r->coeffs[8 * i + 7] = (int16_t)(((a[3] >> 4) * KYBER_Q + 8) >> 4);
    a += 4;
  }
#else
#error "Unsupported KYBER_POLYCOMPRESSEDBYTES"
#endif
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
  ntt(r->coeffs);
  poly_reduce(r);
}

void poly_invntt_tomont(poly *r) {
  invntt(r->coeffs);
}

void poly_basemul_montgomery(poly *r, const poly *a, const poly *b) {
  unsigned int i;
  for (i = 0; i < KYBER_N/4; i++) {
    basemul(&r->coeffs[4*i], &a->coeffs[4*i], &b->coeffs[4*i], zetas[64 + i]);
    basemul(&r->coeffs[4*i+2], &a->coeffs[4*i+2], &b->coeffs[4*i+2], -zetas[64 + i]);
  }
}

void poly_tomont(poly *r) {
  unsigned int i;
  const int16_t f = (1ULL << 32) % KYBER_Q;
  for (i = 0; i < KYBER_N; i++) {
    r->coeffs[i] = montgomery_reduce((int32_t)r->coeffs[i] * f);
  }
}
