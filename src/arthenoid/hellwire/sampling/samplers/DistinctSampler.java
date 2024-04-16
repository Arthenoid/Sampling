package arthenoid.hellwire.sampling.samplers;

import arthenoid.hellwire.sampling.IntegerResult;
import arthenoid.hellwire.sampling.MemoryUser;
import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.context.Hash;
import arthenoid.hellwire.sampling.structures.SparseRecoverer;

public class DistinctSampler implements IntegerSampler {
  @Override
  public double p() {
    return 0;
  }
  
  protected final Context context;
  protected final long n;
  protected final int log2n;
  protected final Subsampler[] subsamplers;
  
  @Override
  public int memoryUsed() {
    int m = 4 + subsamplers.length;
    for (Subsampler subsampler : subsamplers) m += subsampler.memoryUsed();
    return m;
  }
  
  protected class Subsampler implements MemoryUser {
    protected final Hash h;
    protected final SparseRecoverer[] D;
    
    @Override
    public int memoryUsed() {
      int m = 3 + log2n + h.memoryUsed();
      for (SparseRecoverer sparseRecoverer : D) m += sparseRecoverer.memoryUsed();
      return m;
    }
    
    public Subsampler() {
      h = context.newHash();
      D = new SparseRecoverer[log2n + 1];
      for (int ℓ = 0; ℓ <= log2n; ℓ++) {
        D[ℓ] = new SparseRecoverer(context, n);
      }
    }
    
    public void update(long i, long w) {
      D[0].update(i, w);
      int ℓ = 1;
      for (long hash = h.toBits(i, log2n); (hash & 1) > 0; hash >>>= 1) D[ℓ++].update(i, w);
    }
    
    public IntegerResult query() {
      for (int ℓ = 0; ℓ <= log2n; ℓ++) {
        IntegerResult res = D[ℓ].query();
        if (res != null && res.weight != 0) return res;
      }
      return null;
    }
  }
  
  public DistinctSampler(Context context, long n, double δ, double ε) {
    this.context = context;
    this.n = n;
    log2n = Long.SIZE - Long.numberOfLeadingZeros(n - 1);
    subsamplers = new Subsampler[log2n]; //TODO Choose DistinctSampler repetitions number
    for (int i = 0; i < subsamplers.length; i++) subsamplers[i] = new Subsampler();
  }
  
  @Override
  public void update(long i, long w) {
    if (i < 0 || i >= n) throw new IllegalArgumentException("Item outside of range (" + n + "): " + i);
    for (Subsampler subsampler : subsamplers) subsampler.update(i, w);
  }
  
  @Override
  public IntegerResult query() {
    for (Subsampler subsampler : subsamplers) {
      IntegerResult res = subsampler.query();
      if (res != null) return res;
    }
    return null;
  }
}