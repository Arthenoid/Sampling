package arthenoid.hellwire.sampling;

import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.context.Hash;
import arthenoid.hellwire.sampling.structures.CountSketch;
import java.util.Random;

public class PrecisionSampler {
  protected final Context context;
  protected final long n;
  protected final double δ, ε;
  protected final Random uR;
  protected final int k;
  protected final int repeat;
  protected final Subsampler[] subsamplers;
  
  protected class Subsampler {
    protected final Hash uH;
    protected final CountSketch D;
    
    protected Subsampler() {
      uH = context.newHash();
      D = new CountSketch(context, k, 1);
    }
    
    protected double u(long i) {
      uR.setSeed(uH.toRange(i, n));
      return (uR.nextDouble() * (n * n - 1) + 1) / (n * n);
    }
    
    public void update(long i, double w) {
      D.update(i, w / Math.sqrt(u(i)));
    }
    
    public RealResult query() {
      RealResult res = null;
      for (long i = 0; i < n; i++) {
        double g2 = D.query(i);
        g2 *= g2;
        if (g2 < 4 / ε) continue;
        if (res != null) return null;
        res = new RealResult(i, g2 * u(i));
      }
      return res;
    }
  }
  
  public PrecisionSampler(Context context, long n, double δ, double ε) {
    this.context = context;
    this.n = n;
    this.δ = δ;
    this.ε = ε;
    uR = new Random();
    k = (int) Math.round(650 * Math.log(n) / ε);
    repeat = (int) Math.round(Math.log(1 / δ) / ε);
    System.out.println("!!!" + repeat);
    subsamplers = new Subsampler[repeat];
    for (int i = 0; i < repeat; i++) subsamplers[i] = new Subsampler();
  }
  
  public void update(long i, double w) {
    if (i < 0 || i >= n) throw new IllegalArgumentException();
    for (Subsampler subsampler : subsamplers) subsampler.update(i, w);
  }
  
  public RealResult query() {
    for (Subsampler subsampler : subsamplers) {
      RealResult res = subsampler.query();
      if (res != null) return res;
    }
    return null;
  }
}