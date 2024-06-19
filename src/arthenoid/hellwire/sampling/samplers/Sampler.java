package arthenoid.hellwire.sampling.samplers;

import arthenoid.hellwire.sampling.MemoryUser;
import arthenoid.hellwire.sampling.Result;
import java.util.stream.Stream;

public interface Sampler extends MemoryUser {
  double p();
  void update(long i, long w);
  Result query();
  Stream<Result> queryAll();
}