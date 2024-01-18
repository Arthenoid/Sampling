# Sampling

A Java library for sampling of large data streams using small memory.

## Build

To build run the `build` script (`.sh` for Linux-based and `.bat` for Windows).

## Run CLI

```
java -jar Sampling.jar <sampler>(<parameter>..) [<argument>]...
```

### Arguments

| Name | Description |
| :-: | :-: |
| `period <p>` | Prints query every *p* updates. If not set, prints only at the end. |
| `P <p>` | Alias for `period` |
| `in <path>` | Reads updates from *path*. If not set, uses stdin. |
| `prime <p>` | Sets the prime number. |
| `seed <seed>` | Sets the seed for random number generation. If not set, chooses random. |
| `hash <hash>` | Sets the hash. If not set, uses Murmur hash. |
| `H <hash>` | Alias for `hash` |

### Available samplers

- Distinct(n)
- Precision(n, δ, ε)

### Available hashes

- Linear
- Murmur

### Example

```
java -jar Sampling.jar "Distinct(42)" H Linear seed 42 in  data/updates.txt
```