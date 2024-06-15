package arthenoid.hellwire.sampling.datagen;

import arthenoid.hellwire.sampling.Util;
import java.io.IOException;
import java.util.Random;

public class SUFormatOutlier extends SUFormat {
  protected final long outlier;
  
  public SUFormatOutlier(long seed, long n) {
    super(seed, n);
    outlier = Util.randomLong(new Random(~seed), n);
  }
  
  protected long targetWeight(long i) {
    return i == outlier ? n : 1;
  }
  
  @Override
  public void generate(UpdateConsumer out) throws IOException {
    for (long i = 0; i < n; i++) out.update(i, targetWeight(i));
  }
  
  @Override
  public Expectation expected(double p, long i) {
    long w = targetWeight(i);
    return new Expectation(w, Math.pow(w, p) / (n - 1 + Math.pow(n, p)));
  }
}