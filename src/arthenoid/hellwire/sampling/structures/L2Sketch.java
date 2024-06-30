package arthenoid.hellwire.sampling.structures;

import arthenoid.hellwire.sampling.MemoryUser;
import arthenoid.hellwire.sampling.Util;
import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.context.Hash;
import java.util.Random;

public class L2Sketch implements MemoryUser {
  protected final int k;
  protected final double[] C, e;
  protected final Hash h;
  protected final Random r;
  
  @Override
  public int memoryUsed() {
    return 5 + k + h.memoryUsed();
  }
  
  public L2Sketch(Context context, double ε) {
    k = (int) (8 * Math.log(1 / ε));
    C = new double[k];
    e = new double[k];
    h = context.newHash();
    r = new Random();
  }
  
  public L2Sketch(L2Sketch other) {
    k = other.k;
    C = new double[k];
    e = new double[k];
    h = other.h;
    r = new Random();
  }
  
  public void update(long i, double w) {
    r.setSeed(h.toBits(i, Long.SIZE));
    for (int j = 0; j < k; j++) C[j] += r.nextGaussian() * w;
  }
  
  public double query() {
    for (int j = 0; j < k; j++) e[j] = Math.abs(C[j]);
    return Util.mutMedian(e);
  }
  
  public void merge(L2Sketch other) {
    if (other.h != h) throw new IllegalArgumentException("Sketches have different parameters.");
    for (int j = 0; j < k; j++) C[j] += other.C[j];
  }
}