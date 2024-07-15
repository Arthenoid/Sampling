package arthenoid.hellwire.sampling.cli;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;

public class Opt {
  protected final String[] names;
  
  protected boolean present = false;
  
  public Opt(String... names) {
    this.names = names;
  }
  
  public boolean present() {
    return present;
  }
  
  public static class Val<T> extends Opt {
    protected final Function<String, T> convertor;
    
    protected T value = null;
    
    public Val(Function<String, T> convertor, String... names) {
      super(names);
      this.convertor = convertor;
    }
    
    public static Val<String> newString(String... names) {
      return new Val<>(Function.identity(), names);
    }
    
    public static Val<Long> newIntegral(String... names) {
      return new Val<>(Long::parseLong, names);
    }
    
    public static Val<Long> newPositiveIntegral(String... names) {
      return new Val<>(value -> {
        long number = Long.parseLong(value);
        if (number <= 0) throw new IllegalArgumentException("Must be positive");
        return number;
      }, names);
    }
    
    public static Val<Double> newPositiveReal(String... names) {
      return new Val<>(value -> {
        double number = Double.parseDouble(value);
        if (number <= 0) throw new IllegalArgumentException("Must be positive");
        return number;
      }, names);
    }
    
    public static Val<Path> newPath(String... names) {
      return new Val<>(Path::of, names);
    }
    
    protected void convertValue(String arg) {
      value = convertor.apply(arg);
    }
    
    public T value() {
      return value;
    }
    
    public Val<T> set(T value) {
      this.value = value;
      return this;
    }
    
    public T or(T def) {
      return present ? value : def;
    }
    
    public T or(Supplier<T> def) {
      return present ? value : def.get();
    }
  }
  
  public static boolean checkExclusive(Opt... opts) {
    boolean present = false;
    for (Opt opt : opts) if (opt.present()) {
      if (present) return false;
        else present = true;
    }
    return true;
  }
  
  public static final Opt
    gen = new Opt("gen", "g"),
    time = new Opt("time", "t"),
    distribution = new Opt("distribution", "D");
  public static final Val<Path>
    in = Val.newPath("in", "i"),
    out = Val.newPath("out", "o");
  public static final Val<Long>
    period = Val.newPositiveIntegral("period", "p").set(Long.MAX_VALUE),
    domainSize = Val.newPositiveIntegral("domain-size", "n"),
    seed = Val.newIntegral("seed", "s"),
    kMer = Val.newPositiveIntegral("k-mer", "k"),
    samplers = Val.newPositiveIntegral("samplers", "m").set(1000L),
    buffer = Val.newPositiveIntegral("buffer", "b").set(1000000L);
  public static final Val<Double>
    relativeError = Val.newPositiveReal("relative-error", "r", "ε").set(1e-2),
    absoluteError = Val.newPositiveReal("absolute-error", "a", "θ").set(1e-2),
    failureProbability = Val.newPositiveReal("failure-probability", "f", "δ").set(1e-2);
  public static final Val<String> hash = Val.newString("hash", "h");
}