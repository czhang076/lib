#include <stddef.h>
#include <stdint.h>
#include "verify.h"

int verify(const uint8_t *a, const uint8_t *b, size_t len) {
  uint8_t diff = 0;
  for (size_t i = 0; i < len; i++) {
    diff |= (uint8_t)(a[i] ^ b[i]);
  }
  return (int)((diff | (uint8_t)(-diff)) >> 7);
}

void cmov(uint8_t *r, const uint8_t *x, size_t len, uint8_t b) {
  b = (uint8_t)(-b);
  for (size_t i = 0; i < len; i++) {
    r[i] ^= b & (r[i] ^ x[i]);
  }
}
