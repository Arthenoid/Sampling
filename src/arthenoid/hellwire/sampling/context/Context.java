package arthenoid.hellwire.sampling.context;

public interface Context {
  long getPrime();
  
  long random();
  
  long random(long bound);
  
  default long randomP() {
    return random(getPrime() - 1) + 1;
  }
  
  double randomReal();
  
  Hash newHash();
  
  Hash staticHash(int key);
}