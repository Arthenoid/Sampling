package arthenoid.hellwire.sampling.context;

public class MultiplyShiftHash implements Hash {
  protected final long a, b;
  
  @Override
  public int memoryUsed() {
    return 2;
  }
  
  public MultiplyShiftHash(long a, long b) {
    this.a = a | 1;
    this.b = b;
  }
  
  public MultiplyShiftHash(Context c) {
    this(c.random(), c.random());
  }
  
  @Override
  public long toLong(long x) {
    return a * x + b;
  }
  
  @Override
  public long toRange(long x, long bound) {
    return Long.remainderUnsigned(Long.reverse(toLong(x)), bound);
  }
  
  @Override
  public long toBits(long x, int bits) {
    return toLong(x) >>> (Long.SIZE - bits);
  }
}