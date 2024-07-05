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
    generate((index, frequencyChange) -> {
      out.writeLong(index);
      out.writeLong(frequencyChange);
    });
  }
  
  @FunctionalInterface
  public interface UpdateConsumer {
    void update(long index, long frequencyChange) throws IOException;
  }
  
  public abstract void generate(UpdateConsumer out) throws IOException;
  
  public static class Expectation {
    public final double frequency, probability;
    
    public Expectation(double frequency, double probability) {
      this.frequency = frequency;
      this.probability = probability;
    }
  }
  
  public abstract Expectation expected(double p, long index);
}