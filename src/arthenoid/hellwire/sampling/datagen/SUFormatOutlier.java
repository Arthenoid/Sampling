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
  
  protected long targetFrequency(long i) {
    return i == outlier ? n : 1;
  }
  
  @Override
  public void generate(UpdateConsumer out) throws IOException {
    for (long i = 0; i < n; i++) out.update(i, targetFrequency(i));
  }
  
  @Override
  public Expectation expected(double p, long index) {
    long f = targetFrequency(index);
    return new Expectation(f, Util.pow(f, p) / (n - 1 + Util.pow(n, p)));
  }
}