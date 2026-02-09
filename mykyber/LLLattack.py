import sys
import numpy as np
import random

# --- 1. Parameters (Weak Kyber) ---
N, K, Q = 16, 2, 3329
ETA = 2  # Noise parameter

# --- 2. Manual LLL Implementation ---
def lll_reduction(basis, delta=0.99):
    """
    Manual LLL Reduction using Euclidean norm.
    Input: Integer Basis Matrix (n x m)
    """
    n = basis.shape[0]
    basis = basis.astype(float)
    ortho = basis.copy() # Gram-Schmidt orthogonalized basis
    mu = np.zeros((n, n)) # Gram-Schmidt coefficients

    def update_gs(start_k):
        """Update Gram-Schmidt from row start_k"""
        for i in range(start_k, n):
            ortho[i] = basis[i]
            for j in range(i):
                mu[i, j] = np.dot(basis[i], ortho[j]) / np.dot(ortho[j], ortho[j])
                ortho[i] -= mu[i, j] * ortho[j]

    print(f"[LLL] Initializing GS for dim {n}...")
    update_gs(0)
    
    k = 1
    while k < n:
        # Progress indicator (slow in Python)
        if k % 5 == 0: print(f"\r[LLL] Processing row {k}/{n}...", end="", flush=True)
        
        # 1. Size Reduction
        for j in range(k - 1, -1, -1):
            if abs(mu[k, j]) > 0.5:
                q = round(mu[k, j])
                basis[k] -= q * basis[j]
                update_gs(k) # Re-update GS for stability
        
        # 2. Lovasz Condition
        if np.dot(ortho[k], ortho[k]) >= (delta - mu[k, k-1]**2) * np.dot(ortho[k-1], ortho[k-1]):
            k += 1
        else:
            # Swap
            basis[[k, k-1]] = basis[[k-1, k]]
            update_gs(k-1)
            k = max(k - 1, 1)
            
    print("\n[LLL] Reduction complete.")
    return basis.astype(int)

# --- 3. Math & Generation Utils ---
def poly_mul(a, b):
    """Polynomial mult mod (x^N + 1) mod Q"""
    res = np.zeros(2 * N, dtype=int)
    for i in range(N):
        for j in range(N):
            res[i + j] += a[i] * b[j]
    return [(res[i] - res[i + N]) % Q for i in range(N)]

def gen_kyber():
    """Generate Weak Kyber instance (A, s, t)"""
    print(f"[Gen] Generating instance (N={N}, K={K})...")
    
    # Generate A (Uniform), s (CBD), e (CBD)
    A = [[np.random.randint(0, Q, N) for _ in range(K)] for _ in range(K)]
    
    def cbd(): return sum(random.randint(0,1) - random.randint(0,1) for _ in range(ETA))
    s = [[cbd() for _ in range(N)] for _ in range(K)]
    e = [[cbd() for _ in range(N)] for _ in range(K)]
    
    # t = A * s + e
    t = []
    for i in range(K):
        acc = np.zeros(N, dtype=int)
        for j in range(K):
            acc = (acc + poly_mul(A[i][j], s[j])) % Q
        t.append((acc + e[i]) % Q)
    
    return A, s, t

# --- 4. Lattice Construction ---
def negacyclic(coeffs):
    """Build negacyclic matrix for poly multiplication."""
    mat = np.zeros((N, N), dtype=int)
    for i in range(N):
        # Efficient shift and negate
        col = np.roll(coeffs, i)
        col[:i] = -col[:i] # Negate wrapped elements
        mat[:, i] = col
    return mat

def build_basis(A, t):
    """Construct Primal Attack Basis (Dim: 2*K*N + 1)"""
    dim = 2 * K * N + 1
    B = np.zeros((dim, dim), dtype=int)
    kn = K * N

    # [ qI |  0 | 0 ]
    B[:kn, :kn] = np.eye(kn, dtype=int) * Q
    
    # [ A^T|  I | 0 ]
    for r in range(K):
        for c in range(K):
            # Transpose negacyclic for s * A^T
            B[kn + r*N : kn + (r+1)*N, c*N : (c+1)*N] = negacyclic(A[c][r]).T
            
    # [ 0  |  I | 0 ]
    B[kn : 2*kn, kn : 2*kn] = np.eye(kn, dtype=int)

    # [ -t |  0 | 1 ]
    B[-1, :kn] = -np.concatenate(t)
    B[-1, -1] = 1
    return B

# --- 5. Main Execution ---
if __name__ == "__main__":
    # 1. Generate Data
    A, s, t = gen_kyber()
    s_flat = np.concatenate(s)
    print(f"      Target s (first 8): {s_flat[:8]}...")
    
    # 2. Build Lattice
    lattice = build_basis(A, t)
    
    # 3. Run Manual LLL
    reduced_basis = lll_reduction(lattice)
    
    # 4. Analyze Results
    print("[Result] Searching for secret key...")
    reduced_basis = reduced_basis.astype(int)
    norms = np.linalg.norm(reduced_basis, axis=1)
    
    found = False
    for i in np.argsort(norms)[:10]: # Check top 10 shortest vectors
        row = reduced_basis[i]
        last_val = row[-1]
        
        # Check if row looks like [ e | s | 1 ]
        if abs(last_val) == 1:
            candidate = row[K*N : 2*K*N]
            if last_val == -1: candidate = -candidate # Fix sign
            
            # Verify
            if np.array_equal(candidate, s_flat):
                print(f"\n[BINGO!] Row {i:<3} | Norm {norms[i]:.2f}")
                print(f"Recovered s: {candidate[:15]}...")
                found = True
                break
            elif norms[i] < 20:
                print(f"[?] Suspicious Row {i} (Norm {norms[i]:.2f}): {candidate[:5]}...")

    if not found:
        print("\n[FAIL] Key not found. Manual LLL might need higher delta or float precision.")