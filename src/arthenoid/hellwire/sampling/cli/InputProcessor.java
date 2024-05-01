package arthenoid.hellwire.sampling.cli;

import static arthenoid.hellwire.sampling.cli.CLI.getFormat;
import arthenoid.hellwire.sampling.datagen.Format;
import arthenoid.hellwire.sampling.samplers.IntegerSampler;
import arthenoid.hellwire.sampling.samplers.RealSampler;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public interface InputProcessor {
  boolean hasData();
  void update(IntegerSampler sampler) throws IOException;
  void update(RealSampler sampler) throws IOException;
  default String decode(long x) {
    return Long.toString(x);
  }
  
   class Text implements InputProcessor {
    protected final Scanner in;
    
    public Text(InputStream in) {
      this.in = new Scanner(in, StandardCharsets.UTF_8);
    }
    
    @Override
    public boolean hasData() {
      return in.hasNext();
    }
    
    @Override
    public void update(IntegerSampler sampler) {
      sampler.update(in.nextLong(), in.nextLong());
    }
    
    @Override
    public void update(RealSampler sampler) {
      sampler.update(in.nextLong(), in.nextDouble());
    }
  }
  
  class Gen implements InputProcessor {
    protected final DataInputStream in;
    protected int i = 0;
    protected long x;
    protected double w;
    
    public final String name;
    public final Format format;
    
    public Gen(InputStream in) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException {
      this.in = new DataInputStream(in);
      format = getFormat(name = this.in.readUTF(), this.in.readLong(), this.in.readLong(), this.in.readLong());
    }
    
    @Override
    public boolean hasData() {
      return i < format.updates;
    }
    
    protected void read() throws IOException {
      x = in.readLong();
      w = in.readDouble();
      i++;
    }
    
    @Override
    public void update(IntegerSampler sampler) throws IOException {
      read();
      sampler.update(x, (long) w);
    }
    
    @Override
    public void update(RealSampler sampler) throws IOException {
      read();
      sampler.update(x, w);
    }
  }
  
  class KMer implements InputProcessor {
    protected final BufferedReader in;
    protected final int k;
    protected boolean data = true, nl = true;
    protected int n = 0;
    protected long kMer = 0, reverseKMer = 0;
    
    protected final PrintStream out;
    
    public KMer(InputStream ins, long k, PrintStream out) throws IOException {
      in = new BufferedReader(new InputStreamReader(ins, StandardCharsets.UTF_8));
      this.k = (int) k;
      this.out = out;
      read();
    }
    
    public static final int
      C = 0b00,
      G = 0b01,
      A = 0b10,
      T = 0b11,
      U = T;
    
    protected final void add(long nuc) {
      kMer = (kMer << 2) | nuc;
      reverseKMer = (kMer >> 2) | ((nuc ^ 0b01) << (2 * (k - 1)));
      if (n < k) n++;
        else kMer &= (1 << (2 * k)) - 1;
    }
    
    protected final void read() throws IOException {
      for (;;) {
        boolean onl = nl;
        switch (in.read()) {
          case -1:
            data = false;
            return;
          case '\r':
          case '\n':
            nl = true;
            break;
          case '>':
          case ';':
            if (onl) {
              String comment = in.readLine();
              if (out != null) out.println("[FASTA] " + comment);
            } else kMer = reverseKMer = n = 0;
            break;
          case 'C':
            add(C);
            break;
          case 'G':
            add(G);
            break;
          case 'A':
            add(A);
            break;
          case 'T':
            add(T);
            break;
          case 'U':
            add(U);
            break;
          default:
            kMer = reverseKMer = n = 0;
        }
        if (n == k) return;
      }
    }
    
    @Override
    public boolean hasData() {
      return data;
    }
    
    @Override
    public void update(IntegerSampler sampler) throws IOException {
      read();
      sampler.update(Math.min(kMer, reverseKMer), 1);
    }
    
    @Override
    public void update(RealSampler sampler) throws IOException {
      read();
      sampler.update(Math.min(kMer, reverseKMer), 1);
    }
    
    @Override
    public String decode(long x) {
      StringBuilder ret = new StringBuilder();
      for (int i = 0; i < k; i++, x >>>= 2) switch ((int) (x & 0b11)) {
        case C:
          ret.append('C');
          break;
        case G:
          ret.append('G');
          break;
        case A:
          ret.append('A');
          break;
        case T:
          ret.append('T');
          break;
      }
      return ret.reverse().toString();
    }
  }
}