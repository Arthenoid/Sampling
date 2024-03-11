package arthenoid.hellwire.sampling.datagen;

import arthenoid.hellwire.sampling.Util;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

public class FormatUnits extends Format {
  public FormatUnits(long seed, long n, long updates) {
    super(seed, n, updates);
  }
  
  @Override
  public void generate(DataOutputStream out) throws IOException {
    Random r = new Random(seed);
    for (long i = 0; i < updates; i++) {
      out.writeLong(Util.randomLong(r, n));
      out.writeDouble(2 * n * r.nextDouble() / updates);
    }
  }
  
  @Override
  public Expectation expected(double p, long i) {
    return new Expectation(1.0, 1.0 / n);
  }
}