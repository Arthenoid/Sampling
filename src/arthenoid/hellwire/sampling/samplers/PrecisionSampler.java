package arthenoid.hellwire.sampling.samplers;

import arthenoid.hellwire.sampling.RealResult;
import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.context.Hash;
import arthenoid.hellwire.sampling.structures.CountSketch;
import java.util.PriorityQueue;
import java.util.Random;

public class PrecisionSampler implements RealSampler {
  protected final Context context;
  protected final long n;
  protected final double δ, ε;
  protected final Random uR;
  protected final int m;
  protected final int Dt;
  protected final int Dd;
  protected final int repeat;
  protected final Subsampler[] subsamplers;
  
  protected class Subsampler {
    protected final Hash uH;
    protected final CountSketch D;
    protected final CountSketch R;
    
    protected Subsampler() {
      uH = context.newHash();
      D = new CountSketch(context, Dt, Dd);
      R = new CountSketch(context, Dt, Dd);
    }
    
    protected double u(long i) {
      uR.setSeed(uH.toRange(i, 1L << 30));
      return uR.nextDouble();
    }
    
    public void update(long i, double w) {
      D.update(i, w / Math.sqrt(u(i)));
      R.update(i, w);
    }
    
    public RealResult query() {
      PriorityQueue<RealResult> heap = new PriorityQueue<>((r1, r2) -> Double.compare(Math.abs(r1.weight), Math.abs(r2.weight)));
      for (long i = 0; i < m; i++) heap.add(new RealResult(i, D.query(i)));
      for (long i = m; i < n; i++) {
        heap.add(new RealResult(i, D.query(i)));
        heap.remove();
      }
      RealResult[] top = heap.toArray(RealResult[]::new);
      RealResult peak = top[0];
      for (int i = 0; i < m; i++) {
        D.update(top[i].i, -top[i].weight);
        if (Math.abs(top[i].weight) > Math.abs(peak.weight)) peak = top[i];
      }
      double s = D.norm(2), r = R.norm(2);
      for (int i = 0; i < m; i++) D.update(top[i].i, top[i].weight);
      return s > Math.sqrt(ε * m) * r || Math.abs(peak.weight) < r / Math.sqrt(ε) ? null : new RealResult(peak.i, peak.weight * Math.sqrt(u(peak.i)));
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
    repeat = (int) Math.round(Math.log(1 / δ) / ε);
    subsamplers = new Subsampler[repeat];
    for (int i = 0; i < repeat; i++) subsamplers[i] = new Subsampler();
  }
  
  @Override
  public void update(long i, double w) {
    if (i < 0 || i >= n) throw new IllegalArgumentException("Item outside of range (" + n + "): " + i);
    for (Subsampler subsampler : subsamplers) subsampler.update(i, w);
  }
  
  @Override
  public RealResult query() {
    for (Subsampler subsampler : subsamplers) {
      RealResult res = subsampler.query();
      if (res != null) return res;
    }
    return null;
  }
}