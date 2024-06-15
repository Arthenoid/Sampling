package arthenoid.hellwire.sampling.datagen;

import arthenoid.hellwire.sampling.Util;
import java.io.IOException;
import java.util.Random;

public class FormatUnits extends Format {
  public FormatUnits(long seed, long n, long updates) {
    super(seed, n, updates);
  }
  
  @Override
  public void generate(UpdateConsumer out) throws IOException {
    Random r = new Random(seed);
    for (long i = 0; i < updates; i++) out.update(Util.randomLong(r, n), Util.randomLong(r, updates) < n ? 1 : 0);
  }
  
  @Override
  public Expectation expected(double p, long i) {
    return new Expectation(1.0, 1.0 / Math.min(n, updates));
  }
}