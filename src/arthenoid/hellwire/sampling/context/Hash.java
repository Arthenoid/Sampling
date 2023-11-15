package arthenoid.hellwire.sampling.context;

public interface Hash {
  long toRange(long x, long bound);
  
  default long toSign(long x) {
    return toRange(x, 2) * 2 - 1;
  }
}