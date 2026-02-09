#include <stdint.h>
#include "params.h"
#include "polyvec.h"
#include "poly.h"

void polyvec_compress(uint8_t r[KYBER_POLYVECCOMPRESSEDBYTES], const polyvec *a) {
  polyvec_tobytes(r, a);
}

void polyvec_decompress(polyvec *r, const uint8_t a[KYBER_POLYVECCOMPRESSEDBYTES]) {
  polyvec_frombytes(r, a);
}

void polyvec_tobytes(uint8_t r[KYBER_POLYVECBYTES], const polyvec *a) {
  unsigned int i;
  for (i = 0; i < KYBER_K; i++) {
    poly_tobytes(r + i * KYBER_POLYBYTES, &a->vec[i]);
  }
}

void polyvec_frombytes(polyvec *r, const uint8_t a[KYBER_POLYVECBYTES]) {
  unsigned int i;
  for (i = 0; i < KYBER_K; i++) {
    poly_frombytes(&r->vec[i], a + i * KYBER_POLYBYTES);
  }
}

void polyvec_reduce(polyvec *r) {
  unsigned int i;
  for (i = 0; i < KYBER_K; i++) {
    poly_reduce(&r->vec[i]);
  }
}

void polyvec_csubq(polyvec *r) {
  unsigned int i;
  for (i = 0; i < KYBER_K; i++) {
    poly_csubq(&r->vec[i]);
  }
}

void polyvec_add(polyvec *r, const polyvec *a, const polyvec *b) {
  unsigned int i;
  for (i = 0; i < KYBER_K; i++) {
    poly_add(&r->vec[i], &a->vec[i], &b->vec[i]);
  }
}

void polyvec_ntt(polyvec *r) {
  unsigned int i;
  for (i = 0; i < KYBER_K; i++) {
    poly_ntt(&r->vec[i]);
  }
}

void polyvec_invntt_tomont(polyvec *r) {
  unsigned int i;
  for (i = 0; i < KYBER_K; i++) {
    poly_invntt_tomont(&r->vec[i]);
  }
}

void polyvec_basemul_acc_montgomery(poly *r, const polyvec *a, const polyvec *b) {
  unsigned int i;
  poly t;

  poly_basemul_montgomery(r, &a->vec[0], &b->vec[0]);
  for (i = 1; i < KYBER_K; i++) {
    poly_basemul_montgomery(&t, &a->vec[i], &b->vec[i]);
    poly_add(r, r, &t);
  }
  poly_reduce(r);
}
