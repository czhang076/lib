#include <stdint.h>
#include <string.h>
#include "params.h"
#include "kem.h"
#include "indcpa.h"
#include "fips202.h"
#include "randombytes.h"

static void hash_h(uint8_t out[32], const uint8_t *in, size_t inlen) {
  sha3_256(out, in, inlen);
}

static void hash_g(uint8_t out[64], const uint8_t *in, size_t inlen) {
  sha3_512(out, in, inlen);
}

static void kdf(uint8_t out[KYBER_SSBYTES], const uint8_t *in, size_t inlen) {
  shake256(out, KYBER_SSBYTES, in, inlen);
}

static int verify(const uint8_t *a, const uint8_t *b, size_t len) {
  uint8_t diff = 0;
  for (size_t i = 0; i < len; i++) {
    diff |= (uint8_t)(a[i] ^ b[i]);
  }
  return (int)((diff | (uint8_t)(-diff)) >> 7);
}

static void cmov(uint8_t *r, const uint8_t *x, size_t len, uint8_t b) {
  b = (uint8_t)(-b);
  for (size_t i = 0; i < len; i++) {
    r[i] ^= b & (r[i] ^ x[i]);
  }
}

void crypto_kem_keypair(uint8_t pk[KYBER_PUBLICKEYBYTES], uint8_t sk[KYBER_SECRETKEYBYTES]) {
  indcpa_keypair(pk, sk);

  memcpy(sk + KYBER_INDCPA_SECRETKEYBYTES, pk, KYBER_PUBLICKEYBYTES);
  hash_h(sk + KYBER_INDCPA_SECRETKEYBYTES + KYBER_PUBLICKEYBYTES, pk, KYBER_PUBLICKEYBYTES);
  randombytes(sk + KYBER_SECRETKEYBYTES - KYBER_SYMBYTES, KYBER_SYMBYTES);
}

void crypto_kem_enc(uint8_t ct[KYBER_CIPHERTEXTBYTES], uint8_t ss[KYBER_SSBYTES], const uint8_t pk[KYBER_PUBLICKEYBYTES]) {
  uint8_t buf[2 * KYBER_SYMBYTES];
  uint8_t kr[2 * KYBER_SYMBYTES];

  randombytes(buf, KYBER_SYMBYTES);
  hash_h(buf, buf, KYBER_SYMBYTES);
  hash_h(buf + KYBER_SYMBYTES, pk, KYBER_PUBLICKEYBYTES);

  hash_g(kr, buf, 2 * KYBER_SYMBYTES);

  indcpa_enc(ct, buf, pk, kr + KYBER_SYMBYTES);

  hash_h(kr + KYBER_SYMBYTES, ct, KYBER_CIPHERTEXTBYTES);
  kdf(ss, kr, 2 * KYBER_SYMBYTES);
}

void crypto_kem_dec(uint8_t ss[KYBER_SSBYTES], const uint8_t ct[KYBER_CIPHERTEXTBYTES], const uint8_t sk[KYBER_SECRETKEYBYTES]) {
  uint8_t buf[2 * KYBER_SYMBYTES];
  uint8_t kr[2 * KYBER_SYMBYTES];
  uint8_t cmp[KYBER_CIPHERTEXTBYTES];
  uint8_t ss_reject[KYBER_SSBYTES];
  uint8_t reject_in[KYBER_SYMBYTES + KYBER_CIPHERTEXTBYTES];
  uint8_t fail;

  indcpa_dec(buf, ct, sk);

  memcpy(buf + KYBER_SYMBYTES,
         sk + KYBER_INDCPA_SECRETKEYBYTES + KYBER_PUBLICKEYBYTES,
         KYBER_SYMBYTES);
  hash_g(kr, buf, 2 * KYBER_SYMBYTES);

  indcpa_enc(cmp, buf, sk + KYBER_INDCPA_SECRETKEYBYTES, kr + KYBER_SYMBYTES);

  fail = (uint8_t)verify(ct, cmp, KYBER_CIPHERTEXTBYTES);

  hash_h(kr + KYBER_SYMBYTES, ct, KYBER_CIPHERTEXTBYTES);
  kdf(ss, kr, 2 * KYBER_SYMBYTES);

  memcpy(reject_in, sk + KYBER_SECRETKEYBYTES - KYBER_SYMBYTES, KYBER_SYMBYTES);
  memcpy(reject_in + KYBER_SYMBYTES, ct, KYBER_CIPHERTEXTBYTES);
  kdf(ss_reject, reject_in, sizeof(reject_in));

  cmov(ss, ss_reject, KYBER_SSBYTES, fail);
}
