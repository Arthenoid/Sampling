package arthenoid.hellwire.sampling;

import arthenoid.hellwire.sampling.context.BasicContext;
import arthenoid.hellwire.sampling.context.Context;
import arthenoid.hellwire.sampling.context.Hash;
import arthenoid.hellwire.sampling.context.MurmurHash;
import arthenoid.hellwire.sampling.samplers.IntegerSampler;
import arthenoid.hellwire.sampling.samplers.RealSampler;
import arthenoid.hellwire.sampling.samplers.Sampler;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Function;
import java.util.function.Supplier;

public class CLI {
  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("Sampler not specified");
      System.exit(1);
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
      samplerArgs = samplerArgs.length > 1 ? samplerArgs[1].substring(0, samplerArgs[1].length() - 1).split(", ") : new String[0];
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
    Scanner fin = null;
    long period = 0, prime = 0, seed = Long.MIN_VALUE;
    Function<Context, Hash> hasher = null;
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
        if (fin != null) {
          System.err.println("Input already set");
          System.exit(1);
        }
        try {
          fin = new Scanner(Path.of(args[++i]));
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
      default:
        System.err.println("Unknown argument: " + args[i]);
        System.exit(1);
    }
    if (prime == 0) prime = 1685727585142753L;
    if (hasher == null) hasher = MurmurHash::new;
    Scanner in = fin == null ? new Scanner(System.in) : fin;
    constructorParams[0] = seed == Long.MIN_VALUE ? new BasicContext(prime, hasher) : new BasicContext(prime, seed, hasher);
    Sampler sampler;
    try {
      sampler = constructor.newInstance(constructorParams);
    } catch (Exception e) {
      System.err.println("Sampler cannot be initialised");
      System.exit(1);
      return;
    }
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