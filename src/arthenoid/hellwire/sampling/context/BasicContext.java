package arthenoid.hellwire.sampling.context;

import arthenoid.hellwire.sampling.Util;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

public class BasicContext implements Context {
  protected final long prime;
  protected final Random random;
  protected final Function<Context, Hash> hasher;
  
  protected final Map<Integer, Hash> staticHashes;
  
  public BasicContext(long prime, Random random, Function<Context, Hash> hasher) {
    this.prime = prime;
    this.random = random;
    this.hasher = hasher;
    staticHashes = new HashMap<>();
  }
  
  public BasicContext(long prime, long seed, Function<Context, Hash> hasher) {
    this(prime, new Random(seed), hasher);
  }
  
  public BasicContext(long prime, Function<Context, Hash> hasher) {
    this(prime, new Random(), hasher);
  }
  
  @Override
  public long getPrime() {
    return prime;
  }
  
  @Override
  public long random() {
    return random.nextLong();
  }
  
  @Override
  public long random(long bound) {
    return Util.randomLong(random, bound);
  }
  
  @Override
  public double randomReal() {
    return random.nextDouble();
  }
  
  @Override
  public Hash newHash() {
    return hasher.apply(this);
  }
  
  @Override
  public Hash staticHash(int key) {
    return staticHashes.computeIfAbsent(key, k -> newHash());
  }
}