package arthenoid.hellwire.sampling.datagen;

import arthenoid.hellwire.sampling.Util;
import java.io.IOException;
import java.util.Random;

public class FormatUniform extends Format {
  protected final long sqrtN;
  
  public FormatUniform(long seed, long n, long updates) {
    super(seed, n, updates);
    sqrtN = (long) Math.floor(Math.sqrt(n));
  }
  
  @Override
  public void generate(UpdateConsumer out) throws IOException {
    Random r = new Random(seed);
    for (long i = 0; i < updates; i++) out.update(Util.randomLong(r, n), Util.randomLong(r, sqrtN));
  }
  
  @Override
  public Expectation expected(double p, long index) {
    return new Expectation(updates / (2.0 * sqrtN), 1.0 / Math.min(n, updates));
  }
}