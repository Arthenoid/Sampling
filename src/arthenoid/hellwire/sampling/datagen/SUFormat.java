package arthenoid.hellwire.sampling.datagen;

public abstract class SUFormat extends Format {
  public SUFormat(long seed, long n) {
    super(seed, n, n);
  }
}