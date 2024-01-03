package arthenoid.hellwire.sampling.samplers;

import arthenoid.hellwire.sampling.IntegerResult;

public interface IntegerSampler extends Sampler<Long> {
  void update(long i, long w);
  
  @Override
  default void update(long i, Long w) {
    update(i, (long) w);
  }
  
  @Override
  IntegerResult query();
}
