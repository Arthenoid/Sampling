package arthenoid.hellwire.sampling;

import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.context.Hash;
import arthenoid.hellwire.sampling.structures.SparseRecoverer;

public class DistinctSampler {
  protected final Context context;
  protected final long n;
  protected final int log2n;
  protected final Hash[] h;
  protected final SparseRecoverer[] D;
  
  public DistinctSampler(Context context, long n) {
    this.context = context;
    this.n = n;
    log2n = Long.SIZE - Long.numberOfLeadingZeros(n - 1);
    h = new Hash[log2n + 1];
    D = new SparseRecoverer[log2n + 1];
    for (int ℓ = 0; ℓ <= log2n; ℓ++) {
      h[ℓ] = context.newHash();
      D[ℓ] = new SparseRecoverer(context, n);
    }
  }
  
  public void update(long i, long w) {
    if (i < 0 || i >= n) throw new IllegalArgumentException();
    for (int ℓ = 0; ℓ <= log2n; ℓ++) if (h[ℓ].toRange(i, 1 << ℓ) == 0) D[ℓ].update(i, w);
  }
  
  public IntegerResult query() {
    for (int ℓ = 0; ℓ <= log2n; ℓ++) {
      IntegerResult res = D[ℓ].query();
      if (res != null && res.weight != 0) return res;
    }
    return null;
  }
}