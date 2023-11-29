package arthenoid.hellwire.sampling;

import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.context.Hash;
import arthenoid.hellwire.sampling.structures.CountSketch;
import java.util.Random;

public class PrecisionSampler {
  protected final Context context;
  protected final long n;
  protected final double δ, ε;
  protected final Hash uH;
  protected final Random uR;
  protected final int k;
  protected final CountSketch D;
  
  public PrecisionSampler(Context context, long n, double δ, double ε) {
    this.context = context;
    this.n = n;
    this.δ = δ;
    this.ε = ε;
    uH = context.newHash();
    uR = new Random();
    k = (int) Math.round(650 * Math.log(n) / ε);
    D = new CountSketch(context, k, 1);
  }
  
  protected double u(long i) {
    uR.setSeed(uH.toRange(i, n));
    return (uR.nextDouble() * (n * n - 1) + 1) / (n * n);
  }
  
  public void update(long i, double w) {
    if (i < 0 || i >= n) throw new IllegalArgumentException();
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