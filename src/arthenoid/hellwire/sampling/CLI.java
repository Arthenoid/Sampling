package arthenoid.hellwire.sampling;

import arthenoid.hellwire.sampling.context.BasicContext;
import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.context.Hash;
import arthenoid.hellwire.sampling.context.MurmurHash;
import arthenoid.hellwire.sampling.datagen.Format;
import arthenoid.hellwire.sampling.samplers.IntegerSampler;
import arthenoid.hellwire.sampling.samplers.RealSampler;
import arthenoid.hellwire.sampling.samplers.Sampler;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import java.util.function.Function;

public class CLI {
  protected static void die(String msg) {
    System.err.println(msg);
    System.exit(1);
  }
  
  protected static Format getFormat(String name, long seed, long n, long updates) throws Exception {
    return updates < 0
      ? ((Class<? extends Format>) Class.forName("arthenoid.hellwire.sampling.datagen.SUFormat" + name))
        .getConstructor(long.class, long.class)
        .newInstance(seed, n)
      : ((Class<? extends Format>) Class.forName("arthenoid.hellwire.sampling.datagen.Format" + name))
        .getConstructor(long.class, long.class, long.class)
        .newInstance(seed, n, updates);
  }
  
  public enum InputType {
    TEXT,
    TEST,
    K_MER;
  }
  
  public static void main(String[] args) {
    if (args.length < 1) die("Sampler not specified");
    if (args[0].equals("gen")) {
      if (args.length < 4 || args.length > 5) die("Wrong number of generator arguments (expected: <format> <domain size> <# of updates> [<seed>])");
      try {
        String name = args[1];
        long
          seed = args.length > 4 ? Long.parseLong(args[4]) : (new Random()).nextLong(),
          n = Long.parseLong(args[2]),
          updates = args[3].equals("n") ? -1 : Long.parseLong(args[3]);
        if (n <= 0) die("The domain size must be positive");
        if (!args[3].equals("n") && updates < 0) die("The # of updates can't be negative");
        Format format = getFormat(name, seed, n, updates);
        try (DataOutputStream out = new DataOutputStream(System.out)) {
          out.writeUTF(name);
          out.writeLong(seed);
          out.writeLong(n);
          out.writeLong(updates);
          format.generate(out);
        }
        return;
      } catch (Exception e) {
        die("Format not found");
        return;
      }
    }
    if (args[0].charAt(args[0].length() - 1) != ')') die("Invalid sampler specification");
    Constructor<? extends Sampler> constructor = null;
    Object[] constructorParams;
    try {
      String[] samplerArgs = args[0].split("\\(", 2);
      Class<? extends Sampler> type = (Class<? extends Sampler>) Class.forName("arthenoid.hellwire.sampling.samplers." + samplerArgs[0] + "Sampler");
      samplerArgs = samplerArgs.length > 1 ? samplerArgs[1].substring(0, samplerArgs[1].length() - 1).split("\\s*,\\s*") : new String[0];
      for (Constructor<?> cons : type.getConstructors()) {
        if (cons.getParameterCount() != samplerArgs.length + 1 || cons.getParameterTypes()[0] != Context.class) continue;
        if (constructor != null) throw new Exception("Ambiguous constructor");
        constructor = (Constructor<? extends Sampler>) cons;
      }
      if (constructor == null) throw new NoSuchElementException();
      constructorParams = new Object[constructor.getParameterCount()];
      Class<?>[] constructorParamTypes = constructor.getParameterTypes();
      for (int i = 1; i < constructorParams.length; i++) {
        if (constructorParamTypes[i] == int.class) constructorParams[i] = Integer.valueOf(samplerArgs[i - 1]);
          else if (constructorParamTypes[i] == long.class) constructorParams[i] = Long.valueOf(samplerArgs[i - 1]);
          else if (constructorParamTypes[i] == double.class) constructorParams[i] = Double.valueOf(samplerArgs[i - 1]);
          else throw new Exception("Unsupported sampler argument type");
      }
    } catch (Exception e) {
      die("Sampler not found");
      return;
    }
    InputStream ins = null;
    long period = 0, prime = 0, seed = Long.MIN_VALUE;
    Function<Context, Hash> hasher = null;
    InputType inputType = InputType.TEXT;
    int kmer = -1;
    for (int i = 1; i < args.length; i++) switch (args[i]) {
      case "period":
      case "P":
        if (period > 0) die("Period already set");
        try {
          period = Long.parseLong(args[++i]);
        } catch (Exception e) {
          die("Invalid period");
        }
        if (period <= 0) die("Period must be positive");
        break;
      case "in":
        if (ins != null) die("Input already set");
        try {
          ins = Files.newInputStream(Path.of(args[++i]));
        } catch (Exception e) {
          die("Invalid input");
        }
        break;
      case "prime":
        if (prime > 0) die("Prime already set");
        try {
          prime = Long.parseLong(args[++i]);
        } catch (Exception e) {
          die("Invalid prime");
        }
        if (prime <= 1) die("Prime must be positive");
        break;
      case "seed":
        if (seed > Long.MIN_VALUE) die("Seed already set");
        try {
          seed = Long.parseLong(args[++i]);
        } catch (Exception e) {
          die("Invalid seed");
        }
        break;
      case "hash":
      case "H":
        if (hasher != null) die("Hash already set");
        try {
          Constructor<? extends Hash> hCons = ((Class<? extends Hash>) Class.forName("arthenoid.hellwire.sampling.context." + args[++i] + "Hash")).getConstructor(Context.class);
          if (hCons == null) throw new NoSuchElementException();
          hasher = c -> {
            try {
              return hCons.newInstance(c);
            } catch (Exception e) {
              return null;
            }
          };
        } catch (Exception e) {
          die("Hash not found");
        }
        break;
      case "test":
      case "T":
        if (inputType != InputType.TEXT) die("Input type already set");
        inputType = InputType.TEST;
        break;
      case "kmer":
      case "K":
        if (inputType != InputType.TEXT) die("Input type already set");
        inputType = InputType.K_MER;
        try {
          kmer = Integer.parseInt(args[++i]);
        } catch (Exception e) {
          die("Invalid k-mer k");
        }
        break;
      default:
        die("Unknown argument: " + args[i]);
    }
    if (prime == 0) prime = 1685727585142753L;
    if (hasher == null) hasher = MurmurHash::new;
    if (ins == null) ins = System.in;
    constructorParams[0] = seed == Long.MIN_VALUE ? new BasicContext(prime, hasher) : new BasicContext(prime, seed, hasher);
    Sampler sampler;
    try {
      sampler = constructor.newInstance(constructorParams);
    } catch (Exception e) {
      die("Sampler cannot be initialised");
      return;
    }
    System.out.println("Sampler memory usage: " + sampler.memoryUsed());
    boolean integer = sampler instanceof IntegerSampler;
    IntegerSampler integerSampler;
    RealSampler realSampler;
    if (integer) {
      integerSampler = (IntegerSampler) sampler;
      realSampler = null;
    } else {
      integerSampler = null;
      realSampler = (RealSampler) sampler;
    }
    InputProccessor ip;
    switch (inputType) {
      case TEXT:
        ip = new TextIP(ins);
        break;
      case TEST:
        TestIP tip;
        try {
          ip = tip = new TestIP(ins);
        } catch (Exception e) {
          die("Invalid data format");
          return;
        }
        System.out.println("Test input format: " + tip.name + " " + tip.n + " " + tip.updates + " " + tip.seed);
        break;
      case K_MER:
        try {
          ip = new KmerIP(ins, kmer, true);
        } catch (Exception e) {
          die("Invalid input");
          return;
        }
        break;
      default:
        throw new AssertionError();
    }
    long i = 0;
    try {
      while (ip.hasData()) {
        if (integer) ip.update(integerSampler);
          else ip.update(realSampler);
        i++;
        if (period > 0 && i % period == 0) System.out.println("After " + i + " updates: " + formatQuery(sampler, ip));
      }
    } catch (IOException e) {
      die("IO exception");
      return;
    }
    Result result = sampler.query();
    System.out.println("Final (after " + i + " updates): " + formatResult(result, ip));
    if (inputType == InputType.TEST && result != null) {
      Format.Expectation expected = ((TestIP) ip).format.expected(sampler.p(), result.i);
      System.out.println("This index was expected with probability " + expected.probability + " and frequency around " + expected.weight);
    }
  }
  
  protected static String formatResult(Result result, InputProccessor ip) {
    return result == null ? "QUERY FAILED" : ("(" + ip.decode(result.i) + ", " + result.getWeight() + ")");
  }
  
  protected static String formatQuery(Sampler sampler, InputProccessor ip) {
    return formatResult(sampler.query(), ip);
  }
  
  protected interface InputProccessor {
    boolean hasData();
    void update(IntegerSampler sampler) throws IOException;
    void update(RealSampler sampler) throws IOException;
    default String decode(long x) {
      return Long.toString(x);
    }
  }
  
  protected static class TextIP implements InputProccessor {
    protected final Scanner in;
    
    public TextIP(InputStream ins) {
      in = new Scanner(ins, StandardCharsets.UTF_8);
    }
    
    @Override
    public boolean hasData() {
      return in.hasNext();
    }
    
    @Override
    public void update(IntegerSampler sampler) {
      sampler.update(in.nextLong(), in.nextLong());
    }
    
    @Override
    public void update(RealSampler sampler) {
      sampler.update(in.nextLong(), in.nextDouble());
    }
  }
  
  protected static class TestIP implements InputProccessor {
    protected final DataInputStream in;
    protected int i = 0;
    protected long x;
    protected double w;
    
    public final String name;
    public final long seed, n, updates;
    public final Format format;
    
    public TestIP(InputStream ins) throws Exception {
      in = new DataInputStream(ins);
      long u;
      format = getFormat(name = in.readUTF(), seed = in.readLong(), n = in.readLong(), u = in.readLong());
      updates = u < 0 ? n : u;
    }
    
    @Override
    public boolean hasData() {
      return i < updates;
    }
    
    protected void read() throws IOException {
      x = in.readLong();
      w = in.readDouble();
      i++;
    }
    
    @Override
    public void update(IntegerSampler sampler) throws IOException {
      read();
      sampler.update(x, (long) w);
    }
    
    @Override
    public void update(RealSampler sampler) throws IOException {
      read();
      sampler.update(x, w);
    }
  }
  
  protected static class KmerIP implements InputProccessor {
    protected final BufferedReader in;
    protected final int k;
    protected boolean data = true, nl = true;
    protected int n = 0;
    protected long kmer = 0, reverseKmer = 0;
    
    protected final boolean print;
    
    public KmerIP(InputStream ins, int k, boolean print) throws IOException {
      in = new BufferedReader(new InputStreamReader(ins, StandardCharsets.UTF_8));
      this.k = k;
      this.print = print;
      read();
    }
    
    public static final int
      C = 0b00,
      G = 0b01,
      A = 0b10,
      T = 0b11,
      U = T;
    
    protected void add(long nuc) {
      kmer = (kmer << 2) | nuc;
      reverseKmer = (kmer >> 2) | ((nuc ^ 0b01) << (2 * (k - 1)));
      if (n < k) n++;
        else kmer &= (1 << (2 * k)) - 1;
    }
    
    protected void read() throws IOException {
      for (;;) {
        boolean onl = nl;
        switch (in.read()) {
          case -1:
            data = false;
            return;
          case '\r':
          case '\n':
            nl = true;
            break;
          case '>':
          case ';':
            if (onl) {
              String comment = in.readLine();
              if (print) System.out.println("[FASTA] " + comment);
            } else kmer = reverseKmer = n = 0;
            break;
          case 'C':
            add(C);
            break;
          case 'G':
            add(G);
            break;
          case 'A':
            add(A);
            break;
          case 'T':
            add(T);
            break;
          case 'U':
            add(U);
            break;
          default:
            kmer = reverseKmer = n = 0;
        }
        if (n == k) return;
      }
    }
    
    @Override
    public boolean hasData() {
      return data;
    }
    
    @Override
    public void update(IntegerSampler sampler) throws IOException {
      read();
      sampler.update(Math.min(kmer, reverseKmer), 1);
    }
    
    @Override
    public void update(RealSampler sampler) throws IOException {
      read();
      sampler.update(Math.min(kmer, reverseKmer), 1);
    }
    
    @Override
    public String decode(long x) {
      StringBuilder ret = new StringBuilder();
      for (int i = 0; i < k; i++, x >>>= 2) switch ((int) (x & 0b11)) {
        case C:
          ret.append('C');
          break;
        case G:
          ret.append('G');
          break;
        case A:
          ret.append('A');
          break;
        case T:
          ret.append('T');
          break;
      }
      return ret.reverse().toString();
    }
  }
  
  @FunctionalInterface
  public interface Updater {
    void update(long x, double w);
  }
}