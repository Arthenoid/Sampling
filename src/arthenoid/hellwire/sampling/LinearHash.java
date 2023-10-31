package arthenoid.hellwire.sampling;

public class LinearHash implements Hash {
  protected final long a, b, p;
  
  public LinearHash(long a, long b, long p) {
    this.a = a;
    this.b = b;
    this.p = p;
  }
  
  public LinearHash(Context c) {
    this(c.randomP(), c.randomP(), c.getPrime());
  }
  
  @Override
  public long toRange(long x, long bound) {
    return ((a * x + b) % p) % bound;
  }
}