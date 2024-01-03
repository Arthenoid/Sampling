package arthenoid.hellwire.sampling.samplers;

import arthenoid.hellwire.sampling.RealResult;

public interface RealSampler extends Sampler<Double> {
  void update(long i, double w);
  
  @Override
  default void update(long i, Double w) {
    update(i, (double) w);
  }
  
  @Override
  RealResult query();
}
