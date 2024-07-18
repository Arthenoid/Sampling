package arthenoid.hellwire.sampling.structures;

import arthenoid.hellwire.sampling.MemoryUser;
import arthenoid.hellwire.sampling.Util;
import arthenoid.hellwire.sampling.context.Context;

public class SparseRecoverer implements MemoryUser {
  protected static final long[] PRIMES = new long[] {
    151,
    36857,
    8746519,
    3445524211L,
    888858060049L,
    199279949697637L,
    51066190452443059L,
    6804604908702072427L
  };
  
  /**
   * Chooses a prime number large enough to guarantee the probability of a false positive is small enough.
   * @param n The domain size
   * @param falsePositiveProbability The maximum accepted probability
   * @return A prime
   */
  public static long getPrime(long n, double falsePositiveProbability) {
    for (long prime : PRIMES) if (prime >= n / falsePositiveProbability) return prime;
    throw new IllegalArgumentException("Large enough prime not available");
  }
  
  protected final long n, r, prime;
  protected long sum, weightedSum, polynom;
  
  @Override
  public int memoryUsed() {
    return 6;
  }
  
  public SparseRecoverer(Context context, long n, long prime) {
    this.n = n;
    r = context.random(prime);
    sum = weightedSum = polynom = 0;
    this.prime = prime;
  }
  
  public SparseRecoverer(Context context, long n, double falsePositiveProbability) {
    this(context, n, getPrime(n, falsePositiveProbability));
  }
  
  public void update(long index, long frequencyChange) {
    sum += frequencyChange;
    weightedSum += frequencyChange * index;
    polynom = (polynom + frequencyChange * Util.powMod(r, index, prime)) % prime;
  }
  
  public class IntegerResult {
    public final long index, frequency;

    public IntegerResult(long index, long frequency) {
      this.index = index;
      this.frequency = frequency;
    }
  }
  
  public IntegerResult query() {
    if (sum == 0 && weightedSum == 0 && polynom == 0) return new IntegerResult(0, 0);
    if (polynom < 0) polynom += prime;
    if (weightedSum % sum != 0) return null;
    long index = weightedSum / sum;
    if (index < 0|| index >= n || polynom != (sum * Util.powMod(r, index, prime)) % prime) return null;
    return new IntegerResult(index, sum);
  }
}