package arthenoid.hellwire.sampling.cli;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ArgParser {
  protected final Map<String, Opt> options = new HashMap<>();
  
  public ArgParser add(Opt... opts) throws IllegalArgumentException {
    for (Opt opt : opts) for (String name : opt.names) if (options.putIfAbsent(name, opt) != null) throw new IllegalArgumentException();
    return this;
  }
  
  public static ArgParser create(Opt... opts) {
    return (new ArgParser()).add(opts);
  }
  
  public String parse(Iterator<String> args) {
    while (args.hasNext()) {
      String arg = args.next();
      if (!arg.startsWith("-")) return "Non-option argument: " + arg;
      if (arg.startsWith("--")) {
        String name = arg.substring(2);
        String[] split = name.split("=", 2);
        name = split[0];
        if (name.length() < 2) return "Invalid long option name: " + name;
        Opt opt = options.get(name);
        if (opt == null) return "Unknown long option: " + name;
        if (opt.present) return "Repeated option " + name;
        opt.present = true;
        if (opt instanceof Opt.Val) {
          Opt.Val<?> valOpt = (Opt.Val) opt;
          String value;
          if (split.length < 2) {
            if (!args.hasNext()) return "Missing value for long option " + name;
            value = args.next();
          } else value = split[1];
          try {
            valOpt.convertValue(value);
          } catch (Exception e) {
            return "Invalid value for long option " + name + ": " + e.getMessage();
          }
        } else if (split.length > 1) return "Value for a flag " + name;
      } else for (int i = 1; i < arg.length(); i++) {
        String name = arg.substring(i, i + 1);
        if (name.equals("=")) return "Value for flag " + arg.substring(i - 1, i);
        Opt opt = options.get(name);
        if (opt == null) return "Unknown  short option: " + name;
        if (opt.present) return "Repeated option " + name;
        opt.present = true;
        if (opt instanceof Opt.Val) {
          Opt.Val<?> valOpt = (Opt.Val) opt;
          String value = arg.substring(i + 1);
          if (value.isEmpty()) {
            if (!args.hasNext()) return "Missing value for short option " + name;
            value = args.next();
          } else if (value.charAt(0) == '=') value = value.substring(1);
          try {
            valOpt.convertValue(value);
          } catch (Exception e) {
            return "Invalid value for short option " + name + ": " + e.getMessage();
          }
          break;
        }
      }
    }
    return null;
  }
}