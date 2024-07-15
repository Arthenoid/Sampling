package arthenoid.hellwire.sampling.context;

public interface Context {
  long random();
  
  long random(long bound);
  
  double randomReal();
  
  Hash newHash();
  
  Hash staticHash(int key);
}