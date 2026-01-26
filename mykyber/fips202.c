#include <stddef.h>
#include <stdint.h>
#include <string.h>
#include "fips202.h"

#define KECCAKF_ROUNDS 24

static const uint64_t keccakf_rndc[24] = {
  0x0000000000000001ULL, 0x0000000000008082ULL,
  0x800000000000808aULL, 0x8000000080008000ULL,
  0x000000000000808bULL, 0x0000000080000001ULL,
  0x8000000080008081ULL, 0x8000000000008009ULL,
  0x000000000000008aULL, 0x0000000000000088ULL,
  0x0000000080008009ULL, 0x000000008000000aULL,
  0x000000008000808bULL, 0x800000000000008bULL,
  0x8000000000008089ULL, 0x8000000000008003ULL,
  0x8000000000008002ULL, 0x8000000000000080ULL,
  0x000000000000800aULL, 0x800000008000000aULL,
  0x8000000080008081ULL, 0x8000000000008080ULL,
  0x0000000080000001ULL, 0x8000000080008008ULL
};

static const int keccakf_rotc[24] = {
  1,  3,  6, 10, 15, 21, 28, 36, 45, 55,  2, 14,
 27, 41, 56,  8, 25, 43, 62, 18, 39, 61, 20, 44
};

static const int keccakf_piln[24] = {
 10,  7, 11, 17, 18,  3,  5, 16,  8, 21, 24,  4,
 15, 23, 19, 13, 12,  2, 20, 14, 22,  9,  6,  1
};

static inline uint64_t rol64(uint64_t x, int s) {
  return (x << s) | (x >> (64 - s));
}

static uint64_t load64(const uint8_t x[8]) {
  uint64_t r = 0;
  for (unsigned int i = 0; i < 8; i++) {
    r |= ((uint64_t)x[i]) << (8 * i);
  }
  return r;
}

static void store64(uint8_t x[8], uint64_t u) {
  for (unsigned int i = 0; i < 8; i++) {
    x[i] = (uint8_t)(u >> (8 * i));
  }
}

static void keccakf(uint64_t st[25]) {
  int i, j, round;
  uint64_t t, bc[5];

  for (round = 0; round < KECCAKF_ROUNDS; round++) {
    for (i = 0; i < 5; i++) {
      bc[i] = st[i] ^ st[i + 5] ^ st[i + 10] ^ st[i + 15] ^ st[i + 20];
    }

    for (i = 0; i < 5; i++) {
      t = bc[(i + 4) % 5] ^ rol64(bc[(i + 1) % 5], 1);
      for (j = 0; j < 25; j += 5) {
        st[j + i] ^= t;
      }
    }

    t = st[1];
    for (i = 0; i < 24; i++) {
      j = keccakf_piln[i];
      bc[0] = st[j];
      st[j] = rol64(t, keccakf_rotc[i]);
      t = bc[0];
    }

    for (j = 0; j < 25; j += 5) {
      for (i = 0; i < 5; i++) {
        bc[i] = st[j + i];
      }
      for (i = 0; i < 5; i++) {
        st[j + i] ^= (~bc[(i + 1) % 5]) & bc[(i + 2) % 5];
      }
    }

    st[0] ^= keccakf_rndc[round];
  }
}

static void keccak_absorb_once(keccak_state *st, unsigned int rate, const uint8_t *in, size_t inlen, uint8_t domain) {
  unsigned int i;
  uint8_t t[200];

  memset(st, 0, sizeof(*st));

  while (inlen >= rate) {
    for (i = 0; i < rate / 8; i++) {
      st->s[i] ^= load64(in + 8 * i);
    }
    keccakf(st->s);
    in += rate;
    inlen -= rate;
  }

  memset(t, 0, rate);
  if (inlen) {
    memcpy(t, in, inlen);
  }
  t[inlen] = domain;
  t[rate - 1] |= 0x80;

  for (i = 0; i < rate / 8; i++) {
    st->s[i] ^= load64(t + 8 * i);
  }
  st->pos = rate;
}

static void keccak_squeezeblocks(uint8_t *out, size_t nblocks, keccak_state *st, unsigned int rate) {
  unsigned int i;
  while (nblocks--) {
    keccakf(st->s);
    for (i = 0; i < rate / 8; i++) {
      store64(out + 8 * i, st->s[i]);
    }
    out += rate;
  }
}

void shake128_init(keccak_state *state) {
  memset(state, 0, sizeof(*state));
}

void shake128_absorb(keccak_state *state, const uint8_t *in, size_t inlen) {
  keccak_absorb_once(state, SHAKE128_RATE, in, inlen, 0x1F);
}

void shake128_finalize(keccak_state *state) {
  (void)state;
}

void shake128_squeeze(uint8_t *out, size_t outlen, keccak_state *state) {
  size_t nblocks = outlen / SHAKE128_RATE;
  size_t rem = outlen % SHAKE128_RATE;

  if (nblocks) {
    keccak_squeezeblocks(out, nblocks, state, SHAKE128_RATE);
    out += nblocks * SHAKE128_RATE;
  }

  if (rem) {
    uint8_t t[SHAKE128_RATE];
    keccak_squeezeblocks(t, 1, state, SHAKE128_RATE);
    memcpy(out, t, rem);
  }
}

void shake128_absorb_once(keccak_state *state, const uint8_t *in, size_t inlen) {
  keccak_absorb_once(state, SHAKE128_RATE, in, inlen, 0x1F);
}

void shake128_squeezeblocks(uint8_t *out, size_t nblocks, keccak_state *state) {
  keccak_squeezeblocks(out, nblocks, state, SHAKE128_RATE);
}

void shake256_init(keccak_state *state) {
  memset(state, 0, sizeof(*state));
}

void shake256_absorb(keccak_state *state, const uint8_t *in, size_t inlen) {
  keccak_absorb_once(state, SHAKE256_RATE, in, inlen, 0x1F);
}

void shake256_finalize(keccak_state *state) {
  (void)state;
}

void shake256_squeeze(uint8_t *out, size_t outlen, keccak_state *state) {
  size_t nblocks = outlen / SHAKE256_RATE;
  size_t rem = outlen % SHAKE256_RATE;

  if (nblocks) {
    keccak_squeezeblocks(out, nblocks, state, SHAKE256_RATE);
    out += nblocks * SHAKE256_RATE;
  }

  if (rem) {
    uint8_t t[SHAKE256_RATE];
    keccak_squeezeblocks(t, 1, state, SHAKE256_RATE);
    memcpy(out, t, rem);
  }
}

void shake256_absorb_once(keccak_state *state, const uint8_t *in, size_t inlen) {
  keccak_absorb_once(state, SHAKE256_RATE, in, inlen, 0x1F);
}

void shake256_squeezeblocks(uint8_t *out, size_t nblocks, keccak_state *state) {
  keccak_squeezeblocks(out, nblocks, state, SHAKE256_RATE);
}

void shake128(uint8_t *out, size_t outlen, const uint8_t *in, size_t inlen) {
  keccak_state st;
  keccak_absorb_once(&st, SHAKE128_RATE, in, inlen, 0x1F);
  shake128_squeeze(out, outlen, &st);
}

void shake256(uint8_t *out, size_t outlen, const uint8_t *in, size_t inlen) {
  keccak_state st;
  keccak_absorb_once(&st, SHAKE256_RATE, in, inlen, 0x1F);
  shake256_squeeze(out, outlen, &st);
}

void sha3_256(uint8_t out[32], const uint8_t *in, size_t inlen) {
  keccak_state st;
  uint8_t t[SHA3_256_RATE];
  keccak_absorb_once(&st, SHA3_256_RATE, in, inlen, 0x06);
  keccak_squeezeblocks(t, 1, &st, SHA3_256_RATE);
  memcpy(out, t, 32);
}

void sha3_512(uint8_t out[64], const uint8_t *in, size_t inlen) {
  keccak_state st;
  uint8_t t[SHA3_512_RATE];
  keccak_absorb_once(&st, SHA3_512_RATE, in, inlen, 0x06);
  keccak_squeezeblocks(t, 1, &st, SHA3_512_RATE);
  memcpy(out, t, 64);
}
