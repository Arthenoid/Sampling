package arthenoid.hellwire.sampling.structures;

import arthenoid.hellwire.sampling.MemoryUser;
import arthenoid.hellwire.sampling.Util;
import arthenoid.hellwire.sampling.context.Context;

public class SparseRecoverer implements MemoryUser {
  protected final long n, r, prime;
  protected long sum, weightedSum, polynom;
  
  @Override
  public int memoryUsed() {
    return 6;
  }
  
  public SparseRecoverer(Context context, long n) {
    if (context.getPrime() <= n * n * n) throw new IllegalArgumentException("Prime must be more than n^3.");
    this.n = n;
    r = context.randomP();
    sum = weightedSum = polynom = 0;
    prime = context.getPrime();
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
    if (index >= n || polynom != (sum * Util.powMod(r, index, prime)) % prime) return null;
    return new IntegerResult(index, sum);
  }
}