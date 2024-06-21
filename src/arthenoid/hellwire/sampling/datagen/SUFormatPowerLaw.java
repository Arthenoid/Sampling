package arthenoid.hellwire.sampling.datagen;

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
  
  protected long targetWeight(long i) {
    return (long) (n / Math.exp(exp * rh.toRange(i, n)));
  }
  
  @Override
  public void generate(UpdateConsumer out) throws IOException {
    for (long i = 0; i < n; i++) out.update(i, targetWeight(i));
  }
  
  @Override
  public Expectation expected(double p, long i) {
    long w = targetWeight(i);
    return new Expectation(w, -Math.pow(w, p) * Math.expm1(p * -exp) / (n * n));
  }
}