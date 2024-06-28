package arthenoid.hellwire.sampling.structures;

import arthenoid.hellwire.sampling.MemoryUser;
import arthenoid.hellwire.sampling.Util;
import arthenoid.hellwire.sampling.context.Context;

public class SparseRecoverer implements MemoryUser {
  protected final long n, r, prime;
  protected long ℓ, z, p;
  
  @Override
  public int memoryUsed() {
    return 6;
  }
  
  public SparseRecoverer(Context context, long n) {
    if (context.getPrime() <= n * n * n) throw new IllegalArgumentException("Prime must be more than n^3.");
    this.n = n;
    r = context.randomP();
    ℓ = z = p = 0;
    prime = context.getPrime();
  }
  
  public void update(long i, long w) {
    ℓ += w;
    z += w * i;
    p = (p + w * Util.powMod(r, i, prime)) % prime;
  }
  
  public class IntegerResult {
    public final long i, w;

    public IntegerResult(long i, long w) {
      this.i = i;
      this.w = w;
    }
  }
  
  public IntegerResult query() {
    if (ℓ == 0 && z == 0 && p == 0) return new IntegerResult(0, 0);
    if (p < 0) p += prime;
    if (z % ℓ != 0) return null;
    long i = z / ℓ;
    if (i >= n || p != (ℓ * Util.powMod(r, i, prime)) % prime) return null;
    return new IntegerResult(i, ℓ);
  }
}