package arthenoid.hellwire.sampling.cli;

import arthenoid.hellwire.sampling.Result;
import static arthenoid.hellwire.sampling.cli.CLI.LOCALE;
import static arthenoid.hellwire.sampling.cli.CLI.die;
import arthenoid.hellwire.sampling.context.BasicContext;
import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.context.Hash;
import arthenoid.hellwire.sampling.context.MurmurHash;
import arthenoid.hellwire.sampling.datagen.Format;
import arthenoid.hellwire.sampling.datagen.SUFormat;
import arthenoid.hellwire.sampling.samplers.Sampler;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.stream.Stream;

public class Run {
  protected static Function<Context, Hash> getHasher() {
    if (!Opt.hash.present()) return MurmurHash::new;
    try {
      Constructor<? extends Hash> hCons = Class.forName("arthenoid.hellwire.sampling.context." + Opt.hash.value() + "Hash").asSubclass(Hash.class).getConstructor(Context.class);
      return c -> {
        try {
          return hCons.newInstance(c);
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException e) {
          throw new RuntimeException(e);
        }
      };
    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
      die("Hash not found");
      return null;
    }
  }
  
  protected static Constructor<? extends Sampler> getSamplerConstructor(String name) {
    try {
      return Class.forName("arthenoid.hellwire.sampling.samplers." + name + "Sampler")
        .asSubclass(Sampler.class)
        .getConstructor(Context.class, long.class, double.class, double.class);
    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
      die("Sampler not found");
      return null;
    }
  }
  
  protected static Sampler createSampler(Constructor<? extends Sampler> constructor, Context context, long n) {
    try {
      return constructor.newInstance(
        context,
        n,
        Opt.delta.value(),
        Opt.epsilon.value()
      );
    } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException e) {
      die("Sampler cannot be initialised", e);
      return null;
    }
  }
  
  protected static Sampler createSampler(Constructor<? extends Sampler> constructor, long seed, Function<Context, Hash> hasher, long n) {
    return createSampler(
      constructor,
      new BasicContext(Opt.prime.value(), seed, hasher),
      n
    );
  }
  
  protected static Sampler createSampler(Constructor<? extends Sampler> constructor, Function<Context, Hash> hasher, long n) {
    return createSampler(
      constructor,
      new BasicContext(Opt.prime.value(), hasher),
      n
    );
  }
  
  protected static Format getFormat(String name, long seed, long n, long updates) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
    return updates < 0
      ? Class.forName("arthenoid.hellwire.sampling.datagen.SUFormat" + name)
        .asSubclass(SUFormat.class)
        .getConstructor(long.class, long.class)
        .newInstance(seed, n)
      : Class.forName("arthenoid.hellwire.sampling.datagen.Format" + name)
        .asSubclass(Format.class)
        .getConstructor(long.class, long.class, long.class)
        .newInstance(seed, n, updates);
  }
  
  protected static String formatResult(Result result, InputProcessor ip) {
    return result == null ? "QUERY FAILED" : ("(" + ip.decode(result.i) + ", " + result.w + ")");
  }
  
  protected static String formatQuery(Sampler sampler, InputProcessor ip) {
    return formatResult(sampler.query(), ip);
  }
  
  protected static String formatTime(long since) {
    long t = System.nanoTime() - since, a = t % 1000000000;
    t /= 1000000000;
    String ret = "." + a;
    a = t % 60;
    t /= 60;
    ret = a + ret;
    if (t == 0) return ret;
    a = t % 60;
    t /= 60;
    ret = a + ":" + ret;
    return t == 0 ? ret : t % 60 + ":" + ret;
  }
  
  protected static void testOn(Path file, LongFunction<Sampler[]> samplerFactory, PrintStream out) throws IOException {
    out.println("================================");
    out.println("Testing on file: " + file);
    try (
      InputStream in = Files.newInputStream(file)
    ) {
      InputProcessor.Gen ip;
      try {
        ip = new InputProcessor.Gen(in);
      } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException  e) {
        die("Invalid data format");
        return;
      }
      Format format = ip.format;
      out.printf(LOCALE, "Input format: %s with domain size of %d and %d updates (seed: %d)\n",  ip.name, format.n, format.updates, format.seed);
      
      Sampler[] samplers = samplerFactory.apply(format.n);
      out.println("Sampler memory usage: " + samplers[0].memoryUsed());
      
      long[] actual = new long[(int) format.n];
      
      long t = System.nanoTime();
      while (ip.hasData()) ip.update((i, w) -> {
        actual[(int) i] += w;
        Stream.of(samplers).unordered().parallel().forEach(s -> s.update(i, w));
      });
      if (Opt.time.present()) out.println("Update: " + formatTime(t));
      
      t = System.nanoTime();
      Result[][] results = Stream.of(samplers).unordered().parallel().map(s -> s.queryAll().toArray(Result[]::new)).toArray(Result[][]::new);
      if (Opt.time.present()) out.println("Query: " + formatTime(t));
      
      t = System.nanoTime();
      long failed = 0, failedSub = 0, total = 0;
      for (Result[] result : results) {
        long f = 0;
        for (Result r : result) if (r == null) f++;
        if (f == result.length) failed++;
        failedSub += f;
        total += result.length;
      }
      out.printf(LOCALE, "Failed samplers: %d/%d (%.2f%%)\n",
        failed,
        samplers.length,
        failed * 100.0 / samplers.length
      );
      out.printf(LOCALE, "Failed subsamplers: %d/%d (%.2f%%)\n",
        failedSub,
        total,
        failedSub * 100.0 / total
      );
      if (Opt.time.present()) out.println("Result analysys: " + formatTime(t));
    }
  }
}