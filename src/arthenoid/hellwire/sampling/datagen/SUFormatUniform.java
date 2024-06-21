package arthenoid.hellwire.sampling.datagen;

import arthenoid.hellwire.sampling.Util;
import java.io.IOException;
import java.util.Random;

public class SUFormatUniform extends SUFormat {
  protected final long sqrtN;
  
  public SUFormatUniform(long seed, long n) {
    super(seed, n);
    sqrtN = (long) Math.floor(Math.sqrt(n));
  }
  
  @Override
  public void generate(UpdateConsumer out) throws IOException {
    Random r = new Random(seed);
    for (long i = 0; i < n; i++) out.update(i, Util.randomLong(r, sqrtN));
  }
  
  @Override
  public Expectation expected(double p, long i) {
    return new Expectation(sqrtN / 2.0, 1.0 / n);
  }
}