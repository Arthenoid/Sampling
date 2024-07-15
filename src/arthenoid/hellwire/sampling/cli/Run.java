package arthenoid.hellwire.sampling.cli;

import arthenoid.hellwire.sampling.Result;
import arthenoid.hellwire.sampling.Util;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
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
        .getConstructor(Context.class, long.class, double.class, double.class, double.class);
    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
      die("Sampler not found");
      return null;
    }
  }
  
  protected static Sampler createSampler(Constructor<? extends Sampler> constructor, Context context, long n) throws IllegalAccessException, IllegalArgumentException, InstantiationException, InvocationTargetException {
    return constructor.newInstance(
      context,
      n,
      Opt.relativeError.value(),
      Opt.absoluteError.value(),
      Opt.failureProbability.value()
    );
  }
  
  protected static Sampler createSampler(Constructor<? extends Sampler> constructor, long seed, Function<Context, Hash> hasher, long n) throws IllegalAccessException, IllegalArgumentException, InstantiationException, InvocationTargetException {
    return createSampler(
      constructor,
      new BasicContext(seed, hasher),
      n
    );
  }
  
  protected static Sampler createSampler(Constructor<? extends Sampler> constructor, Function<Context, Hash> hasher, long n) throws IllegalAccessException, IllegalArgumentException, InstantiationException, InvocationTargetException {
    return createSampler(
      constructor,
      new BasicContext(hasher),
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
    return result == null ? "QUERY FAILED" : ("(" + ip.decode(result.index) + ", " + result.frequency + ")");
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
  
  protected static void printTime(PrintStream out, String label, long t) {
    if (Opt.time.present()) out.printf(LOCALE, "[%s: %s]\n", label, formatTime(t));
  }
  
  protected static void printTimeSince(PrintStream out, String label, long since) {
    printTime(out, label, System.nanoTime() - since);
  }
  
  @FunctionalInterface
  public interface SamplerFactory {
    Sampler create(int i, long n) throws IllegalAccessException, IllegalArgumentException, InstantiationException, InvocationTargetException;
  }
  
  public static final Predicate<String> HAS_FASTA_EXTENSION = Pattern.compile(".*\\.fa(s(ta)?)?", Pattern.CASE_INSENSITIVE).asMatchPredicate();
  
  protected static void testOn(Path file, int m, SamplerFactory samplerFactory, PrintStream out) throws IOException {
    out.println("================================");
    out.println("Testing on file: " + file);
    try (
      InputStream in = Files.newInputStream(file)
    ) {
      long tt = System.nanoTime();
      
      int n, size;
      InputProcessor ip;
      if (HAS_FASTA_EXTENSION.test(file.toString())) {
        if (!Opt.kMer.present()) {
          out.println("Input is a FASTA file, but the value for k was not specified.");
          return;
        }
        ip = new InputProcessor.KMer(in, Opt.kMer.value(), out);
        n = 1 << (2 * Opt.kMer.value());
        size = (int) Files.size(file);
      } else {
        InputProcessor.Gen gip;
        try {
          ip = gip = new InputProcessor.Gen(in);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException  e) {
          out.println("Invalid data format");
          return;
        }
        Format format = gip.format;
        out.printf(LOCALE, "Input format: %s with domain size of %d and %d updates (seed: %d)\n",  gip.name, format.n, format.updates, format.seed);
        n = (int) format.n;
        size = (int) format.updates;
      }
      
      double p;
      try {
        Sampler sampler = samplerFactory.create(0, n);
        out.println("Sampler memory usage: " + sampler.memoryUsed());
        p = sampler.p();
      } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException e) {
        out.printf(LOCALE, "Sampler cannot be initialised: %s\n", CLI.getMessage(e));
        return;
      }
      
      int bufferSize = Math.min(Opt.buffer.value().intValue(), size);
      long[]
        buffIndex = new long[bufferSize],
        buffDiff = new long[bufferSize];
      AtomicLong
        update = new AtomicLong(),
        query = new AtomicLong();
      long[] frequencies = new long[n];
      Result[][] results;
      
      long t = System.nanoTime();
      if (bufferSize < size) {
        Sampler[] samplers = new Sampler[m];
        try {
          for (int i = 0; i < m; i++) samplers[i] = samplerFactory.create(i, n);
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException e) {
          out.printf(LOCALE, "Samplers cannot be initialised: %s\n", CLI.getMessage(e));
          return;
        }
        
        while (ip.hasData()) {
          int fill = 0;
          while (fill < bufferSize && ip.hasData()) {
            int index = fill;
            ip.update((i, f) -> {
              frequencies[(int) i] += f;
              buffIndex[index] = i;
              buffDiff[index] = f;
            });
            fill++;
          }
          int to = fill;
          Stream.of(samplers).unordered().parallel().forEach(s -> {
            long ut = System.nanoTime();
            for (int i = 0; i < to; i++) s.update(buffIndex[i], buffDiff[i]);
            update.addAndGet(System.nanoTime() - ut);
          });
        }
        
        results = Stream.of(samplers).unordered().parallel().map(s -> {
          long qt = System.nanoTime();
          Result[] r = s.queryAll().toArray(Result[]::new);
          query.addAndGet(System.nanoTime() - qt);
          return r;
        }).toArray(Result[][]::new);
      } else {
        int fill = 0;
        while (ip.hasData()) {
          int index = fill;
          ip.update((i, f) -> {
            frequencies[(int) i] += f;
            buffIndex[index] = i;
            buffDiff[index] = f;
          });
          fill++;
        }
        int to = fill;
        
        results = IntStream.range(0, m).unordered().parallel().mapToObj(i -> {
          Sampler s;
          try {
            s = samplerFactory.create(i, n);
          } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
          }
          long uqt = System.nanoTime();
          for (int j = 0; j < to; j++) s.update(buffIndex[j], buffDiff[j]);
          update.addAndGet(System.nanoTime() - uqt);
          uqt = System.nanoTime();
          Result[] r = s.queryAll().toArray(Result[]::new);
          query.addAndGet(System.nanoTime() - uqt);
          return r;
        }).toArray(Result[][]::new);
      }
      printTime(out, "Update average", update.get() / m);
      printTime(out, "Query average", query.get() / m);
      printTimeSince(out, "Update and query total", t);
      
      
      t = System.nanoTime();
      double[]
        sampleFrequencies = new double[n],
        sampleErrors = new double[n];
      long[] sampled = new long[n];
      long failed = 0, failedSub = 0, total = 0;
      for (Result[] result : results) {
        long f = 0;
        for (Result r : result) if (r == null) {
          f++;
        } else {
          int i = (int) r.index;
          sampleFrequencies[i] += r.frequency;
          sampleErrors[i] += Math.abs(r.frequency - frequencies[i]);
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
      out.printf(
        LOCALE,
        "Failed samplers: %d/%d (%.2f%%)\n",
        failed,
        m,
        failed * 100.0 / m
      );
      out.printf(
        LOCALE,
        "Failed subsamplers: %d/%d (%.2f%%)\n",
        failedSub,
        total,
        failedSub * 100.0 / total
      );
      long samples = total - failedSub;
      out.printf(
        LOCALE,
        "Samples: %d\n",
        samples
      );
      out.println("Frequency estimates (absolute ~ relative):");
      out.printf(
        LOCALE,
        "- Average sample error: %.3g ~ %.3g\n",
        DoubleStream.of(sampleErrors).unordered().parallel().sum() / samples,
        IntStream.range(0, n).unordered().parallel().filter(i -> sampled[i] > 0).mapToDouble(i -> sampleErrors[i] / frequencies[i]).sum() / samples
      );
      for (int i = 0; i < n; i++) if (sampled[i] > 0) sampleFrequencies[i] = Math.abs(sampleFrequencies[i] - frequencies[i] * sampled[i]);
      out.printf(
        LOCALE,
        "- Average index average error: %.3g ~ %.3g\n",
        DoubleStream.of(sampleFrequencies).unordered().parallel().sum() / samples,
        IntStream.range(0, n).unordered().parallel().filter(i -> sampled[i] > 0).mapToDouble(i -> sampleFrequencies[i] / frequencies[i]).sum() / samples
      );
      double[]
        weights = new double[n],
        probabilities = new double[n],
        sampleShares = new double[n];
      for (int i = 0; i < n; i++) weights[i] = Util.pow(frequencies[i], p);
      double
        pNorm = DoubleStream.of(weights).unordered().parallel().sum(),
        pNormCA = IntStream.range(0, n).unordered().parallel().filter(i -> sampled[i] > 0).mapToDouble(i -> weights[i]).sum();
      for (int i = 0; i < n; i++) {
        probabilities[i] = weights[i] / pNorm;
        sampleShares[i] = sampled[i] / (double) samples;
      }
      out.printf(
        LOCALE,
        "Total variation distance: %.4g\n- Coverage adjusted: %.4g with %.4g coverage\n",
        IntStream.range(0, n).unordered().parallel().mapToDouble(i -> Math.abs(sampleShares[i] - probabilities[i])).sum() / 2,
        IntStream.range(0, n).unordered().parallel().filter(i -> sampled[i] > 0).mapToDouble(i -> Math.abs(sampleShares[i] - weights[i] / pNormCA)).sum() / 2,
        pNormCA / pNorm
      );
      out.printf(
        LOCALE,
        "Kullback-Leibler divergence: %.4g\n",
        IntStream.range(0, n).unordered().parallel().filter(i -> sampled[i] > 0).mapToDouble(i -> sampleShares[i] * Math.log(sampleShares[i] / probabilities[i])).sum()
      );
      printTimeSince(out, "Result analysys", t);
      
      if (Opt.distribution.present()) {
        t = System.nanoTime();
        out.println("Distribution:");
        for (int i = 0; i < n; i++) out.printf(
          LOCALE,
          "- %d:\n  - Expected: %.3g\n  - Actual:   %.3g\n",
          i,
          probabilities[i],
          sampleShares[i]
        );
        printTimeSince(out, "Distribution", t);
      }
      
      printTimeSince(out, "Total", tt);
    }
  }
}