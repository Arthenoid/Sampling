package arthenoid.hellwire.sampling;

import java.util.Arrays;
import java.util.Random;
import java.util.function.ToLongFunction;

public class Util {
  private Util() {}
  
  public static double pow(double a, double b) {
    return b > 0 ? Math.pow(a, b) : a == 0 ? 0 : 1;
  }
  
  public static long powMod(long a, long b, long m) {
    long ret = 1;
    while (b > 0) {
      if ((b & 1) != 0) ret = Long.remainderUnsigned(ret * a, m);
      b >>>= 1;
      a = Long.remainderUnsigned(a * a, m);
    }
    return ret;
  }
  
  public static void swap(double[] vals, int i, int j) {
    double x = vals[i];
    vals[i] = vals[j];
    vals[j] = x;
  }
  
  public static double mutMedian(double[] vals) {
    return mutMedian(vals, 0, vals.length);
  }
  
  public static double mutSelect(int k, double[] vals) {
    return mutSelect(k, vals, 0, vals.length);
  }
  
  public static double mutMedian(double[] vals, int from, int to) {
    return mutSelect((to - from) >> 1, vals, from, to);
  }

  public static double mutSelect(int k, double[] vals, int from, int to) {
    k += from;
    for (;;) {
      if (from >= to - 1) return vals[from];
      if (to - from < 16) {
        Arrays.sort(vals, from, to);
        return vals[k];
      }
      int ito = from + (to - from) % 5;
      for (int i = ito; i < to; i += 5) {
        Arrays.sort(vals, i, i + 5);
        swap(vals, ito++, i + 2);
      }
      double pivot = mutMedian(vals, from, ito);
      int ifrom = from;
      ito = to - 1;
      while (ifrom <= ito) {
        if (vals[ifrom] >= pivot) swap(vals, ifrom, ito--);
          else ifrom++;
      }
      if (ifrom > k) to = ifrom;
        else from = ifrom;
    }
  }
  
  public static long randomLong(Random random, long bound) {
    if (bound <= Integer.MAX_VALUE) return random.nextInt((int) bound);
    long q = bound - 1, s = random.nextLong() >>> 1, r;
    while (s - (r = s % bound) + q < 0) s = random.nextLong() >>> 1;
    return r;
  }
  
  public static ToLongFunction<Random> fractionalRandomLong(long numerator, long denominator) {
    long whole = numerator / denominator;
    double fraction = (numerator % denominator) / (double) denominator;
    return whole > 0
      ? r -> Util.randomLong(r, whole) + (r.nextDouble() < fraction ? 1 : 0)
      : r -> r.nextDouble() < fraction ? 1 : 0;
  }
}