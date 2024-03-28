package arthenoid.hellwire.sampling.context;

import arthenoid.hellwire.sampling.MemoryUser;

public interface Hash extends MemoryUser {
  long toRange(long x, long bound);
  
  default long toSign(long x) {
    return toRange(x, 2) * 2 - 1;
  }
  
  default long toBits(long x, int bits) {
    return toRange(x, 1L << bits);
  }
}