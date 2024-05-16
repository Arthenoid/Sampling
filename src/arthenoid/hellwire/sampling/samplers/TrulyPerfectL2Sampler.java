package arthenoid.hellwire.sampling.samplers;

import arthenoid.hellwire.sampling.IntegerResult;
import arthenoid.hellwire.sampling.MemoryUser;
import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.structures.MisraGries;
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
  protected final Subsampler[] subsamplers;
  protected final PriorityQueue<Subsampler> subsamplerPQ;
  protected final Map<Long, Counter> c;
  protected long t;
  protected final MisraGries mg;
  
  @Override
  public int memoryUsed() {
    int m = 7 + (int) Math.ceil(5.5 * subsamplers.length) + mg.memoryUsed();
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
        d = ctr.c - (t - tt);
        tt = (long) Math.ceil(tt / context.randomReal());
      }
    }
    
    protected IntegerResult query(long ζ) {
      long cc = c.get(s).c - d;
      return context.random(ζ) < 2 * cc + 1 ? new IntegerResult(s, cc) : null;
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
    subsamplers = new Subsampler[(int) Math.sqrt(n)];
    subsamplerPQ = new PriorityQueue<>((s1, s2) -> Long.compare(s1.tt, s2.tt));
    for (int i = 0; i < subsamplers.length; i++) subsamplerPQ.add(subsamplers[i] = new Subsampler());
    c = new HashMap<>();
    Counter ctr = new Counter();
    ctr.refs = subsamplers.length;
    c.put(-1L, ctr);
    mg = new MisraGries(subsamplers.length);
  }
  
  @Override
  public void update(long i, long w) {
    t += w;
    Counter ctr = c.get(i);
    if (ctr == null && subsamplerPQ.peek().tt > t) return;
    if (ctr == null) c.put(i, ctr = new Counter());
    ctr.c += w;
    while (subsamplerPQ.peek().tt <= t) {
      Subsampler ss = subsamplerPQ.poll();
      ss.reset(i, ctr);
      subsamplerPQ.add(ss);
    }
    mg.update(i, w);
  }
  
  @Override
  public IntegerResult query() {
    long ζ = 2 * mg.queryMax();
    for (Subsampler subsampler : subsamplers) {
      IntegerResult res = subsampler.query(ζ);
      if (res != null) return res;
    }
    return null;
  }
}