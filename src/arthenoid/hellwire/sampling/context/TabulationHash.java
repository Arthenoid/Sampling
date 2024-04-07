package arthenoid.hellwire.sampling.context;


public class TabulationHash implements Hash {
  protected final int t, k;
  protected final long m;
  protected final long[][] T;
  
  public TabulationHash(Context c, int t, int k) {
    this.t = t;
    this.k = k;
    m = (1L << k) - 1;
    this.T = new long[t][1 << k];
    for (long[] tab : T) for (int i = 0; i < tab.length; i++) tab[i] = c.random();
  }
  
  @Override
  public int memoryUsed() {
    return 3 + t + (t << k);
  }
  
  public long toLong(long x) {
    long h = 0;
    for (long[] tab : T) {
      h ^= tab[(int) (x & m)];
      x >>>= k;
    }
    return h;
  }
  
  @Override
  public long toRange(long x, long bound) {
    return Long.remainderUnsigned(toLong(x), bound);
  }
  
  @Override
  public long toBits(long x, int bits) {
    return toLong(x) & ((1L << bits) - 1);
  }
}
