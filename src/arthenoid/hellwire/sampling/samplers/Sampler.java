package arthenoid.hellwire.sampling.samplers;

import arthenoid.hellwire.sampling.MemoryUser;
import arthenoid.hellwire.sampling.Result;

public interface Sampler<T extends Number> extends MemoryUser {
  double p();
  void update(long i, T w);
  Result query();
}