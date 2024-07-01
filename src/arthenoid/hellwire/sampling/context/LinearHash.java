package arthenoid.hellwire.sampling.context;

public class LinearHash implements Hash {
  protected final long a, b, p;
  
  @Override
  public int memoryUsed() {
    return 3;
  }
  
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
    return Long.remainderUnsigned(Long.remainderUnsigned(a * x + b, p), bound);
  }
}