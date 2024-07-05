package arthenoid.hellwire.sampling.datagen;

import arthenoid.hellwire.sampling.context.Hash;
import arthenoid.hellwire.sampling.context.MurmurHash;
import java.io.IOException;

public class SUFormatUnitsAndRoots extends SUFormat {
  protected final long sqrtN;
  protected final Hash rh;
  
  public SUFormatUnitsAndRoots(long seed, long n) {
    super(seed, n);
    rh = new MurmurHash((int) (seed ^ (seed >>> 32)));
    sqrtN = (long) Math.floor(Math.sqrt(n));
  }
  
  protected long targetFrequency(long i) {
    return rh.toRange(i, n) <= sqrtN ? sqrtN : 1;
  }
  
  @Override
  public void generate(UpdateConsumer out) throws IOException {
    for (long i = 0; i < n; i++) out.update(i, targetFrequency(i));
  }
  
  @Override
  public Expectation expected(double p, long index) {
    double total = 0;
    for (long i = 0; i < n; i++) total += Math.pow(targetFrequency(i), p);
    long f = targetFrequency(index);
    return new Expectation(f, Math.pow(f, p) / total);
  }
}