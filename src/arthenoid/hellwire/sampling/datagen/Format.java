package arthenoid.hellwire.sampling.datagen;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class Format {
  public final long seed, n, updates;
  
  public Format(long seed, long n, long updates) {
    this.seed = seed;
    this.n = n;
    this.updates = updates;
  }
  
  public void generate(OutputStream out) throws IOException {
    generate(new DataOutputStream(out));
  }
  
  public void generate(DataOutputStream out) throws IOException {
    generate((x, w) -> {
      out.writeLong(x);
      out.writeLong(w);
    });
  }
  
  @FunctionalInterface
  public interface UpdateConsumer {
    void update(long x, long w) throws IOException;
  }
  
  public abstract void generate(UpdateConsumer out) throws IOException;
  
  public static class Expectation {
    public final double weight, probability;
    
    public Expectation(double weight, double probability) {
      this.weight = weight;
      this.probability = probability;
    }
  }
  
  public abstract Expectation expected(double p, long i);
}