package arthenoid.hellwire.sampling.samplers;

import arthenoid.hellwire.sampling.MemoryUser;
import arthenoid.hellwire.sampling.Result;
import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.context.Hash;
import arthenoid.hellwire.sampling.structures.CountSketch;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.stream.Stream;

public class PrecisionSampler implements Sampler {
  @Override
  public double p() {
    return 2;
  }
  
  protected final Context context;
  protected final long n;
  protected final double δ, ε;
  protected final Random uR;
  protected final int m;
  protected final int Dt;
  protected final int Dd;
  protected final Subsampler[] subsamplers;
  
  @Override
  public int memoryUsed() {
    int m = 8 + subsamplers.length;
    for (Subsampler subsampler : subsamplers) m += subsampler.memoryUsed();
    return m;
  }
  
  protected class Subsampler implements MemoryUser {
    protected final Hash uH;
    protected final CountSketch D;
    protected final CountSketch R;
    
    @Override
    public int memoryUsed() {
      return 3 + uH.memoryUsed() + D.memoryUsed() + R.memoryUsed();
    }
    
    protected Subsampler() {
      uH = context.newHash();
      D = new CountSketch(context, Dt, Dd);
      R = new CountSketch(context, Dt, Dd);
    }
    
    protected double u(long i) {
      uR.setSeed(uH.toBits(i, Long.SIZE));
      return uR.nextDouble();
    }
    
    public void update(long i, double w) {
      D.update(i, w / Math.sqrt(u(i)));
      R.update(i, w);
    }
    
    public Result query() {
      PriorityQueue<Result> heap = new PriorityQueue<>((r1, r2) -> Double.compare(Math.abs(r1.w), Math.abs(r2.w)));
      for (long i = 0; i < m; i++) heap.add(new Result(i, D.query(i)));
      for (long i = m; i < n; i++) {
        heap.add(new Result(i, D.query(i)));
        heap.remove();
      }
      Result[] top = heap.toArray(Result[]::new);
      Result peak = top[0];
      for (int i = 0; i < m; i++) {
        D.update(top[i].i, -top[i].w);
        if (Math.abs(top[i].w) > Math.abs(peak.w)) peak = top[i];
      }
      double s = D.norm(2), r = R.norm(2);
      for (int i = 0; i < m; i++) D.update(top[i].i, top[i].w);
      return s > Math.sqrt(ε * m) * r || Math.abs(peak.w) < r / Math.sqrt(ε) ? null : new Result(peak.i, peak.w * Math.sqrt(u(peak.i)));
    }
  }
  
  public PrecisionSampler(Context context, long n, double δ, double ε) {
    this.context = context;
    this.n = n;
    this.δ = δ;
    this.ε = ε;
    uR = new Random();
    double logN = Math.log(n);
    m = (int) Math.round(50 * logN / ε);
    Dt = (int) Math.round(6 * m / logN);
    Dd = (int) Math.round(logN);
    subsamplers = new Subsampler[(int) Math.round(Math.log(1 / δ) / ε)];
    for (int i = 0; i < subsamplers.length; i++) subsamplers[i] = new Subsampler();
  }
  
  @Override
  public void update(long i, long w) {
    if (i < 0 || i >= n) throw new IllegalArgumentException("Item outside of range (" + n + "): " + i);
    double ww = w;
    for (Subsampler subsampler : subsamplers) subsampler.update(i, ww);
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