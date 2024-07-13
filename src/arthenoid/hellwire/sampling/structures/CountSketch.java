package arthenoid.hellwire.sampling.structures;

import arthenoid.hellwire.sampling.MemoryUser;
import arthenoid.hellwire.sampling.Util;
import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.context.Hash;

public class CountSketch implements MemoryUser {
  protected final int rows, columns;
  protected final double[][] data;
  protected final Hash[] hashes;
  protected final double[] query;
  
  @Override
  public int memoryUsed() {
    int m = 4 + rows * (columns + 3);
    for (int i = 0; i < rows; i++) m += hashes[i].memoryUsed();
    return m;
  }
  
  public CountSketch(Context context, int rows, int columns) {
    this.rows = rows;
    this.columns = columns;
    data = new double[rows][columns];
    hashes = new Hash[rows];
    for (int i = 0; i < rows; i++) hashes[i] = context.newHash();
    query = new double[rows];
  }
  
  public CountSketch(CountSketch other) {
    rows = other.rows;
    columns = other.columns;
    data = new double[rows][columns];
    hashes = other.hashes;
    query = new double[rows];
  }
  
  protected static int hashCell(long hash) {
    return (int) (hash >>> 1);
  }
  
  protected static double hashSign(long hash) {
    return ((hash & 1) << 1) - 1;
  }
  
  public void update(long index, double frequencyChange) {
    for (int i = 0; i < rows; i++) {
      long hash = hashes[i].toRange(index, columns << 1);
      data[i][hashCell(hash)] += hashSign(hash) * frequencyChange;
    }
  }
  
  public double query(long index) {
    for (int i = 0; i < rows; i++) {
      long hash = hashes[i].toRange(index, columns << 1);
      query[i] = data[i][hashCell(hash)] * hashSign(hash);
    }
    return Util.mutMedian(query);
  }
  
  public void merge(CountSketch other) {
    if (other.hashes != hashes) throw new IllegalArgumentException("Sketches have different parameters.");
    for (int i = 0; i < rows; i++) for (int j = 0; j < columns; j++) data[i][j] += other.data[i][j];
  }
}