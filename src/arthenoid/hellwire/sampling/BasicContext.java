package arthenoid.hellwire.sampling;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BasicContext implements Context {
  final long prime;
  final Random random;
  
  final Map<Integer, Hash> staticHashes;
  
  public BasicContext(long prime, Random random) {
    this.prime = prime;
    this.random = random;
    staticHashes = new HashMap<>();
  }
  
  public BasicContext(long prime, long seed) {
    this(prime, new Random(seed));
  }
  
  public BasicContext(long prime) {
    this(prime, new Random());
  }
  
  @Override
  public long getPrime() {
    return prime;
  }
  
  @Override
  public long random(long bound) {
    if (bound <= Integer.MAX_VALUE) return random.nextInt((int) bound);
    if (bound <= 0) throw new IllegalArgumentException("Bound must be positive.");
    long q = bound - 1, s = random.nextLong() >>> 1, r;
    while (s - (r = s % bound) + q < 0) s = random.nextLong() >>> 1;
    return r;
  }
  
  @Override
  public Hash newHash() {
    return new LinearHash(this);
  }
  
  @Override
  public Hash staticHash(int key) {
    return staticHashes.computeIfAbsent(key, k -> newHash());
  }
}