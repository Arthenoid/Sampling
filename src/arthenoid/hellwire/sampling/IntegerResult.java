package arthenoid.hellwire.sampling;

public class IntegerResult extends Result<Long> {
  public final long weight;
  
  public IntegerResult(long i, long weight) {
    super(i);
    this.weight = weight;
  }
  
  @Override
  public Long getWeight() {
    return weight;
  }
}