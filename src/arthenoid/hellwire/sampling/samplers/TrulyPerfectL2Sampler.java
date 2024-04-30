package arthenoid.hellwire.sampling.samplers;

import arthenoid.hellwire.sampling.IntegerResult;
import arthenoid.hellwire.sampling.MemoryUser;
import arthenoid.hellwire.sampling.context.Context;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class TrulyPerfectL2Sampler implements IntegerSampler {
  @Override
  public double p() {
    return 2;
  }
  
  protected final Context context;
  protected final long n;
  protected final PriorityQueue<Subsampler> subsamplers;
  protected final Map<Long, Counter> c;
  protected long t;
  
  @Override
  public int memoryUsed() {
    int m = 5 + (int) Math.ceil(4.5 * subsamplers.size());
    for (Subsampler subsampler : subsamplers) m += subsampler.memoryUsed();
    return m;
  }
  
  protected class Subsampler implements MemoryUser {
    protected long s = -1, d, tt = 1;
    
    @Override
    public int memoryUsed() {
      return 3;
    }
    
    protected void reset(long i, Counter ctr) {
      if (i != s) {
        if (--c.get(s).refs < 1) c.remove(s);
        s = i;
        ctr.refs++;
      }
      while (tt <= t) {
        d = ctr.c - (t - tt + 1);
        tt = (long) Math.ceil(tt / context.randomReal());
      }
    }
  }
  
  protected class Counter {
    protected long c = 0;
    protected int refs = 0;
  }
  
  public TrulyPerfectL2Sampler(Context context, long n, double δ, double ε) {
    this.context = context;
    this.n = n;
    t = 0;
    subsamplers = new PriorityQueue<>((s1, s2) -> Long.compare(s1.tt, s2.tt));
    for (int i = (int) Math.sqrt(n); i > 0; i--) subsamplers.add(new Subsampler());
    c = new HashMap<>();
    Counter ctr = new Counter();
    ctr.refs = subsamplers.size();
    c.put(-1L, ctr);
  }
  
  @Override
  public void update(long i, long w) {
    t += w;
    Counter ctr = c.get(i);
    if (ctr == null && subsamplers.peek().tt > t) return;
    if (ctr == null) c.put(i, ctr = new Counter());
    ctr.c += w;
    while (subsamplers.peek().tt <= t) {
      Subsampler ss = subsamplers.poll();
      ss.reset(i, ctr);
      subsamplers.add(ss);
    }
  }
  
  @Override
  public IntegerResult query() {
    return null; //TODO TPS query
  }
}