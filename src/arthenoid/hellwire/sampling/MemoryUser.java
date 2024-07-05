package arthenoid.hellwire.sampling;

public interface MemoryUser {
  /**
   * Estimates the memory used by the object in its current configuration.
   * In case the used amount can change throughout the program execution, the upper limit is used.
   * The value is in units of eight bytes, which corresponds to one {@code long}, {@code double} or a reference to another object.
   * The memory used by owned (not shared objects is also included.
   * @return The object's memory usage
   */
  int memoryUsed();
}