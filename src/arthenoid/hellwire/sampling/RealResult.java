package arthenoid.hellwire.sampling;

public class RealResult extends Result<Double> {
  public final double weight;
  
  public RealResult(long i, double weight) {
    super(i);
    this.weight = weight;
  }
  
  @Override
  public Double getWeight() {
    return weight;
  }
}