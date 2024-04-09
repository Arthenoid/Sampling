package arthenoid.hellwire.sampling.datagen;

import java.io.DataOutputStream;
import java.io.IOException;

public class SUFormatUnits extends SUFormat {
  public SUFormatUnits(long seed, long n) {
    super(seed, n);
  }
  
  @Override
  public void generate(DataOutputStream out) throws IOException {
    for (long i = 0; i < n; i++) {
      out.writeLong(i);
      out.writeDouble(1);
    }
  }
  
  @Override
  public Expectation expected(double p, long i) {
    return new Expectation(1.0, 1.0 / n);
  }
}