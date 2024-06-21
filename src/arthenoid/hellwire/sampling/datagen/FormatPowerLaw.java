package arthenoid.hellwire.sampling.datagen;

import arthenoid.hellwire.sampling.Util;
import arthenoid.hellwire.sampling.context.Hash;
import arthenoid.hellwire.sampling.context.MurmurHash;
import java.io.IOException;
import java.util.Random;

public class FormatPowerLaw extends Format {
  protected final Hash rh;
  protected final long sqrtN;
  protected final double invλ;
  
  public FormatPowerLaw(long seed, long n, long updates) {
    super(seed, n, updates);
    rh = new MurmurHash((int) (seed ^ (seed >>> 32)));
    sqrtN = (long) Math.floor(Math.sqrt(n));
    invλ = n / Math.log(n);
  }
  
  @Override
  public void generate(UpdateConsumer out) throws IOException {
    Random r = new Random(seed);
    for (long i = 0; i < updates; i++) out.update(rh.toRange((long) (-invλ * Math.log(1 - r.nextDouble())), n), Util.randomLong(r, sqrtN));
  }
  
  @Override
  public Expectation expected(double p, long i) {
    //TODO Power law expectation
    return new Expectation(0, -1);
  }
}