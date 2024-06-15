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
  
  protected long targetWeight(long i) {
    return rh.toRange(i, n) <= sqrtN ? sqrtN : 1;
  }
  
  @Override
  public void generate(UpdateConsumer out) throws IOException {
    for (long i = 0; i < n; i++) out.update(i, targetWeight(i));
  }
  
  @Override
  public Expectation expected(double p, long i) {
    double total = 0;
    for (long x = 0; x < n; x++) total += Math.pow(targetWeight(x), p);
    long w = targetWeight(i);
    return new Expectation(w, Math.pow(w, p) / total);
  }
}