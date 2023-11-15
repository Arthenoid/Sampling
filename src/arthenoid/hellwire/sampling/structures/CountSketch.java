package arthenoid.hellwire.sampling.structures;

import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.context.Hash;
import arthenoid.hellwire.sampling.Util;

public class CountSketch {
  protected final Context context;
  protected final int d, t;
  protected final double[][] C;
  protected final Hash[] h, g;
  
  public CountSketch(Context context, int t, int d) {
    this.context = context;
    this.t = t;
    this.d = d;
    C = new double[d][t];
    h = new Hash[d];
    g = new Hash[d];
    for (int j = 0; j < d; j++) {
      h[j] = context.staticHash(d * (d - 1) + j);
      g[j] = context.staticHash(d * d + j);
    }
  }
  
  public void update(long i, double w) {
    for (int j = 0; j < d; j++) C[j][(int) h[j].toRange(i, t)] += w * g[j].toSign(i);
  }
  
  public double query(long x) {
    double[] e = new double[d];
    for (int j = 0; j < d; j++) e[j] = C[j][(int) h[j].toRange(x, t)] * g[j].toSign(x);
    return Util.mutMedian(e);
  }
  
  public void merge(CountSketch other) {
    if (other.context != context || other.t != t || other.d != d) throw new IllegalArgumentException("Sketches have different parameters.");
    for (int j = 0; j < d; j++) for (int i = 0; i < t; i++) C[j][i] += other.C[j][i];
  }
}