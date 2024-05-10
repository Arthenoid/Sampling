package arthenoid.hellwire.sampling.cli;

import arthenoid.hellwire.sampling.datagen.Format;
import java.lang.reflect.InvocationTargetException;

public class Run {
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
}