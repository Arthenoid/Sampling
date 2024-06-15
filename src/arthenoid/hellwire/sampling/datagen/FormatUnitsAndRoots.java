package arthenoid.hellwire.sampling.datagen;

import arthenoid.hellwire.sampling.Util;
import arthenoid.hellwire.sampling.context.Hash;
import arthenoid.hellwire.sampling.context.MurmurHash;
import java.io.IOException;
import java.util.Random;
import java.util.function.ToLongFunction;

public class FormatUnitsAndRoots extends Format {
  protected final Hash rh;
  protected final long sqrtN, rootSlots;
  protected final ToLongFunction<Random> rootUpdate;
  
  public FormatUnitsAndRoots(long seed, long n, long updates) {
    super(seed, n, updates);
    rh = new MurmurHash((int) (seed ^ (seed >>> 32)));
    sqrtN = (long) Math.floor(Math.sqrt(n));
    rootSlots = Math.max(sqrtN, updates - n);
    rootUpdate = Util.fractionalRandomLong(2 * (n - sqrtN), rootSlots);
  }
  
  @Override
  public void generate(UpdateConsumer out) throws IOException {
    Random r = new Random(seed);
    for (long i = 0; i < updates; i++) {
      if (Util.randomLong(r, updates) >= rootSlots) out.update(Util.randomLong(r, n), 1);
        else out.update(rh.toRange(Util.randomLong(r, sqrtN), n), rootUpdate.applyAsLong(r));
    }
  }
  
  @Override
  public Expectation expected(double p, long i) {
    double w = 1;
    for (long j = 0; j < sqrtN; j++) if (rh.toRange(j, n) == i) w += sqrtN;
    return new Expectation(w, Math.pow(w, p) / (n + sqrtN * (Math.pow(sqrtN, p) - 1)));
  }
}