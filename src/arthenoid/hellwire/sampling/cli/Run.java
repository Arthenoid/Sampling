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
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
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
  
  protected static String formatTime(long t) {
    long a = t % 1000000000;
    t /= 1000000000;
    String ret = String.format(LOCALE, ".%09d", a);
    a = t % 60;
    t /= 60;
    if (t == 0) return String.format(LOCALE, "%d%s", a, ret);
    ret = String.format(LOCALE, "%02d%s", a, ret);
    a = t % 60;
    t /= 60;
    return t == 0
      ? String.format(LOCALE, "%d:%s", a, ret)
      : String.format(LOCALE, "%d:%02d:%s", t, a, ret);
  }
  
  protected static void printTime(PrintStream out, String label, long since) {
    if (Opt.time.present()) out.printf(LOCALE, "[%s: %s]\n", label, formatTime(System.nanoTime() - since));
  }
  
  
  protected static void testOn(Path file, LongFunction<Sampler[]> samplerFactory, PrintStream out) throws IOException {
    out.println("================================");
    out.println("Testing on file: " + file);
    try (
      InputStream in = Files.newInputStream(file)
    ) {
      long tt = System.nanoTime();
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
      
      int n = (int) format.n;
      long[] frequencies = new long[n];
      
      long t = System.nanoTime();
      int bufferSize = Math.min(Opt.buffer.value().intValue(), (int) format.updates);
      long[]
        buffIndex = new long[bufferSize],
        buffDiff = new long[bufferSize];
      while (ip.hasData()) {
        int fill = 0;
        while (fill < bufferSize && ip.hasData()) {
          int index = fill;
          ip.update((i, w) -> {
            frequencies[(int) i] += w;
            buffIndex[index] = i;
            buffDiff[index] = w;
          });
          fill++;
        }
        int to = fill;
        Stream.of(samplers).unordered().parallel().forEach(s -> {
          for (int i = 0; i < to; i++) s.update(buffIndex[i], buffDiff[i]);
        });
      }
      printTime(out, "Update", t);
      
      t = System.nanoTime();
      Result[][] results = Stream.of(samplers).unordered().parallel().map(s -> s.queryAll().toArray(Result[]::new)).toArray(Result[][]::new);
      printTime(out, "Query", t);
      
      t = System.nanoTime();
      double[]
        weights = new double[n],
        sampleFrequencies = new double[n],
        sampleDeviations = new double[n];
      long[] sampled = new long[n];
      long failed = 0, failedSub = 0, total = 0;
      for (Result[] result : results) {
        long f = 0;
        for (Result r : result) if (r == null) {
          f++;
        } else {
          int i = (int) r.i;
          sampleFrequencies[i] += r.w;
          sampleDeviations[i] += Math.abs(r.w - frequencies[i]);
          sampled[i]++;
        }
        if (f == result.length) failed++;
        failedSub += f;
        total += result.length;
      }
      if (failedSub == total) {
        out.println("All samplers failed.");
        return;
      }
      long samples = total - failedSub;
      out.printf(
        LOCALE,
        "Failed samplers: %d/%d (%.2f%%)\n",
        failed,
        samplers.length,
        failed * 100.0 / samplers.length
      );
      out.printf(
        LOCALE,
        "Failed subsamplers: %d/%d (%.2f%%)\n",
        failedSub,
        total,
        failedSub * 100.0 / total
      );
      out.println("Frequency estimates (absolute ~ relative):");
      out.printf(
        LOCALE,
        "- Average sample deviation: %.3g ~ %.3g\n",
        DoubleStream.of(sampleDeviations).unordered().parallel().sum() / samples,
        IntStream.range(0, n).unordered().parallel().mapToDouble(i -> sampleDeviations[i] / frequencies[i]).sum() / samples
      );
      for (int i = 0; i < n; i++) if (sampled[i] > 0) sampleFrequencies[i] = Math.abs(sampleFrequencies[i] - frequencies[i] * sampled[i]);
      out.printf(
        LOCALE,
        "- Average index average deviation: %.3g ~ %.3g\n",
        DoubleStream.of(sampleFrequencies).unordered().parallel().sum() / samples,
        IntStream.range(0, n).unordered().parallel().mapToDouble(i -> sampleFrequencies[i] / frequencies[i]).sum() / samples
      );
      double p = samplers[0].p();
      for (int i = 0; i < n; i++) weights[i] = Math.pow(frequencies[i], p);
      double normP = DoubleStream.of(weights).unordered().parallel().sum();
      out.printf(
        LOCALE,
        "Distribution deviation: %.4g\n",
        IntStream.range(0, n).unordered().parallel().mapToDouble(i -> Math.abs(sampled[i] / (double) samples - weights[i] / normP)).sum() / 2
      );
      printTime(out, "Result analysys", t);
      
      if (Opt.distribution.present()) {
        t = System.nanoTime();
        out.println("Distribution:");
        for (int i = 0; i < n; i++) out.printf(
          LOCALE,
          "- %d:\n  - Expected: %.3g\n  - Actual:   %.3g\n",
          i,
          weights[i] / normP,
          sampled[i] / (double) samples
        );
        printTime(out, "Distribution", t);
      }
      
      printTime(out, "Total", tt);
    }
  }
}