package arthenoid.hellwire.sampling.structures;

import arthenoid.hellwire.sampling.IntegerResult;
import arthenoid.hellwire.sampling.MemoryUser;
import arthenoid.hellwire.sampling.context.Context;

public class SparseRecoverer implements MemoryUser {
  protected final Context context;
  protected final long n, r;
  protected long ℓ, z, p;
  
  @Override
  public int memoryUsed() {
    return 6;
  }
  
  public SparseRecoverer(Context context, long n) {
    if (context.getPrime() <= n * n * n) throw new IllegalArgumentException("Prime must be at most n^3.");
    this.context = context;
    this.n = n;
    r = context.randomP();
    ℓ = z = p = 0;
  }
  
  public void update(long i, long w) {
    if (i < 0 || i >= n) throw new IllegalArgumentException("Item outside of range (" + n + "): " + i);
    long prime = context.getPrime();
    ℓ = (ℓ + w) % prime;
    z = (z + w * i) % prime;
    p = (p + w * context.powP(r, i)) % prime;
  }
  
  public IntegerResult query() {
    if (ℓ == 0 && z == 0 && p == 0) return new IntegerResult(0, 0);
    long prime = context.getPrime();
    if (ℓ < 0) ℓ += prime;
    if (z < 0) z += prime;
    if (p < 0) p += prime;
    long i = z / ℓ;
    if (i >= n || p != (ℓ * context.powP(r, i) + prime) % prime) return null;
    return new IntegerResult(i, ℓ);
  }
}