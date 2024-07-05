package arthenoid.hellwire.sampling.samplers;

import arthenoid.hellwire.sampling.MemoryUser;
import arthenoid.hellwire.sampling.Result;
import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.context.Hash;
import arthenoid.hellwire.sampling.structures.CountSketch;
import arthenoid.hellwire.sampling.structures.L2Sketch;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.stream.Stream;

public class PrecisionSampler implements Sampler {
  @Override
  public double p() {
    return 2;
  }
  
  protected final long n;
  protected final double ε;
  protected final Random precisionRandom;
  protected final int sketchSize;
  protected final Subsampler[] subsamplers;
  protected final L2Sketch normSketch;
  
  @Override
  public int memoryUsed() {
    int m = 6 + subsamplers.length + normSketch.memoryUsed();
    for (Subsampler subsampler : subsamplers) m += subsampler.memoryUsed();
    return m;
  }
  
  protected class Subsampler implements MemoryUser {
    protected final Hash precisionHash;
    protected final CountSketch sketch;
    
    @Override
    public int memoryUsed() {
      return 2 + precisionHash.memoryUsed() + sketch.memoryUsed();
    }
    
    protected Subsampler(Context context, int sketchRows, int sketchColumns) {
      precisionHash = context.newHash();
      sketch = new CountSketch(context, sketchRows, sketchColumns);
    }
    
    protected double precision(long index) {
      precisionRandom.setSeed(precisionHash.toBits(index, Long.SIZE));
      return precisionRandom.nextDouble();
    }
    
    public void update(long index, double frequencyChange) {
      sketch.update(index, frequencyChange / Math.sqrt(precision(index)));
    }
    
    public Result query(double norm) {
      PriorityQueue<Result> heap = new PriorityQueue<>((r1, r2) -> Double.compare(Math.abs(r1.frequency), Math.abs(r2.frequency)));
      long topSize = Math.min(sketchSize, n);
      for (long i = 0; i < topSize; i++) heap.add(new Result(i, sketch.query(i)));
      double tailNorm = 0;
      for (long i = sketchSize; i < n; i++) {
        heap.add(new Result(i, sketch.query(i)));
        double f = heap.remove().frequency;
        tailNorm += f * f;
      }
      tailNorm = Math.sqrt(tailNorm);
      Result[] top = heap.toArray(Result[]::new);
      Result peak = top[0];
      for (int i = 1; i < topSize; i++) if (Math.abs(top[i].frequency) > Math.abs(peak.frequency)) peak = top[i];
      return tailNorm > Math.sqrt(ε * sketchSize) * norm || Math.abs(peak.frequency) < norm / Math.sqrt(ε) ? null : new Result(peak.index, peak.frequency * Math.sqrt(precision(peak.index)));
    }
  }
  
  public PrecisionSampler(Context context, long n, double δ, double ε) {
    this.n = n;
    this.ε = ε;
    precisionRandom = new Random();
    double logN = Math.log(n);
    sketchSize = (int) Math.round(50 * logN / ε);
    int
      sketchRows = (int) Math.round(logN),
      sketchColumns = (int) Math.round(6 * sketchSize / logN);
    subsamplers = new Subsampler[(int) Math.round(4 / ε)];
    for (int i = 0; i < subsamplers.length; i++) subsamplers[i] = new Subsampler(context, sketchRows, sketchColumns);
    normSketch = new L2Sketch(context, ε);
  }
  
  @Override
  public void update(long index, long frequencyChange) {
    double realFrequencyChange = frequencyChange;
    normSketch.update(index, realFrequencyChange);
    for (Subsampler subsampler : subsamplers) subsampler.update(index, realFrequencyChange);
  }
  
  @Override
  public Result query() {
    double norm = normSketch.query();
    for (Subsampler subsampler : subsamplers) {
      Result res = subsampler.query(norm);
      if (res != null) return res;
    }
    return null;
  }
  
  @Override
  public Stream<Result> queryAll() {
    double norm = normSketch.query();
    return Stream.of(subsamplers).map(s -> s.query(norm));
  }
}