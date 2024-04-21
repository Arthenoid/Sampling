# Sampling

A Java library for sampling of large data streams using small memory.

## Build

To build run the `build` script (`.sh` for Linux-based and `.bat` for Windows).

## CLI

```
java -jar Sampling.jar <command> [<argument>]...
```

### Running samplers [`sample`]

```
java -jar Sampling.jar sample <sampler> [<argument>]...
```

|          Option         | Description                                                               |
| :---------------------: | :------------------------------------------------------------------------ |
|    `--in, -i <path>`    | Reads updates from the given file instead of the standard input.          |
|    `--out, -o <path>`   | Outputs to the given file instead of the standard output.                 |
| `--domain-size, -n <n>` | Sets the domain size.                                                     |
|  `--delta, -d, -δ <δ>`  | Sets the sampler delta (δ). Default is 0.01 (1%).                         |
| `--epsilon, -e, -ε <ε>` | Sets the sampler epsilon (ε). Default is 0.01 (1%).                       |
|    `--period, -P <p>`   | Prints query every *p* updates. If not specified, prints only at the end. |
|   `--seed, -s <seed>`   | Sets the sampler seed. If not specified, it is chosen randomly.           |
|    `--prime, -p <p>`    | Sets the prime number used for hashing and similar purposes.              |
|   `--hash, -h <hash>`   | Sets the hash function. If not specified, MurmurHash is used.             |
|       `--gen, -g`       | Consumes generated test data.                                             |
|    `--k-mer, -k <k>`    | Reads FASTA files and samples *k*-mers.                                   |

For standard mode (no `-g` or `-x`) you have to specify the domain size (`-n`).
The input is interpreted as a (textual) stream of white space separated updates.
Each update consists of the (zero based) index of an item followed by the change to its frequency.

#### Available samplers

- Distinct
- Precision

#### Available hash functions

- Linear
- Murmur
- MultiplyShift
- Tabulation

### Test data generation [`gen`]

```
java -jar Sampling.jar gen <format> <domain size> <# of updates>
```

|        Option       | Description                                                       |
| :-----------------: | :---------------------------------------------------------------- |
|  `--out, -o <path>` | Outputs to the given file instead of the standard output.         |
| `--seed, -s <seed>` | Sets the generator seed. If not specified, it is chosen randomly. |

If you use the character `n` as the number of updates, each index will get only one update with the final frequency.

The generated data then can be read for sampling with the `--gen` flag;

#### Available formats

- Units: Each index has final frequency around one.
- UnitsAndRoots: There are about √n indices with frequency around √n, the rest are around one.
- Outlier: There is one index with frequency around n, the rest are around one.

### Example

```
java -jar Sampling.jar sample Distinct -n=42 -hLinear --seed 42 --in=data/updates.txt
```