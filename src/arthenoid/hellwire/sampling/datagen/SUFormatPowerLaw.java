package arthenoid.hellwire.sampling.datagen;

import arthenoid.hellwire.sampling.Util;
import arthenoid.hellwire.sampling.context.Hash;
import arthenoid.hellwire.sampling.context.MurmurHash;
import java.io.IOException;

public class SUFormatPowerLaw extends SUFormat {
  protected final Hash rh;
  protected final double exp;
  
  public SUFormatPowerLaw(long seed, long n) {
    super(seed, n);
    rh = new MurmurHash((int) (seed ^ (seed >>> 32)));
    exp = Math.log(n) / n;
  }
  
  protected long targetFrequency(long i) {
    return (long) (n / Math.exp(exp * rh.toRange(i, n)));
  }
  
  @Override
  public void generate(UpdateConsumer out) throws IOException {
    for (long i = 0; i < n; i++) out.update(i, targetFrequency(i));
  }
  
  @Override
  public Expectation expected(double p, long index) {
    long f = targetFrequency(index);
    return new Expectation(f, -Util.pow(f, p) * Math.expm1(p * -exp) / (n * n));
  }
}