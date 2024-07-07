package arthenoid.hellwire.sampling.structures;

import arthenoid.hellwire.sampling.MemoryUser;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

public class MisraGries implements MemoryUser {
  protected final int factor;
  protected long removed;
  protected final Map<Long, MultisetEntry> data;
  protected final NavigableSet<MultisetEntry> queue;
  
  protected static class MultisetEntry implements Comparable<MultisetEntry> {
    protected final long value;
    protected long count;
    
    public MultisetEntry(long value, long count) {
      this.value = value;
      this.count = count;
    }
    
    @Override
    public int compareTo(MultisetEntry o) {
      int c = Long.compare(count, o.count);
      return c != 0 ? c : Long.compare(value, o.value);
    }
  }
  
  @Override
  public int memoryUsed() {
    return 4 + 6 * factor;
  }
  
  public MisraGries(int factor) {
    this.factor = factor;
    removed = 0;
    data = new HashMap<>();
    queue = new TreeSet<>();
  }
  
  public void update(long index, long frequencyChange) {
    MultisetEntry entry = data.get(index);
    if (entry == null) {
      entry = new MultisetEntry(index, removed + frequencyChange);
      data.put(index, entry);
      queue.add(entry);
      if (data.size() >= factor) {
        removed = queue.first().count;
        while (!queue.isEmpty() && queue.first().count <= removed) data.remove(queue.pollFirst().value);
      }
    } else {
      queue.remove(entry);
      entry.count += frequencyChange;
      queue.add(entry);
    }
  }
  
  public long queryMax() {
    return queue.stream().mapToLong(e -> e.count).max().orElse(removed);
  }
}