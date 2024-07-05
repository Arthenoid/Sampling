package arthenoid.hellwire.sampling.datagen;

import arthenoid.hellwire.sampling.Util;
import java.io.IOException;
import java.util.Random;
import java.util.function.ToLongFunction;

public class FormatOutlier extends Format {
  protected final long outlier;
  protected final ToLongFunction<Random> outlierUpdate;
  
  public FormatOutlier(long seed, long n, long updates) {
    super(seed, n, updates);
    outlier = Util.randomLong(new Random(~seed), n);
    outlierUpdate = Util.fractionalRandomLong(2 * (n - 1), updates - n);
  }
  
  @Override
  public void generate(UpdateConsumer out) throws IOException {
    Random r = new Random(seed);
    if (updates > n) {
      for (long i = 0; i < updates; i++) {
        long index = Util.randomLong(r, updates);
        if (index < n) out.update(index, 1);
          else out.update(outlier, outlierUpdate.applyAsLong(r));
      }
    } else {
      long outlierIndex = Util.randomLong(r, updates);
      for (long i = 0; i < outlierIndex; i++) out.update(Util.randomLong(r, n), 1);
      out.update(outlier, n);
      for (long i = outlierIndex + 1; i < updates; i++) out.update(Util.randomLong(r, n), 1);
    }
  }
  
  @Override
  public Expectation expected(double p, long index) {
    long f = index == outlier ? n : 1;
    return new Expectation(f, Math.pow(f, p) / (Math.min(n, updates) - 1 + Math.pow(n, p)));
  }
}