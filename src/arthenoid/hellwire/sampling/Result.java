package arthenoid.hellwire.sampling;

public abstract class Result<T extends Number> {
  public final long i;
  
  public Result(long i) {
    this.i = i;
  }
  
  public abstract T getWeight();
}