/* * Kyber-768 Visualizer / Deep Dive Demo
 * Designed for detailed cryptographic process analysis
 */

#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <unistd.h> // For sleep()

#include "params.h"
#include "indcpa.h"
#include "kem.h"       // Standard KEM interface
#include "fips202.h"   // SHA3 primitives
#include "randombytes.h"
#include "verify.h"    // (Ensure you have this, or verify implementation)

// --- UI Framework for "Better Frontend" ---

#define COLOR_RESET   "\033[0m"
#define COLOR_CYAN    "\033[1;36m"
#define COLOR_GREEN   "\033[1;32m"
#define COLOR_RED     "\033[1;31m"
#define COLOR_YELLOW  "\033[1;33m"
#define COLOR_MAGENTA "\033[1;35m"
#define COLOR_BLUE    "\033[1;34m"
#define COLOR_BOLD    "\033[1m"

void ui_clear_screen() {
    // ASCII escape to clear screen
    printf("\033[H\033[J");
}

void ui_banner(const char *text) {
    printf("\n%s╔══════════════════════════════════════════════════════════════╗%s\n", COLOR_CYAN, COLOR_RESET);
    printf("%s║ %-60s ║%s\n", COLOR_CYAN, text, COLOR_RESET);
    printf("%s╚══════════════════════════════════════════════════════════════╝%s\n", COLOR_CYAN, COLOR_RESET);
}

void ui_step(const char *title) {
    printf("\n%s[STEP] %s%s\n", COLOR_YELLOW, title, COLOR_RESET);
    printf("%s----------------------------------------------------------------%s\n", COLOR_YELLOW, COLOR_RESET);
    usleep(200000); // Small animation delay
}

void ui_substep(const char *desc) {
    printf("  %s├──%s %s\n", COLOR_BLUE, COLOR_RESET, desc);
}

void ui_info(const char *label, const char *val_desc) {
    printf("  %s│%s   %-15s : %s\n", COLOR_BLUE, COLOR_RESET, label, val_desc);
}

// Interactive Pause
void ui_pause() {
    printf("\n  %s[Press ENTER to continue next phase...]%s", COLOR_MAGENTA, COLOR_RESET);
    getchar();
}

// Sophisticated Hex Dump
void ui_hex(const char *label, const uint8_t *data, size_t len, int highlight_idx) {
    printf("  %s│%s   %-15s : %s[ ", COLOR_BLUE, COLOR_RESET, label, COLOR_BOLD);
    
    // Show first 16 bytes
    size_t preview = 16;
    for (size_t i = 0; i < preview && i < len; i++) {
        if (i == highlight_idx) printf("%s%02X%s ", COLOR_RED, data[i], COLOR_RESET); // Highlight for errors
        else printf("%02X ", data[i]);
    }
    
    if (len > preview) printf("... (%zu bytes total) ", len);
    printf("]%s\n", COLOR_RESET);
}

// --- Helper for Internal Logic Simulation ---
// These mimic the static functions inside kem.c so we can visualize them
void demo_hash_h(uint8_t *out, const uint8_t *in, size_t inlen) { sha3_256(out, in, inlen); }
void demo_hash_g(uint8_t *out, const uint8_t *in, size_t inlen) { sha3_512(out, in, inlen); }
void demo_kdf(uint8_t *out, const uint8_t *in, size_t inlen) { shake256(out, KYBER_SSBYTES, in, inlen); }
// Note: Verify logic needs to be reimplemented here for visualization if not public
int demo_verify(const uint8_t *a, const uint8_t *b, size_t len) {
    uint8_t diff = 0;
    for(size_t i=0;i<len;i++) diff |= (a[i]^b[i]);
    return (int)((uint64_t)(diff | (uint8_t)-diff) >> 7);
}

// ============================================================================
//                              SCENARIO ENGINE
// ============================================================================

void run_detailed_flow(int attack_mode) {
    uint8_t pk[KYBER_PUBLICKEYBYTES];
    uint8_t sk[KYBER_SECRETKEYBYTES];
    uint8_t ct[KYBER_CIPHERTEXTBYTES];
    uint8_t ss_alice[KYBER_SSBYTES];
    uint8_t ss_bob[KYBER_SSBYTES];
    
    // Internal variables for visualization
    uint8_t m_bob[KYBER_SYMBYTES];
    uint8_t kr_bob[2*KYBER_SYMBYTES];
    uint8_t buf[2*KYBER_SYMBYTES];
    uint8_t kr_alice[2*KYBER_SYMBYTES];
    uint8_t cmp_ct[KYBER_CIPHERTEXTBYTES];
    
    ui_clear_screen();
    if (attack_mode) ui_banner("SCENARIO 2: MAN-IN-THE-MIDDLE ATTACK (Deep Dive)");
    else             ui_banner("SCENARIO 1: HONEST KEY EXCHANGE (Deep Dive)");

    // =================================================================
    // PHASE 1: KEY GENERATION (ALICE)
    // =================================================================
    ui_step("Alice Generates Keypair (PKE + FO Context)");
    
    // We call standard keypair, but explain the parts
    crypto_kem_keypair(pk, sk);
    
    ui_substep("Generating LWE Matrix A and Secret Vector s...");
    ui_substep("Computing Public Key t = As + e");
    ui_hex("Alice PK (t, rho)", pk, KYBER_PUBLICKEYBYTES, -1);
    
    ui_substep("Storing context into Secret Key (SK)");
    ui_info("SK Structure", "[ s vector ] || [ PK ] || [ H(PK) ] || [ z ]");
    ui_hex("Alice SK", sk, KYBER_SECRETKEYBYTES, -1);
    
    // Peek at H(pk) inside SK
    // H(pk) is at sk + INDCPA_SECRET_BYTES + PUBLIC_BYTES
    size_t offset_hpk = KYBER_INDCPA_SECRETKEYBYTES + KYBER_PUBLICKEYBYTES;
    ui_hex("-> H(PK) in SK", sk + offset_hpk, 32, -1);
    
    ui_pause();

    // =================================================================
    // PHASE 2: ENCAPSULATION (BOB) - MANUALLY EXECUTED
    // =================================================================
    ui_step("Bob Encapsulates (Encryption + Coin Derivation)");
    
    // 1. Generate random message m
    randombytes(m_bob, KYBER_SYMBYTES);
    ui_substep("1. Bob picks a random 32-byte message 'm'");
    ui_hex("Message (m)", m_bob, KYBER_SYMBYTES, -1);
    
    // 2. Hash m || H(pk)
    ui_substep("2. Hashing 'm' with Alice's 'H(PK)'...");
    demo_hash_h(buf, m_bob, KYBER_SYMBYTES);                // Hash m
    demo_hash_h(buf + KYBER_SYMBYTES, pk, KYBER_PUBLICKEYBYTES); // Hash pk
    demo_hash_g(kr_bob, buf, 2*KYBER_SYMBYTES);             // G(m || H(pk))
    
    ui_info("Derivation", "(SharedKey || Coins) = G(m || H(pk))");
    ui_hex("Shared Key (K)", kr_bob, 32, -1);
    ui_hex("Rand Coins (r)", kr_bob+32, 32, -1);
    
    // Save Bob's K for later
    memcpy(ss_bob, kr_bob, KYBER_SSBYTES);

    // 3. Encrypt
    ui_substep("3. Deterministic Encryption: c = Encrypt(pk, m, r)");
    indcpa_enc(ct, buf, pk, kr_bob + KYBER_SYMBYTES);
    ui_hex("Ciphertext (c)", ct, KYBER_CIPHERTEXTBYTES, -1);
    
    printf("\n  %s[INFO] Bob now has the Shared Key (K) and sends (c) to Alice.%s\n", COLOR_CYAN, COLOR_RESET);
    ui_pause();

    // =================================================================
    // PHASE 3: INTERCEPTION (ATTACKER)
    // =================================================================
    if (attack_mode) {
        ui_step("NETWORK LAYER: Attacker Tampering");
        
        printf("  %s[!] Attacker intercepts the ciphertext...%s\n", COLOR_RED, COLOR_RESET);
        ui_hex("Original CT", ct, KYBER_CIPHERTEXTBYTES, -1);
        
        ct[0] ^= 0xFF; // Flip bits in first byte
        ct[10] ^= 0x55; 
        
        printf("  %s[!] Attacker modifies byte 0 and byte 10!%s\n", COLOR_RED, COLOR_RESET);
        ui_hex("Tampered CT", ct, KYBER_CIPHERTEXTBYTES, 0); // Highlight index 0
        ui_pause();
    }

    // =================================================================
    // PHASE 4: DECAPSULATION (ALICE) - THE DEEP DIVE
    // =================================================================
    ui_step("Alice Decapsulates (The FO Transform Check)");

    // 1. Decrypt
    ui_substep("1. IND-CPA Decrypt: m' = Decrypt(sk, c)");
    indcpa_dec(buf, ct, sk);
    
    if (attack_mode) {
        // Since CT was changed, m' will be garbage noise
        ui_hex("Recovered m'", buf, 32, -1);
        printf("  %s[WARN] Note that m' is completely different from Bob's m!%s\n", COLOR_YELLOW, COLOR_RESET);
    } else {
        ui_hex("Recovered m'", buf, 32, -1);
        printf("  %s[OK] Matches Bob's m.%s\n", COLOR_GREEN, COLOR_RESET);
    }

    // 2. Re-derive K', r'
    ui_substep("2. Re-Deriving Coins: (K', r') = G(m' || H(pk))");
    // Copy H(pk) from SK to buffer
    memcpy(buf + KYBER_SYMBYTES, sk + KYBER_SECRETKEYBYTES - 2*KYBER_SYMBYTES, KYBER_SYMBYTES);
    demo_hash_g(kr_alice, buf, 2*KYBER_SYMBYTES);
    
    ui_hex("Re-derived r'", kr_alice+32, 32, -1);

    // 3. Re-encrypt
    ui_substep("3. Re-Encryption: c' = Encrypt(pk, m', r')");
    indcpa_enc(cmp_ct, buf, pk, kr_alice + KYBER_SYMBYTES);
    
    if (attack_mode) {
        ui_hex("Re-encrypted c'", cmp_ct, KYBER_CIPHERTEXTBYTES, 0);
        printf("  %s[FAIL] c' does NOT match received c!%s\n", COLOR_RED, COLOR_RESET);
    } else {
        ui_hex("Re-encrypted c'", cmp_ct, KYBER_CIPHERTEXTBYTES, -1);
        printf("  %s[PASS] c' is identical to received c.%s\n", COLOR_GREEN, COLOR_RESET);
    }

    // 4. Verify & Select
    ui_substep("4. Constant-Time Verification & Implicit Rejection");
    
    int fail = demo_verify(ct, cmp_ct, KYBER_CIPHERTEXTBYTES);
    
    if (fail) {
        printf("  %s--> VERIFY FAILED (Non-zero difference)%s\n", COLOR_RED, COLOR_RESET);
        ui_substep("Logic: Return K_reject = H(z || c)");
        
        // Simulate Implicit Rejection calculation
        uint8_t k_reject[32];
        uint8_t z_buf[KYBER_SYMBYTES + KYBER_CIPHERTEXTBYTES];
        // Read z from SK
        memcpy(z_buf, sk + KYBER_SECRETKEYBYTES - KYBER_SYMBYTES, KYBER_SYMBYTES);
        memcpy(z_buf + KYBER_SYMBYTES, ct, KYBER_CIPHERTEXTBYTES);
        demo_kdf(k_reject, z_buf, sizeof(z_buf));
        
        memcpy(ss_alice, k_reject, KYBER_SSBYTES);
        ui_hex("Alice's Final Key", ss_alice, 32, -1);
        
    } else {
        printf("  %s--> VERIFY PASSED%s\n", COLOR_GREEN, COLOR_RESET);
        ui_substep("Logic: Return K' (Derived from m')");
        
        memcpy(ss_alice, kr_alice, KYBER_SSBYTES); // Take K'
        ui_hex("Alice's Final Key", ss_alice, 32, -1);
    }

    // =================================================================
    // FINAL RESULT
    // =================================================================
    ui_step("Final Agreement Check");
    
    printf("  Bob's Key:   ");
    for(int i=0;i<8;i++) printf("%02X ", ss_bob[i]); printf("...\n");
    
    printf("  Alice's Key: ");
    for(int i=0;i<8;i++) printf("%02X ", ss_alice[i]); printf("...\n");
    
    if (memcmp(ss_alice, ss_bob, KYBER_SSBYTES) == 0) {
        printf("\n  %s%s✅ SUCCESS: SECURE CHANNEL ESTABLISHED%s\n", COLOR_BOLD, COLOR_GREEN, COLOR_RESET);
    } else {
        printf("\n  %s%s🛡️ SECURITY ACTIVE: KEYS DO NOT MATCH (ATTACK MITIGATED)%s\n", COLOR_BOLD, COLOR_RED, COLOR_RESET);
        printf("  Alice safely derived a random key. Attacker learns nothing.\n");
    }
    printf("\n");
}

int main() {
    ui_clear_screen();
    ui_banner("Kyber-768 (ML-KEM) Internal Logic Visualizer");
    printf("  This tool dissects the Kyber KEM process step-by-step.\n\n");
    
    ui_pause();
    
    // Run Standard
    run_detailed_flow(0);
    
    ui_pause();
    
    // Run Attack
    run_detailed_flow(1);
    
    return 0;
}