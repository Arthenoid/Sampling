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
        long x = Util.randomLong(r, updates);
        if (x < n) out.update(x, 1);
          else out.update(outlier, outlierUpdate.applyAsLong(r));
      }
    } else {
      long oi = Util.randomLong(r, updates);
      for (long i = 0; i < oi; i++) out.update(Util.randomLong(r, n), 1);
      out.update(outlier, n);
      for (long i = oi + 1; i < updates; i++) out.update(Util.randomLong(r, n), 1);
    }
  }
  
  @Override
  public Expectation expected(double p, long i) {
    long w = i == outlier ? n : 1;
    return new Expectation(w, Math.pow(w, p) / (Math.min(n, updates) - 1 + Math.pow(n, p)));
  }
}