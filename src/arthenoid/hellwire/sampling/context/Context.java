package arthenoid.hellwire.sampling.context;

/**
 * A source of randomness and hash functions
 */
public interface Context {
  /**
   * Provides a uniformly random {@code long} value.
   * @return A random {@code long}
   */
  long random();
  
  /**
   * Provides a uniformly random non-negative {@code long} value with the given upper bound (exclusive).
   * @param bound An exclusive upper bound
   * @return A bounded random {@code long}
   */
  long random(long bound);
  
  /**
   * Provides a uniformly random real value between zero and one.
   * @return A random real value
   */
  double randomReal();
  
  /**
   * Provides a random hash function.
   * @return A hash function
   */
  Hash newHash();
  
  /**
   * Provides a random hash function parametrised by the given key.
   * Two calls (on the same instance) with the same key are guaranteed to return the same value.
   * @param key A key
   * @return A hash function
   */
  Hash staticHash(int key);
}