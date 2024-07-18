package arthenoid.hellwire.sampling.context;

import arthenoid.hellwire.sampling.MemoryUser;

/**
 * A hash function
 */
public interface Hash extends MemoryUser {
  /**
   * Hashes to the maximum possible range the algorithm is capable of.
   * @param x A value to hash
   * @return A hash
   */
  long toLong(long x);
  
  /**
   * Hashes to the range from zero (inclusive) to the bound (exclusive).
   * @param x A value to hash
   * @param bound An exclusive upper bound
   * @return A hash
   */
  default long toRange(long x, long bound) {
    return Long.remainderUnsigned(toLong(x), bound);
  }
  
  /**
   * Hashes to the given number of bits.
   * @param x A value to hash
   * @param bits A number of bits specifying the target range
   * @return A hash
   */
  default long toBits(long x, int bits) {
    return toLong(x) & (-1L >>> (Long.SIZE - bits));
  }
  
  /**
   * Hashes to the set {-1, 1}.
   * @param x A value to hash
   * @return A hash
   */
  default long toSign(long x) {
    return toBits(x, 1) * 2 - 1;
  }
}