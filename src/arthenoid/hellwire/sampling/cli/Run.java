package arthenoid.hellwire.sampling.cli;

import arthenoid.hellwire.sampling.Result;
import arthenoid.hellwire.sampling.datagen.Format;
import arthenoid.hellwire.sampling.datagen.SUFormat;
import arthenoid.hellwire.sampling.samplers.Sampler;
import java.lang.reflect.InvocationTargetException;

public class Run {
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
}