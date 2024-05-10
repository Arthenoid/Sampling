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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
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
      case "gen":
        gen(args);
        break;
      case "sample":
        sample(args);
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
      format = Run.getFormat(name, seed, n, updates);
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
  
  public static void sample(Iterator<String> args) {
    if (!args.hasNext()) die("Sampler not specified");
    String name = args.next();
    long n;
    ArgParser ap = ArgParser.create(
      Opt.in,
      Opt.out,
      Opt.domainSize,
      Opt.delta,
      Opt.epsilon,
      Opt.period,
      Opt.seed,
      Opt.prime,
      Opt.hash,
      Opt.gen,
      Opt.kMer
    );
    tryParse(ap, args);
    if (!Opt.checkExclusive(Opt.domainSize, Opt.gen, Opt.kMer)) {
      if (Opt.domainSize.present()) die("Domain size needs to be specified only for the default input format.");
        else die("Conflicting input formats specified.");
    }
    
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
    
    Constructor<? extends Sampler<?>> samplerConstructor;
    try {
      @SuppressWarnings("unchecked")
      Constructor<? extends Sampler<?>> sw = (Constructor<? extends Sampler<?>>) Class.forName("arthenoid.hellwire.sampling.samplers." + name + "Sampler")
        .asSubclass(Sampler.class)
        .getConstructor(Context.class, long.class, double.class, double.class);
      samplerConstructor = sw;
    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
      die("Sampler not found");
      return;
    }
    
    try (
      InputStream in = Opt.in.present() ? Files.newInputStream(Opt.in.value()) : System.in;
      PrintStream out = Opt.out.present() ? new PrintStream(Opt.out.value().toFile(), StandardCharsets.UTF_8) : System.out
    ) {
      InputProcessor ip;
      if (Opt.domainSize.present()) {
        n = Opt.domainSize.value();
        ip = new InputProcessor.Text(in);
      } else if (Opt.gen.present()) {
        InputProcessor.Gen gip;
        try {
          ip = gip = new InputProcessor.Gen(in);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException  e) {
          die("Invalid data format");
          return;
        }
        Format format = gip.format;
        out.printf("Test input format: %s with domain size of %d and %d updates (seed: %d)\n",  gip.name, format.n, format.updates, format.seed);
        n = format.n;
      } else if (Opt.kMer.present()) {
        n = 1 << (2 * Opt.kMer.value());
        ip = new InputProcessor.KMer(in, Opt.kMer.value(), out);
      } else {
        die("Missing domain size");
        return;
      }
      
      Sampler<?> sampler;
      try {
        sampler = samplerConstructor.newInstance(
          Opt.seed.present() ? new BasicContext(Opt.prime.value(), Opt.seed.value(), hasher) : new BasicContext(Opt.prime.value(), hasher),
          n,
          Opt.delta.value(),
          Opt.epsilon.value()
        );
      } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException e) {
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
      out.println("Sampler memory usage: " + sampler.memoryUsed());
      
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
        Format.Expectation expected = ((InputProcessor.Gen) ip).format.expected(sampler.p(), result.i);
        out.printf("This index was expected with probability %f and frequency around %f.\n", expected.probability, expected.weight);
      }
    } catch (IOException e) {
      die("IO exception: " + e.getMessage());
    }
  }
  
  protected static String formatResult(Result<?> result, InputProcessor ip) {
    return result == null ? "QUERY FAILED" : ("(" + ip.decode(result.i) + ", " + result.getWeight() + ")");
  }
  
  protected static String formatQuery(Sampler<?> sampler, InputProcessor ip) {
    return formatResult(sampler.query(), ip);
  }
}