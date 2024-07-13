package arthenoid.hellwire.sampling.context;

import arthenoid.hellwire.sampling.MemoryUser;

public interface Hash extends MemoryUser {
  long toLong(long x);
  
  default long toRange(long x, long bound) {
    return Long.remainderUnsigned(toLong(x), bound);
  }
  
  default long toBits(long x, int bits) {
    return toLong(x) & (-1L >>> (Long.SIZE - bits));
  }
  
  default long toSign(long x) {
    return toRange(x, 2) * 2 - 1;
  }
}