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
  
  protected final long n;
  protected final double δ, ε;
  protected final Random uR;
  protected final int m;
  protected final int Dt;
  protected final int Dd;
  protected final Subsampler[] subsamplers;
  protected final CountSketch R;
  
  @Override
  public int memoryUsed() {
    int mem = 8 + subsamplers.length + R.memoryUsed();
    for (Subsampler subsampler : subsamplers) mem += subsampler.memoryUsed();
    return mem;
  }
  
  protected class Subsampler implements MemoryUser {
    protected final Hash uH;
    protected final CountSketch D;
    
    @Override
    public int memoryUsed() {
      return 2 + uH.memoryUsed() + D.memoryUsed();
    }
    
    protected Subsampler(Context context) {
      uH = context.newHash();
      D = new CountSketch(context, Dt, Dd);
    }
    
    protected double u(long i) {
      uR.setSeed(uH.toBits(i, Long.SIZE));
      return uR.nextDouble();
    }
    
    public void update(long i, double w) {
      D.update(i, w / Math.sqrt(u(i)));
    }
    
    public Result query(double r) {
      PriorityQueue<Result> heap = new PriorityQueue<>((r1, r2) -> Double.compare(Math.abs(r1.w), Math.abs(r2.w)));
      for (long i = 0; i < m; i++) heap.add(new Result(i, D.query(i)));
      double s = 0;
      for (long i = m; i < n; i++) {
        heap.add(new Result(i, D.query(i)));
        double w = heap.remove().w;
        s += w * w;
      }
      s = Math.sqrt(s);
      Result[] top = heap.toArray(Result[]::new);
      Result peak = top[0];
      for (int i = 1; i < m; i++) if (Math.abs(top[i].w) > Math.abs(peak.w)) peak = top[i];
      return s > Math.sqrt(ε * m) * r || Math.abs(peak.w) < r / Math.sqrt(ε) ? null : new Result(peak.i, peak.w * Math.sqrt(u(peak.i)));
    }
  }
  
  public PrecisionSampler(Context context, long n, double δ, double ε) {
    this.n = n;
    this.δ = δ;
    this.ε = ε;
    uR = new Random();
    double logN = Math.log(n);
    m = (int) Math.round(50 * logN / ε);
    Dt = (int) Math.round(6 * m / logN);
    Dd = (int) Math.round(logN);
    subsamplers = new Subsampler[(int) Math.round(Math.log(1 / δ) / ε)];
    for (int i = 0; i < subsamplers.length; i++) subsamplers[i] = new Subsampler(context);
    R = new CountSketch(context, Dt, Dd);
  }
  
  @Override
  public void update(long i, long w) {
    double ww = w;
    R.update(i, ww);
    for (Subsampler subsampler : subsamplers) subsampler.update(i, ww);
  }
  
  @Override
  public Result query() {
    double r = R.norm(2);
    for (Subsampler subsampler : subsamplers) {
      Result res = subsampler.query(r);
      if (res != null) return res;
    }
    return null;
  }
  
  @Override
  public Stream<Result> queryAll() {
    double r = R.norm(2);
    return Stream.of(subsamplers).map(s -> s.query(r));
  }
}