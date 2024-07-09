package arthenoid.hellwire.sampling.samplers;

import arthenoid.hellwire.sampling.MemoryUser;
import arthenoid.hellwire.sampling.Result;
import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.context.Hash;
import arthenoid.hellwire.sampling.structures.SparseRecoverer;
import java.util.stream.Stream;

public class DistinctSampler implements Sampler {
  @Override
  public double p() {
    return 0;
  }
  
  protected final long n;
  protected final int log2n;
  protected final Subsampler[] subsamplers;
  
  @Override
  public int memoryUsed() {
    int m = 3 + subsamplers.length;
    for (Subsampler subsampler : subsamplers) m += subsampler.memoryUsed();
    return m;
  }
  
  protected class Subsampler implements MemoryUser {
    protected final Hash h;
    protected final SparseRecoverer[] recoverers;
    
    @Override
    public int memoryUsed() {
      int m = 3 + log2n + h.memoryUsed();
      for (SparseRecoverer sparseRecoverer : recoverers) m += sparseRecoverer.memoryUsed();
      return m;
    }
    
    public Subsampler(Context context) {
      h = context.newHash();
      recoverers = new SparseRecoverer[log2n + 1];
      for (int ℓ = 0; ℓ <= log2n; ℓ++) recoverers[ℓ] = new SparseRecoverer(context, n);
    }
    
    public void update(long index, long frequencyChange) {
      recoverers[0].update(index, frequencyChange);
      int i = 1;
      for (long hash = h.toBits(index, log2n); (hash & 1) > 0; hash >>>= 1) recoverers[i++].update(index, frequencyChange);
    }
    
    public Result query() {
      for (int i = 0; i <= log2n; i++) {
        SparseRecoverer.IntegerResult res = recoverers[i].query();
        if (res != null && res.frequency != 0) return new Result(res.index, res.frequency);
      }
      return null;
    }
  }
  
  public DistinctSampler(Context context, long n, double relativeError, double failureProbability) {
    this.n = n;
    log2n = Long.SIZE - Long.numberOfLeadingZeros(n - 1);
    subsamplers = new Subsampler[log2n]; //TODO Choose DistinctSampler repetitions number
    for (int i = 0; i < subsamplers.length; i++) subsamplers[i] = new Subsampler(context);
  }
  
  @Override
  public void update(long index, long frequencyChange) {
    for (Subsampler subsampler : subsamplers) subsampler.update(index, frequencyChange);
  }
  
  @Override
  public Result query() {
    for (Subsampler subsampler : subsamplers) {
      Result res = subsampler.query();
      if (res != null) return res;
    }
    return null;
  }
  
  @Override
  public Stream<Result> queryAll() {
    return Stream.of(subsamplers).map(Subsampler::query);
  }
}