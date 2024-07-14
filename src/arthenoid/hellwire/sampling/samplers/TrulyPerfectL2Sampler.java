package arthenoid.hellwire.sampling.samplers;

import arthenoid.hellwire.sampling.MemoryUser;
import arthenoid.hellwire.sampling.Result;
import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.structures.MisraGries;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Stream;

public class TrulyPerfectL2Sampler implements Sampler {
  @Override
  public double p() {
    return 2;
  }
  
  protected final Context context;
  protected final MisraGries maximumEstimator;
  protected final Subsampler[] subsamplers;
  protected final PriorityQueue<Subsampler> subsamplerPQ;
  protected final Map<Long, Counter> counters;
  protected long step;
  
  @Override
  public int memoryUsed() {
    int m = 6 + (int) Math.ceil(5.5 * subsamplers.length) + maximumEstimator.memoryUsed();
    for (Subsampler subsampler : subsamplers) m += subsampler.memoryUsed();
    return m;
  }
  
  protected class Subsampler implements MemoryUser {
    protected long selected = -1, difference, nextStep = 1;
    
    @Override
    public int memoryUsed() {
      return 3;
    }
    
    protected long nextAfter(long current) {
      return (long) Math.ceil(current / context.randomReal());
    }
    
    protected void reset(long index, Counter ctr) {
      if (index != selected) {
        if (--counters.get(selected).refs < 1) counters.remove(selected);
        selected = index;
        ctr.refs++;
      }
      long nextNextStep = nextAfter(nextStep);
      if (nextNextStep <= step) {
        nextStep += context.random(step - nextStep + 1);
        //nextNextStep += (long) Math.ceil(context.randomReal() * (s tep - nextNextStep + 1));
        nextNextStep = nextAfter(step);
      }
      difference = ctr.count - (step - nextStep);
      nextStep = nextNextStep;
    }
    
    protected Result query(long maxWeight) {
      long
        count = counters.get(selected).count - difference,
        weight = 2 * count + 1;
      return context.random(maxWeight) < weight ? new Result(selected, 1.5 * count + 0.75 + 0.25 / weight) : null;
    }
  }
  
  protected class Counter {
    protected long count = 0;
    protected int refs = 0;
  }
  
  public TrulyPerfectL2Sampler(Context context, long n, double relativeError, double absoluteError, double failureProbability) {
    this.context = context;
    step = 0;
    double sqrtN = Math.sqrt(n);
    maximumEstimator = new MisraGries((int) sqrtN);
    subsamplers = new Subsampler[(int) (4 * sqrtN * Math.log(1 / failureProbability))];
    subsamplerPQ = new PriorityQueue<>((s1, s2) -> Long.compare(s1.nextStep, s2.nextStep));
    for (int i = 0; i < subsamplers.length; i++) subsamplerPQ.add(subsamplers[i] = new Subsampler());
    counters = new HashMap<>();
    Counter ctr = new Counter();
    ctr.refs = subsamplers.length;
    counters.put(-1L, ctr);
  }
  
  @Override
  public void update(long index, long frequencyChange) {
    maximumEstimator.update(index, frequencyChange);
    step += frequencyChange;
    Counter ctr = counters.get(index);
    if (ctr == null && subsamplerPQ.peek().nextStep > step) return;
    if (ctr == null) counters.put(index, ctr = new Counter());
    ctr.count += frequencyChange;
    while (subsamplerPQ.peek().nextStep <= step) {
      Subsampler subsampler = subsamplerPQ.poll();
      subsampler.reset(index, ctr);
      subsamplerPQ.add(subsampler);
    }
  }
  
  @Override
  public Result query() {
    long maxWeight = 2 * maximumEstimator.queryMax() - 1;
    for (Subsampler subsampler : subsamplers) {
      Result res = subsampler.query(maxWeight);
      if (res != null) return res;
    }
    return null;
  }
  
  @Override
  public Stream<Result> queryAll() {
    long maxWeight = 2 * maximumEstimator.queryMax() - 1;
    return Stream.of(subsamplers).map(s -> s.query(maxWeight));
  }
}