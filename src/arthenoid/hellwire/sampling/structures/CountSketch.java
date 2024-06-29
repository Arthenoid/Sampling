package arthenoid.hellwire.sampling.structures;

import arthenoid.hellwire.sampling.MemoryUser;
import arthenoid.hellwire.sampling.Util;
import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.context.Hash;

public class CountSketch implements MemoryUser {
  protected final int d, t;
  protected final double[][] C;
  protected final Hash[] h;
  protected final double[] e;
  
  @Override
  public int memoryUsed() {
    int m = 4 + d * (t + 3);
    for (int i = 0; i < d; i++) m += h[i].memoryUsed();
    return m;
  }
  
  public CountSketch(Context context, int t, int d) {
    this.t = t;
    this.d = d;
    C = new double[d][t];
    h = new Hash[d];
    for (int j = 0; j < d; j++) h[j] = context.newHash();
    e = new double[d];
  }
  
  public CountSketch(CountSketch other) {
    d = other.d;
    t = other.t;
    C = new double[d][t];
    h = other.h;
    e = new double[d];
  }
  
  protected static int hashCell(long hash) {
    return (int) (hash >> 1);
  }
  
  protected static double hashSign(long hash) {
    return ((hash & 1) << 1) - 1;
  }
  
  public void update(long i, double w) {
    for (int j = 0; j < d; j++) {
      long hash = h[j].toRange(i, t << 1);
      C[j][hashCell(hash)] += w * hashSign(hash);
    }
  }
  
  public double query(long x) {
    for (int j = 0; j < d; j++) {
      long hash = h[j].toRange(x, t << 1);
      e[j] = C[j][hashCell(hash)] * hashSign(hash);
    }
    return Util.mutMedian(e);
  }
  
  public void merge(CountSketch other) {
    if (other.h != h) throw new IllegalArgumentException("Sketches have different parameters.");
    for (int j = 0; j < d; j++) for (int i = 0; i < t; i++) C[j][i] += other.C[j][i];
  }
  
  public double norm(double p) {
    for (int j = 0; j < d; j++) {
      e[j] = 0;
      for (int i = 0; i < t; i++) e[j] += Math.pow(Math.abs(C[j][i]), p);
    }
    return Math.pow(Util.mutMedian(e), 1 / p);
  }
}