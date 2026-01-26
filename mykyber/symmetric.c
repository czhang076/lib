#include <stddef.h>
#include <stdint.h>
#include "params.h"
#include "symmetric.h"
#include "fips202.h"

void kyber_shake128_absorb(xof_state *state, const uint8_t seed[KYBER_SYMBYTES], uint8_t x, uint8_t y) {
  uint8_t in[KYBER_SYMBYTES + 2];
  for (size_t i = 0; i < KYBER_SYMBYTES; i++) in[i] = seed[i];
  in[KYBER_SYMBYTES + 0] = x;
  in[KYBER_SYMBYTES + 1] = y;
  shake128_absorb_once(state, in, sizeof(in));
}

void kyber_shake128_squeezeblocks(uint8_t *out, size_t nblocks, xof_state *state) {
  shake128_squeezeblocks(out, nblocks, state);
}

void kyber_shake256_prf(uint8_t *out, size_t outlen, const uint8_t key[KYBER_SYMBYTES], uint8_t nonce) {
  uint8_t in[KYBER_SYMBYTES + 1];
  for (size_t i = 0; i < KYBER_SYMBYTES; i++) in[i] = key[i];
  in[KYBER_SYMBYTES] = nonce;
  keccak_state st;
  shake256_absorb_once(&st, in, sizeof(in));
  shake256_squeeze(out, outlen, &st);
}
