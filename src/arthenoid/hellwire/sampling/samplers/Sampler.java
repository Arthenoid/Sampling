package arthenoid.hellwire.sampling.samplers;

import arthenoid.hellwire.sampling.MemoryUser;
import arthenoid.hellwire.sampling.Result;
import java.util.stream.Stream;

/**
 * An L<sub>p<sub> sampler
 */
public interface Sampler extends MemoryUser {
  /**
   * The parameter p for this sampler
   * @return The value of p
   */
  double p();
  void update(long index, long frequencyChange);
  Result query();
  Stream<Result> queryAll();
}