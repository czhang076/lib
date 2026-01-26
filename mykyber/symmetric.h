#ifndef SYMMETRIC_H
#define SYMMETRIC_H

#include <stddef.h>
#include <stdint.h>
#include "params.h"
#include "fips202.h"

#define XOF_BLOCKBYTES SHAKE128_RATE
#define PRF_BLOCKBYTES SHAKE256_RATE

typedef keccak_state xof_state;

typedef keccak_state prf_state;

void kyber_shake128_absorb(xof_state *state, const uint8_t seed[KYBER_SYMBYTES], uint8_t x, uint8_t y);
void kyber_shake128_squeezeblocks(uint8_t *out, size_t nblocks, xof_state *state);

void kyber_shake256_prf(uint8_t *out, size_t outlen, const uint8_t key[KYBER_SYMBYTES], uint8_t nonce);

#endif
