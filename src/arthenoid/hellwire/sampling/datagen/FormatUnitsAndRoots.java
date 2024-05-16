package arthenoid.hellwire.sampling.datagen;

import arthenoid.hellwire.sampling.Util;
import arthenoid.hellwire.sampling.context.Hash;
import arthenoid.hellwire.sampling.context.MurmurHash;
import java.io.IOException;
import java.util.Random;

public class FormatUnitsAndRoots extends Format {
  protected final double sqrtN;
  protected final Hash rh;
  
  public FormatUnitsAndRoots(long seed, long n, long updates) {
    super(seed, n, updates);
    rh = new MurmurHash((int) (seed ^ (seed >>> 32)));
    sqrtN = Math.sqrt(n);
  }
  
  protected double targetWeight(long i) {
    return rh.toRange(i, n) <= sqrtN ? sqrtN : 1;
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
    return new Expectation(w, Math.pow(w, p) / (n + sqrtN * (Math.pow(sqrtN, p) - 1)));
  }
}