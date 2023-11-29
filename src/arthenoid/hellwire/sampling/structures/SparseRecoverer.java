package arthenoid.hellwire.sampling.structures;

import arthenoid.hellwire.sampling.context.Context;

public class SparseRecoverer {
  protected final Context context;
  protected final long n, r;
  protected long ℓ, z, p;
  
  public SparseRecoverer(Context context, long n) {
    if (context.getPrime() <= n * n * n) throw new IllegalArgumentException();
    this.context = context;
    this.n = n;
    r = context.randomP();
    ℓ = z = p = 0;
  }
  
  public void update(long i, long w) {
    if (i < 0 || i >= n) throw new IllegalArgumentException();
    long prime = context.getPrime();
    ℓ = (ℓ + w) % prime;
    z = (z + w * i) % prime;
    p = (p + w * context.powP(r, i)) % prime;
  }
  
  public static class Result {
    public final long i,  weight;
    
    public Result(long i, long weight) {
      this.i = i;
      this.weight = weight;
    }
  }
  
  public Result query() {
    if (ℓ == 0 && z == 0 && p == 0) return new Result(0, 0);
    long prime = context.getPrime();
    if (ℓ < 0) ℓ += prime;
    if (z < 0) z += prime;
    if (p < 0) p += prime;
    long i = z / ℓ;
    if (i >= n || p != (ℓ * context.powP(r, i) + prime) % prime) return null;
    return new Result(i, ℓ);
  }
}