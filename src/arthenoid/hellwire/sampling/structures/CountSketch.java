package arthenoid.hellwire.sampling.structures;

import arthenoid.hellwire.sampling.MemoryUser;
import arthenoid.hellwire.sampling.Util;
import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.context.Hash;

public class CountSketch implements MemoryUser {
  protected final Context context;
  protected final int d, t;
  protected final double[][] C;
  protected final Hash[] h, g;
  
  @Override
  public int memoryUsed() {
    int m = 5 + d * (t + 3);
    for (int i = 0; i < d; i++) m += h[i].memoryUsed() + g[i].memoryUsed();
    return m;
  }
  
  public CountSketch(Context context, int t, int d) {
    this.context = context;
    this.t = t;
    this.d = d;
    C = new double[d][t];
    h = new Hash[d];
    g = new Hash[d];
    for (int j = 0; j < d; j++) {
      h[j] = context.newHash();
      g[j] = context.newHash();
    }
  }
  
  public CountSketch(CountSketch other) {
    context = other.context;
    d = other.d;
    t = other.t;
    C = new double[d][t];
    h = other.h;
    g = other.g;
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
    if (other.context != context || other.h != h || other.g != g) throw new IllegalArgumentException("Sketches have different parameters.");
    for (int j = 0; j < d; j++) for (int i = 0; i < t; i++) C[j][i] += other.C[j][i];
  }
  
  public double norm(double p) {
    double[] e = new double[d];
    for (int j = 0; j < d; j++) {
      e[j] = 0;
      for (int i = 0; i < t; i++) e[j] += Math.pow(Math.abs(C[j][i]), p);
    }
    return Math.pow(Util.mutMedian(e), 1 / p);
  }
}