#ifndef KEM_H
#define KEM_H

#include <stdint.h>
#include "params.h"

void crypto_kem_keypair(uint8_t pk[KYBER_PUBLICKEYBYTES], uint8_t sk[KYBER_SECRETKEYBYTES]);
void crypto_kem_enc(uint8_t ct[KYBER_CIPHERTEXTBYTES], uint8_t ss[KYBER_SSBYTES], const uint8_t pk[KYBER_PUBLICKEYBYTES]);
void crypto_kem_dec(uint8_t ss[KYBER_SSBYTES], const uint8_t ct[KYBER_CIPHERTEXTBYTES], const uint8_t sk[KYBER_SECRETKEYBYTES]);

#endif
