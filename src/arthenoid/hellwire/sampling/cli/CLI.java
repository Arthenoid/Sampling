package arthenoid.hellwire.sampling.cli;

import arthenoid.hellwire.sampling.Result;
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
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Stream;

public class CLI {
  protected static void die(String msg) {
    System.err.println(msg);
    System.exit(1);
  }
  
  public static void main(String[] args) {
    main(Stream.of(args).iterator());
  }
  
  public static void main(Iterator<String> args) {
    if (!args.hasNext()) die("No command specified");
    switch (args.next()) {
      case "sample":
        sample(args);
        break;
      case "gen":
        gen(args);
        break;
      default:
        die("Unknown command");
    }
  }
  
  public static void tryParse(ArgParser ap, Iterator<String> args) {
    String error = ap.parse(args);
    if (error != null) die("Invalid arguments: " + error);
  }
  
  public static void gen(Iterator<String> args) {
    String name;
    long n, updates, seed;
    try {
      name = args.next();
      n = Long.parseLong(args.next());
      if (n <= 0) die("The domain size must be positive");
      String updatesArg = args.next();
      updates = updatesArg.equals("n") ? -1 : Long.parseLong(updatesArg);
      if (!updatesArg.equals("n") && updates < 0) die("The # of updates can't be negative");
    } catch (NoSuchElementException e) {
      die("Wrong number of generator arguments (expected: <format> <domain size> <# of updates>)");
      return;
    } catch (NumberFormatException e) {
      die("Invalid generator arguments");
      return;
    }
    ArgParser ap = ArgParser.create(Opt.out, Opt.seed);
    tryParse(ap, args);
    
    seed = Opt.seed.or(() -> (new Random()).nextLong());
    Format format;
    try {
      format = getFormat(name, seed, n, updates);
    } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
      die("Format not found: " + e.getMessage());
      return;
    }
    try (DataOutputStream out = new DataOutputStream(Opt.out.present() ? Files.newOutputStream(Opt.out.value()) : System.out)) {
      out.writeUTF(name);
      out.writeLong(seed);
      out.writeLong(n);
      out.writeLong(updates);
      format.generate(out);
    } catch (IOException  e) {
      die("IOException: " + e.getMessage());
    }
  }
  
  protected static Format getFormat(String name, long seed, long n, long updates) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
    return updates < 0
      ? Class.forName("arthenoid.hellwire.sampling.datagen.SUFormat" + name)
        .asSubclass(Format.class)
        .getConstructor(long.class, long.class)
        .newInstance(seed, n)
      : Class.forName("arthenoid.hellwire.sampling.datagen.Format" + name)
        .asSubclass(Format.class)
        .getConstructor(long.class, long.class, long.class)
        .newInstance(seed, n, updates);
  }
  
  public static void sample(Iterator<String> args) {
    String name;
    long n;
    try {
      name = args.next();
      n = Long.parseLong(args.next());
      if (n <= 0) die("The domain size must be positive");
    } catch (NoSuchElementException e) {
      die("Sampler not specified");
      return;
    } catch (NumberFormatException e) {
      die("Invalid domain size");
      return;
    }
    ArgParser ap = ArgParser.create(Opt.in, Opt.out, Opt.delta, Opt.epsilon, Opt.period, Opt.seed, Opt.prime, Opt.hash, Opt.gen, Opt.kMer);
    tryParse(ap, args);
    
    Function<Context, Hash> hasher;
    if (Opt.hash.present()) {
      try {
        Constructor<? extends Hash> hCons = Class.forName("arthenoid.hellwire.sampling.context." + Opt.hash.value() + "Hash").asSubclass(Hash.class).getConstructor(Context.class);
        hasher = c -> {
          try {
            return hCons.newInstance(c);
          } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
          }
        };
      } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
        die("Hash not found");
        return;
      }
    } else hasher = MurmurHash::new;
    
    Sampler<?> sampler;
    try {
      sampler = Class.forName("arthenoid.hellwire.sampling.samplers." + name + "Sampler")
        .asSubclass(Sampler.class)
        .getConstructor(Context.class, long.class, double.class, double.class)
        .newInstance(
          Opt.seed.present() ? new BasicContext(Opt.prime.value(), Opt.seed.value(), hasher) : new BasicContext(Opt.prime.value(), hasher),
          n,
          Opt.delta.value(),
          Opt.epsilon.value()
        );
    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
      die("Sampler not found");
      return;
    } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException e) {
      die("Sampler cannot be initialised: " + e.getMessage());
      return;
    }
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
    
    try (
      InputStream in = Opt.in.present() ? Files.newInputStream(Opt.in.value()) : System.in;
      PrintStream out = Opt.out.present() ? new PrintStream(Opt.out.value().toFile(), StandardCharsets.UTF_8) : System.out
    ) {
      out.println("Sampler memory usage: " + sampler.memoryUsed());
      
      InputProccessor ip;
      if (Opt.gen.present() && Opt.kMer.present()) die("Mulitple output types specified.");
      if (Opt.gen.present()) {
        GenIP gip;
        try {
          ip = gip = new GenIP(in);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException  e) {
          die("Invalid data format");
          return;
        }
        Format format = gip.format;
        out.printf("Test input format: %s with domain size of %d and %d updates (seed: %d)\n",  gip.name, format.n, format.updates, format.seed);
        if (n != format.n) die("The specified domain size is different than in the input.");
      } else if (Opt.kMer.present()) {
        long reqN = 1 << (2 * Opt.kMer.value());
        if (n != reqN) die("The specified domain size is different from the required (" + reqN + ")");
        ip = new KmerIP(in, Opt.kMer.value(), out);
      } else {
        ip = new TextIP(in);
      }
      
      long i = 0, period = Opt.period.value();
      while (ip.hasData()) {
        if (integer) ip.update(integerSampler);
          else ip.update(realSampler);
        i++;
        if (period > 0 && i % period == 0) out.println("After " + i + " updates: " + formatQuery(sampler, ip));
      }
      Result<?> result = sampler.query();
      out.println("Final (after " + i + " updates): " + formatResult(result, ip));
      if (Opt.gen.present() && result != null) {
        Format.Expectation expected = ((GenIP) ip).format.expected(sampler.p(), result.i);
        out.printf("This index was expected with probability %f and frequency around %f.\n", expected.probability, expected.weight);
      }
    } catch (IOException e) {
      die("IO exception: " + e.getMessage());
    }
  }
  
  protected static String formatResult(Result<?> result, InputProccessor ip) {
    return result == null ? "QUERY FAILED" : ("(" + ip.decode(result.i) + ", " + result.getWeight() + ")");
  }
  
  protected static String formatQuery(Sampler<?> sampler, InputProccessor ip) {
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
    
    public TextIP(InputStream in) {
      this.in = new Scanner(in, StandardCharsets.UTF_8);
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
  
  protected static class GenIP implements InputProccessor {
    protected final DataInputStream in;
    protected int i = 0;
    protected long x;
    protected double w;
    
    public final String name;
    public final Format format;
    
    public GenIP(InputStream in) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException {
      this.in = new DataInputStream(in);
      format = getFormat(name = this.in.readUTF(), this.in.readLong(), this.in.readLong(), this.in.readLong());
    }
    
    @Override
    public boolean hasData() {
      return i < format.updates;
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
    protected long kMer = 0, reverseKMer = 0;
    
    protected final PrintStream out;
    
    public KmerIP(InputStream ins, long k, PrintStream out) throws IOException {
      in = new BufferedReader(new InputStreamReader(ins, StandardCharsets.UTF_8));
      this.k = (int) k;
      this.out = out;
      read();
    }
    
    public static final int
      C = 0b00,
      G = 0b01,
      A = 0b10,
      T = 0b11,
      U = T;
    
    protected void add(long nuc) {
      kMer = (kMer << 2) | nuc;
      reverseKMer = (kMer >> 2) | ((nuc ^ 0b01) << (2 * (k - 1)));
      if (n < k) n++;
        else kMer &= (1 << (2 * k)) - 1;
    }
    
    protected final void read() throws IOException {
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
              if (out != null) out.println("[FASTA] " + comment);
            } else kMer = reverseKMer = n = 0;
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
            kMer = reverseKMer = n = 0;
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
      sampler.update(Math.min(kMer, reverseKMer), 1);
    }
    
    @Override
    public void update(RealSampler sampler) throws IOException {
      read();
      sampler.update(Math.min(kMer, reverseKMer), 1);
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
}