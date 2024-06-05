package arthenoid.hellwire.sampling.structures;

import arthenoid.hellwire.sampling.MemoryUser;
import java.util.HashMap;
import java.util.Map;

public class MisraGries implements MemoryUser {
  protected final int k;
  protected long i, r;
  protected final Map<Long, Long> t;
  
  @Override
  public int memoryUsed() {
    return 4 + 4 * k;
  }
  
  public MisraGries(int k) {
    this.k = k;
    i = r = 0;
    t = new HashMap<>();
  }
  
  public void update(long b, long w) {
    t.merge(b, w, Long::sum);
    if (t.size() >= k) {
      long min = t.values().stream().mapToLong(Long::longValue).min().getAsLong();
      t.replaceAll((bb, ww) -> ww - min);
      t.entrySet().removeIf(bw -> bw.getValue() <= 0);
      r += min;
    }
    i += w;
  }
  
  public long queryMax() {
    return t.values().stream().mapToLong(Long::longValue).max().orElse(0) + r;
  }
}