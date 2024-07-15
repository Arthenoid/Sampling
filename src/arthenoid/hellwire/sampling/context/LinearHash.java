package arthenoid.hellwire.sampling.context;

public class LinearHash implements Hash {
  public static final long PRIME = 1199256178700994257L;
  
  protected final long a, b;
  
  @Override
  public int memoryUsed() {
    return 2;
  }
  
  public LinearHash(long a, long b) {
    this.a = a;
    this.b = b;
  }
  
  public LinearHash(Context c) {
    this(c.random(PRIME - 1) + 1, c.random(PRIME));
  }
  
  @Override
  public long toLong(long x) {
    return Long.remainderUnsigned(a * x + b, PRIME);
  }
}