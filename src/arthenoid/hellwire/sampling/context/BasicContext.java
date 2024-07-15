package arthenoid.hellwire.sampling.context;

import arthenoid.hellwire.sampling.Util;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

public class BasicContext implements Context {
  protected final Random random;
  protected final Function<Context, Hash> hasher;
  
  protected final Map<Integer, Hash> staticHashes;
  
  public BasicContext(Random random, Function<Context, Hash> hasher) {
    this.random = random;
    this.hasher = hasher;
    staticHashes = new HashMap<>();
  }
  
  public BasicContext(long seed, Function<Context, Hash> hasher) {
    this(new Random(seed), hasher);
  }
  
  public BasicContext(Function<Context, Hash> hasher) {
    this(new Random(), hasher);
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