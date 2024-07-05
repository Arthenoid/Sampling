package arthenoid.hellwire.sampling.samplers;

import arthenoid.hellwire.sampling.MemoryUser;
import arthenoid.hellwire.sampling.Result;
import java.util.stream.Stream;

public interface Sampler extends MemoryUser {
  double p();
  void update(long index, long frequencyChange);
  Result query();
  Stream<Result> queryAll();
}