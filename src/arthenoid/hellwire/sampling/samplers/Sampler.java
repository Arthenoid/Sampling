package arthenoid.hellwire.sampling.samplers;

import arthenoid.hellwire.sampling.Result;

public interface Sampler<T extends Number> {
  void update(long i, T w);
  Result query();
}