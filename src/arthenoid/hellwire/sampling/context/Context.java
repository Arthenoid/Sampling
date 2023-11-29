package arthenoid.hellwire.sampling.context;

public interface Context {
  long getPrime();
  
  long random(long bound);
  
  default long randomP() {
    return random(getPrime() - 1) + 1;
  }
  
  Hash newHash();
  
  Hash staticHash(int key);
  
  default long powP(long a, long b) {
    long prime = getPrime(), ret = 1;
    while (b > 0) {
      if ((b & 1) != 0) ret = (ret * a) % prime;
      b >>= 1;
      a = (a * a) % prime;
    }
    return ret;
  }
}