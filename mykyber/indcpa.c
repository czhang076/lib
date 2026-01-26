#include <stdint.h>
#include <string.h>
#include "params.h"
#include "indcpa.h"
#include "poly.h"
#include "polyvec.h"
#include "symmetric.h"
#include "fips202.h"
#include "randombytes.h"

static void pack_pk(uint8_t pk[KYBER_INDCPA_PUBLICKEYBYTES], const polyvec *pkpv, const uint8_t seed[KYBER_SYMBYTES]) {
  polyvec_tobytes(pk, pkpv);
  memcpy(pk + KYBER_POLYVECBYTES, seed, KYBER_SYMBYTES);
}

static void unpack_pk(polyvec *pkpv, uint8_t seed[KYBER_SYMBYTES], const uint8_t pk[KYBER_INDCPA_PUBLICKEYBYTES]) {
  polyvec_frombytes(pkpv, pk);
  memcpy(seed, pk + KYBER_POLYVECBYTES, KYBER_SYMBYTES);
}

static void pack_sk(uint8_t sk[KYBER_INDCPA_SECRETKEYBYTES], const polyvec *skpv) {
  polyvec_tobytes(sk, skpv);
}

static void unpack_sk(polyvec *skpv, const uint8_t sk[KYBER_INDCPA_SECRETKEYBYTES]) {
  polyvec_frombytes(skpv, sk);
}

static void pack_ciphertext(uint8_t c[KYBER_INDCPA_BYTES], const polyvec *b, const poly *v) {
  polyvec_compress(c, b);
  poly_compress(c + KYBER_POLYVECCOMPRESSEDBYTES, v);
}

static void unpack_ciphertext(polyvec *b, poly *v, const uint8_t c[KYBER_INDCPA_BYTES]) {
  polyvec_decompress(b, c);
  poly_decompress(v, c + KYBER_POLYVECCOMPRESSEDBYTES);
}

static unsigned int rej_uniform(int16_t *r, unsigned int len, const uint8_t *buf, unsigned int buflen) {
  unsigned int ctr = 0, pos = 0;
  uint16_t val0, val1;

  while (ctr < len && pos + 3 <= buflen) {
    val0 = (uint16_t)buf[pos] | ((uint16_t)buf[pos + 1] << 8);
    val1 = (uint16_t)(buf[pos + 1] >> 4) | ((uint16_t)buf[pos + 2] << 4);
    pos += 3;

    val0 &= 0x0fff;
    val1 &= 0x0fff;

    if (val0 < KYBER_Q) r[ctr++] = (int16_t)val0;
    if (ctr < len && val1 < KYBER_Q) r[ctr++] = (int16_t)val1;
  }

  return ctr;
}

static void gen_matrix(polyvec *a, const uint8_t seed[KYBER_SYMBYTES], int transposed) {
  unsigned int i, j, k;
  uint8_t buf[SHAKE128_RATE];
  xof_state state;

  for (i = 0; i < KYBER_K; i++) {
    for (j = 0; j < KYBER_K; j++) {
      if (transposed) {
        kyber_shake128_absorb(&state, seed, (uint8_t)i, (uint8_t)j);
      } else {
        kyber_shake128_absorb(&state, seed, (uint8_t)j, (uint8_t)i);
      }

      k = 0;
      while (k < KYBER_N) {
        kyber_shake128_squeezeblocks(buf, 1, &state);
        k += rej_uniform(a[i].vec[j].coeffs + k, KYBER_N - k, buf, SHAKE128_RATE);
      }

      poly_ntt(&a[i].vec[j]);
    }
  }
}

void indcpa_keypair(uint8_t pk[KYBER_INDCPA_PUBLICKEYBYTES], uint8_t sk[KYBER_INDCPA_SECRETKEYBYTES]) {
  uint8_t buf[2 * KYBER_SYMBYTES];
  const uint8_t *publicseed = buf;
  const uint8_t *noiseseed = buf + KYBER_SYMBYTES;
  polyvec a[KYBER_K];
  polyvec skpv, e;
  polyvec pkpv;
  unsigned int i;
  uint8_t nonce = 0;

  randombytes(buf, KYBER_SYMBYTES);
  buf[KYBER_SYMBYTES] = KYBER_K;
  sha3_512(buf, buf, KYBER_SYMBYTES + 1);

  gen_matrix(a, publicseed, 0);

  for (i = 0; i < KYBER_K; i++) {
    poly_getnoise_eta1(&skpv.vec[i], noiseseed, nonce++);
  }
  for (i = 0; i < KYBER_K; i++) {
    poly_getnoise_eta1(&e.vec[i], noiseseed, nonce++);
  }

  polyvec_ntt(&skpv);
  polyvec_ntt(&e);

  for (i = 0; i < KYBER_K; i++) {
    polyvec_basemul_acc_montgomery(&pkpv.vec[i], &a[i], &skpv);
    poly_tomont(&pkpv.vec[i]);
  }

  polyvec_add(&pkpv, &pkpv, &e);
  polyvec_reduce(&pkpv);

  pack_sk(sk, &skpv);
  pack_pk(pk, &pkpv, publicseed);
}

void indcpa_enc(uint8_t c[KYBER_INDCPA_BYTES], const uint8_t m[KYBER_INDCPA_MSGBYTES], const uint8_t pk[KYBER_INDCPA_PUBLICKEYBYTES], const uint8_t coins[KYBER_SYMBYTES]) {
  polyvec sp, ep, at[KYBER_K], pkpv, b;
  poly v, k, epp;
  uint8_t seed[KYBER_SYMBYTES];
  unsigned int i;
  uint8_t nonce = 0;

  unpack_pk(&pkpv, seed, pk);
  poly_frommsg(&k, m);

  gen_matrix(at, seed, 1);

  for (i = 0; i < KYBER_K; i++) {
    poly_getnoise_eta1(&sp.vec[i], coins, nonce++);
  }
  for (i = 0; i < KYBER_K; i++) {
    poly_getnoise_eta2(&ep.vec[i], coins, nonce++);
  }
  poly_getnoise_eta2(&epp, coins, nonce++);

  polyvec_ntt(&sp);

  for (i = 0; i < KYBER_K; i++) {
    polyvec_basemul_acc_montgomery(&b.vec[i], &at[i], &sp);
  }

  polyvec_basemul_acc_montgomery(&v, &pkpv, &sp);

  polyvec_invntt_tomont(&b);
  poly_invntt_tomont(&v);

  polyvec_add(&b, &b, &ep);
  poly_add(&v, &v, &epp);
  poly_add(&v, &v, &k);
  polyvec_reduce(&b);
  poly_reduce(&v);

  pack_ciphertext(c, &b, &v);
}

void indcpa_dec(uint8_t m[KYBER_INDCPA_MSGBYTES], const uint8_t c[KYBER_INDCPA_BYTES], const uint8_t sk[KYBER_INDCPA_SECRETKEYBYTES]) {
  polyvec b, skpv;
  poly v, mp;

  unpack_ciphertext(&b, &v, c);
  unpack_sk(&skpv, sk);

  polyvec_ntt(&b);
  polyvec_basemul_acc_montgomery(&mp, &skpv, &b);
  poly_invntt_tomont(&mp);

  poly_sub(&mp, &v, &mp);
  poly_reduce(&mp);

  poly_tomsg(m, &mp);
}
