package arthenoid.hellwire.sampling.structures;

import arthenoid.hellwire.sampling.MemoryUser;
import java.util.HashMap;
import java.util.Map;

public class MisraGries implements MemoryUser {
  protected final int factor;
  protected long removed;
  protected final Map<Long, Long> data;
  
  @Override
  public int memoryUsed() {
    return 3 + 4 * factor;
  }
  
  public MisraGries(int factor) {
    this.factor = factor;
    removed = 0;
    data = new HashMap<>();
  }
  
  public void update(long index, long frequenyChange) {
    data.merge(index, frequenyChange, Long::sum);
    if (data.size() >= factor) {
      long min = data.values().stream().mapToLong(Long::longValue).min().getAsLong();
      data.replaceAll((i, f) -> f - min);
      data.entrySet().removeIf(entry -> entry.getValue() <= 0);
      removed += min;
    }
  }
  
  public long queryMax() {
    return data.values().stream().mapToLong(Long::longValue).max().orElse(0) + removed;
  }
}