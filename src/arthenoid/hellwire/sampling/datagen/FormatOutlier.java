package arthenoid.hellwire.sampling.datagen;

import arthenoid.hellwire.sampling.Util;
import java.io.IOException;
import java.util.Random;

public class FormatOutlier extends Format {
  protected final long outlier;
  
  public FormatOutlier(long seed, long n, long updates) {
    super(seed, n, updates);
    outlier = Util.randomLong(new Random(~seed), n);
  }
  
  protected double targetWeight(long i) {
    return i == outlier ? n : 1;
  }
  
  @Override
  public void generate(UpdateConsumer out) throws IOException {
    Random r = new Random(seed);
    for (long i = 0; i < updates; i++) {
      long x = Util.randomLong(r, n);
      out.update(x, 2 * n * r.nextDouble() * targetWeight(x) / updates);
    }
  }
  
  @Override
  public Expectation expected(double p, long i) {
    double w = targetWeight(i);
    return new Expectation(w, Math.pow(w, p) / (n - 1 + Math.pow(n, p)));
  }
}