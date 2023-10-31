package arthenoid.hellwire.sampling;

public interface Context {
  long getPrime();
  
  long random(long bound);
  
  default long randomP() {
    return random(getPrime() - 1) + 1;
  }
  
  Hash newHash();
  
  Hash staticHash(int key);
}