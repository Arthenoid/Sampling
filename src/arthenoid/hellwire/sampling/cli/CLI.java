package arthenoid.hellwire.sampling.cli;

import arthenoid.hellwire.sampling.Result;
import static arthenoid.hellwire.sampling.cli.Run.printTime;
import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.context.Hash;
import arthenoid.hellwire.sampling.datagen.Format;
import arthenoid.hellwire.sampling.samplers.Sampler;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

public class CLI {
  public static final Locale LOCALE = Locale.ROOT;
  
  protected static void die(String msg) {
    System.err.println(msg);
    System.exit(1);
  }
  
  protected static void die(String msg, Throwable reason) {
    die(msg + ": " + getMessage(reason));
  }
  
  protected static String getMessage(Throwable t) {
    while (t.getCause() != null) t = t.getCause();
    return t.getMessage();
  }
  
  protected static PrintStream getOut() throws IOException {
    return Opt.out.present() ? new PrintStream(new FileOutputStream(Opt.out.value().toFile()), true, StandardCharsets.UTF_8) : System.out;
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
      case "test":
        test(args);
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
      die("Format not found", e);
      return;
    }
    try (DataOutputStream out = new DataOutputStream(Opt.out.present() ? Files.newOutputStream(Opt.out.value()) : System.out)) {
      out.writeUTF(name);
      out.writeLong(seed);
      out.writeLong(n);
      out.writeLong(updates);
      format.generate(out);
    } catch (IOException  e) {
      die("IOException", e);
    }
  }
  
  public static void sample(Iterator<String> args) {
    if (!args.hasNext()) die("Sampler not specified");
    String name = args.next();
    ArgParser ap = ArgParser.create(
      Opt.in,
      Opt.out,
      Opt.period,
      Opt.domainSize,
      Opt.delta,
      Opt.epsilon,
      Opt.prime,
      Opt.hash,
      Opt.seed,
      Opt.gen,
      Opt.kMer
    );
    tryParse(ap, args);
    if (!Opt.checkExclusive(Opt.domainSize, Opt.gen, Opt.kMer)) {
      if (Opt.domainSize.present()) die("Domain size needs to be specified only for the default input format.");
        else die("Conflicting input formats specified.");
    }
    
    Function<Context, Hash> hasher = Run.getHasher();
    Constructor<? extends Sampler> samplerConstructor = Run.getSamplerConstructor(name);
    
    try (
      InputStream in = Opt.in.present() ? Files.newInputStream(Opt.in.value()) : System.in;
      PrintStream out = getOut()
    ) {
      long n;
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
        out.printf(LOCALE, "Test input format: %s with domain size of %d and %d updates (seed: %d)\n",  gip.name, format.n, format.updates, format.seed);
        n = format.n;
      } else if (Opt.kMer.present()) {
        n = 1 << (2 * Opt.kMer.value());
        ip = new InputProcessor.KMer(in, Opt.kMer.value(), out);
      } else {
        die("Missing domain size");
        return;
      }
      
      Sampler sampler;
      try {
        sampler = Opt.seed.present()
          ? Run.createSampler(samplerConstructor, Opt.seed.value(), hasher, n)
          : Run.createSampler(samplerConstructor, hasher, n);
      } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException e) {
        die("Sampler cannot be initialised", e);
        return;
      }
      out.println("Sampler memory usage: " + sampler.memoryUsed());
      
      long i = 0, period = Opt.period.value();
      while (ip.hasData()) {
        ip.update(sampler);
        i++;
        if (period > 0 && i % period == 0) out.println("After " + i + " updates: " + Run.formatQuery(sampler, ip));
      }
      Result result = sampler.query();
      out.println("Final (after " + i + " updates): " + Run.formatResult(result, ip));
      if (Opt.gen.present() && result != null) {
        Format.Expectation expected = ((InputProcessor.Gen) ip).format.expected(sampler.p(), result.i);
        out.printf(LOCALE, "This index was expected with probability %f and frequency around %f.\n", expected.probability, expected.weight);
      }
    } catch (IOException e) {
      die("IO exception", e);
    }
  }
  
  public static void test(Iterator<String> args) {
    if (!args.hasNext()) die("Sampler not specified");
    String samplerName = args.next();
    if (!args.hasNext()) die("Data not specified");
    Path[] data;
    try {
      Path d = Path.of(args.next());
      data = Files.isDirectory(d) ? Files.list(d).toArray(Path[]::new) : new Path[] {d};
    } catch (IOException e) {
      die("IO exception", e);
      return;
    }
    ArgParser ap = ArgParser.create(
      Opt.out,
      Opt.time,
      Opt.distribution,
      Opt.delta,
      Opt.epsilon,
      Opt.prime,
      Opt.hash,
      Opt.seed,
      Opt.samplers,
      Opt.buffer
    );
    tryParse(ap, args);
    
    Function<Context, Hash> hasher = Run.getHasher();
    Constructor<? extends Sampler> samplerConstructor = Run.getSamplerConstructor(samplerName);
    Run.SamplerFactory samplerFactory;
    int m = Opt.samplers.value().intValue();
    if (Opt.seed.present()) {
      Random r = new Random(Opt.seed.value());
      long[] seeds = new long[m];
      for (int i = 0; i < m; i++) seeds[i] = Long.hashCode(r.nextLong());
      samplerFactory = n -> {
        Sampler[] samplers = new Sampler[m];
        for (int i = 0; i < m; i++) samplers[i] = Run.createSampler(samplerConstructor, seeds[i], hasher, n);
        return samplers;
      };
    } else {
      samplerFactory = n -> {
        Sampler[] samplers = new Sampler[m];
        for (int i = 0; i < m; i++) samplers[i] = Run.createSampler(samplerConstructor, hasher, n);
        return samplers;
      };
    }
    
    if (Opt.out.present() && Files.isDirectory(Opt.out.value())) Opt.out.set(Opt.out.value().resolve(String.format(
      LOCALE,
      "report-%1$tF-%1$tH-%1$tM-%1$tS.txt",
      System.currentTimeMillis()
    )));
    
    try (PrintStream out = getOut()) {
      long t = System.nanoTime();
      out.printf(
        LOCALE,
        "Testing %sSampler\nDelta:   %.2g\nEpsilon: %.2g\nPrime:   %d\nHash:    %s\nSeed:    %s\n",
        samplerName,
        Opt.delta.value(),
        Opt.epsilon.value(),
        Opt.prime.value(),
        Opt.hash.or("Murmur"),
        Opt.seed.present() ? Opt.seed.value() : "random"
      );
      for (Path file : data) Run.testOn(file, samplerFactory, out);
      out.println("================================");
      printTime(out, "All files total", t);
    } catch (IOException e) {
      die("IO exception", e);
    }
  }
}