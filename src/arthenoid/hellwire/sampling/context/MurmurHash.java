package arthenoid.hellwire.sampling.context;

/**
 * Algorithm adapted from https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp
 */
public class MurmurHash implements Hash {
  protected final int seed;
  
  @Override
  public int memoryUsed() {
    return 1; //0.5
  }
  
  public MurmurHash(int seed) {
    this.seed = seed;
  }
  
  public MurmurHash(Context c) {
    this((int) c.random());
  }
  
  public static int murmur32Scramble(int k) {
    k *= 0xCC9E2D51;
    k = (k << 15) | (k >>> 17);
    k *= 0x1B873593;
    return k;
  }
  
  public int to32bits(long x) {
    int h = seed, len = Long.BYTES - (Long.numberOfLeadingZeros(x) >> 3);
    if ((x >>> 32) != 0) {
      h ^= murmur32Scramble((int) x);
      h = (h << 13) | (h >> 19);
      h = h * 5 + 0xE6546B64;
      x >>>= 32;
    }
    h ^= murmur32Scramble((int) x);
    h ^= len;
    h ^= h >>> 16;
    h *= 0x85EBCA6B;
    h ^= h >>> 13;
    h *= 0xC2B2AE35;
    h ^= h >>> 16;
    return h;
  }
  
  @Override
  public long toLong(long x) {
    return (((long) to32bits(x)) << 32) | (((long) to32bits(~x)) & 0xFFFFFFFFl);
  }
  
  @Override
  public long toRange(long x, long bound) {
    return bound > Integer.MAX_VALUE
      ? Long.remainderUnsigned(toLong(x), bound)
      : Integer.remainderUnsigned(to32bits(x), (int) bound);
  }
  
  @Override
  public long toBits(long x, int bits) {
    long mask = -1L >>> (Long.SIZE - bits);
    return (
      bits > Integer.SIZE
        ? toLong(x)
        : to32bits(x)
    ) & mask;
  }
}