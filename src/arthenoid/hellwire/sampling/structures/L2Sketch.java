package arthenoid.hellwire.sampling.structures;

import arthenoid.hellwire.sampling.MemoryUser;
import arthenoid.hellwire.sampling.Util;
import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.context.Hash;
import java.util.Random;

public class L2Sketch implements MemoryUser {
  /** One over median of absolute value of normal standard distribution */
  public static final double INV_BETA = 1.4826022185056018;
  
  protected final int cells;
  protected final double[] data, query;
  protected final Hash h;
  protected final Random r;
  
  @Override
  public int memoryUsed() {
    return 5 + 2 * cells + h.memoryUsed();
  }
  
  public L2Sketch(Context context, double relativeError) {
    cells = (int) (8 * Math.log(1 / relativeError));
    data = new double[cells];
    query = new double[cells];
    h = context.newHash();
    r = new Random();
  }
  
  public L2Sketch(L2Sketch other) {
    cells = other.cells;
    data = new double[cells];
    query = new double[cells];
    h = other.h;
    r = new Random();
  }
  
  public void update(long index, double frequencyChange) {
    r.setSeed(h.toLong(index));
    for (int i = 0; i < cells; i++) data[i] += r.nextGaussian() * frequencyChange;
  }
  
  public double query() {
    for (int i = 0; i < cells; i++) query[i] = Math.abs(data[i]);
    return INV_BETA * Util.mutMedian(query);
  }
  
  public void merge(L2Sketch other) {
    if (other.h != h) throw new IllegalArgumentException("Sketches have different parameters.");
    for (int i = 0; i < cells; i++) data[i] += other.data[i];
  }
}