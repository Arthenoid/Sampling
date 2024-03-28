package arthenoid.hellwire.sampling;

import arthenoid.hellwire.sampling.context.BasicContext;
import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.context.Hash;
import arthenoid.hellwire.sampling.context.MurmurHash;
import arthenoid.hellwire.sampling.datagen.Format;
import arthenoid.hellwire.sampling.samplers.IntegerSampler;
import arthenoid.hellwire.sampling.samplers.RealSampler;
import arthenoid.hellwire.sampling.samplers.Sampler;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import java.util.function.Function;
import java.util.function.Supplier;

public class CLI {
  protected static Format getFormat(String name, long seed, long n, long updates) throws Exception {
    return ((Class<? extends Format>) Class.forName("arthenoid.hellwire.sampling.datagen.Format" + name))
      .getConstructor(long.class, long.class, long.class)
      .newInstance(seed, n, updates);
  }
  
  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("Sampler not specified");
      System.exit(1);
    }
    if (args[0].equals("gen")) {
      if (args.length < 4 || args.length > 5) {
        System.err.println("Wrong number of generator arguments (expected: <format> <domain size> <# of updates> [<seed>])");
        System.exit(1);
      }
      try {
        String name = args[1];
        long
          seed = args.length > 4 ? Long.parseLong(args[4]) : (new Random()).nextLong(),
          n = Long.parseLong(args[2]),
          updates = Long.parseLong(args[3]);
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
        System.err.println("Format not found");
        System.exit(1);
        return;
      }
    }
    if (args[0].charAt(args[0].length() - 1) != ')') {
      System.err.println("Invalid sampler specification");
      System.exit(1);
    }
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
      System.err.println("Sampler not found");
      System.exit(1);
      return;
    }
    InputStream ins = null;
    long period = 0, prime = 0, seed = Long.MIN_VALUE;
    Function<Context, Hash> hasher = null;
    boolean test = false;
    for (int i = 1; i < args.length; i++) switch (args[i]) {
      case "period":
      case "P":
        if (period > 0) {
          System.err.println("Period already set");
          System.exit(1);
        }
        try {
          period = Long.parseLong(args[++i]);
        } catch (Exception e) {
          System.err.println("Invalid period");
          System.exit(1);
        }
        if (period <= 0) {
          System.err.println("Period must be positive");
          System.exit(1);
        }
        break;
      case "in":
        if (ins != null) {
          System.err.println("Input already set");
          System.exit(1);
        }
        try {
          ins = Files.newInputStream(Path.of(args[++i]));
        } catch (Exception e) {
          System.err.println("Invalid input");
          System.exit(1);
        }
        break;
      case "prime":
        if (prime > 0) {
          System.err.println("Prime already set");
          System.exit(1);
        }
        try {
          prime = Long.parseLong(args[++i]);
        } catch (Exception e) {
          System.err.println("Invalid prime");
          System.exit(1);
        }
        if (prime <= 1) {
          System.err.println("Prime must be positive");
          System.exit(1);
        }
        break;
      case "seed":
        if (seed > Long.MIN_VALUE) {
          System.err.println("Seed already set");
          System.exit(1);
        }
        try {
          seed = Long.parseLong(args[++i]);
        } catch (Exception e) {
          System.err.println("Invalid seed");
          System.exit(1);
        }
        break;
      case "hash":
      case "H":
        if (hasher != null) {
          System.err.println("Hash already set");
          System.exit(1);
        }
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
          System.err.println("Hash not found");
          System.exit(1);
        }
        break;
      case "test":
      case "T":
        if (test) {
          System.err.println("Test already set");
          System.exit(1);
        }
        test = true;
        break;
      default:
        System.err.println("Unknown argument: " + args[i]);
        System.exit(1);
    }
    if (prime == 0) prime = 1685727585142753L;
    if (hasher == null) hasher = MurmurHash::new;
    if (ins == null) ins = System.in;
    constructorParams[0] = seed == Long.MIN_VALUE ? new BasicContext(prime, hasher) : new BasicContext(prime, seed, hasher);
    Sampler sampler;
    try {
      sampler = constructor.newInstance(constructorParams);
    } catch (Exception e) {
      System.err.println("Sampler cannot be initialised");
      System.exit(1);
      return;
    }
    System.out.println("Sampler memory usage: " + sampler.memoryUsed());
    if (test) {
      DataInputStream in = new DataInputStream(ins);
      String name;
      long inSeed, n, updates;
      Format format;
      try {
        format = getFormat(name = in.readUTF(), inSeed = in.readLong(), n = in.readLong(), updates = in.readLong());
      } catch (Exception e) {
        System.err.println("Invalid data format");
        System.exit(1);
        return;
      }
      System.out.println("Test input format: " + name + " " + n + " " + updates + " " + inSeed);
      Updater update = sampler instanceof IntegerSampler
        ? (x, w) -> {
          ((IntegerSampler) sampler).update(x, (long) w);
        }
        : (x, w) -> {
          ((RealSampler) sampler).update(x, w);
        };
      try {
        for (long i = 0; i < updates; i++) update.update(in.readLong(), in.readDouble());
      } catch (IOException e) {
        System.err.println("IO exception");
        System.exit(1);
        return;
      }
      Result result = sampler.query();
      if (result == null) {
        System.out.println("Query failed");
      } else {
        System.out.println("Result: (" + result.i + ", " + result.getWeight().doubleValue() + ")");
        Format.Expectation expected = format.expected(sampler.p(), result.i);
        System.out.println("This index was expected with probability" + expected.probability + " and frequency around " + expected.weight);
      }
    } else {
      Scanner in = new Scanner(ins == null ? System.in : ins, StandardCharsets.UTF_8);
      Runnable update;
      Supplier<String> query;
      if (sampler instanceof IntegerSampler) {
        update = () -> {
          ((IntegerSampler) sampler).update(in.nextLong(), in.nextLong());
        };
        query = () -> {
          IntegerResult result = (IntegerResult) sampler.query();
          return result == null ? "FAILED" : ("(" + result.i + ", " + result.weight + ")");
        };
      } else {
        update = () -> {
          ((RealSampler) sampler).update(in.nextLong(), in.nextDouble());
        };
        query = () -> {
          RealResult result = (RealResult) sampler.query();
          return result == null ? "FAILED" : ("(" + result.i + ", " + result.weight + ")");
        };
      }
      long i = 0;
      while (in.hasNext()) {
        if (period > 0 && i % period == 0) System.out.println("After " + i + " updates: " + query.get());
        update.run();
        i++;
      }
      System.out.println("Final (after " + i + " updates): " + query.get());
    }
  }
  
  @FunctionalInterface
  public interface Updater {
    void update(long x, double w);
  }
}