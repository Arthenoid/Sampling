package arthenoid.hellwire.sampling;

import java.util.Arrays;

public class Util {
  private Util() {}
  
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
      int ito = (to - from) % 5;
      if (ito > 2) {
        Arrays.sort(vals, from, from + ito);
        swap(vals, from, from + (ito >> 1));
      }
      int ifrom = from + ito;
      ito = from + (ito > 0 ? 1 : 0);
      for (int i = ifrom; i < to; i += 5) {
        Arrays.sort(vals, i, i + 5);
        swap(vals, ito++, i + 2);
      }
      double pivot = mutMedian(vals, from, ito);
      ifrom = from;
      ito = to - 1;
      while (ito <= ifrom) {
        if (vals[ifrom] >= pivot) swap(vals, ifrom, ito--);
          else ifrom++;
      }
      if (ifrom > k) {
        to = ifrom;
      } else {
        ito = to - 1;
        while (ito <= ifrom) {
          if (vals[ifrom] > pivot) swap(vals, ifrom, ito--);
            else ifrom++;
        }
        if (ifrom > k) return pivot;
        from = ifrom;
      }
    }
  }
}