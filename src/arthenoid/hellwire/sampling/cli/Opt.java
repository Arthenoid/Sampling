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
  
  public static final Opt gen = new Opt("gen", "g");
  public static final Val<Path>
    in = Val.newPath("in", "i"),
    out = Val.newPath("out", "o");
  public static final Val<Long>
    domainSize = Val.newPositiveIntegral("domain-size", "n"),
    kMer = Val.newPositiveIntegral("k-mer", "k"),
    seed = Val.newIntegral("seed", "s"),
    period = Val.newPositiveIntegral("period", "P").set(Long.MAX_VALUE),
    prime = Val.newPositiveIntegral("prime", "p").set(1685727585142753L);
  public static final Val<Double>
    delta = Val.newPositiveReal("delta", "d", "δ").set(1e-2),
    epsilon = Val.newPositiveReal("epsilon", "e", "ε").set(1e-2);
  public static final Val<String> hash = Val.newString("hash", "h");
}