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
java -jar Sampling.jar sample <sampler> <domain size> [<argument>]...
```

|          Option         | Description                                                               |
| :---------------------: | :------------------------------------------------------------------------ |
|    `--in, -i <path>`    | Reads updates from the given file instead of the standard input.          |
|    `--out, -o <path>`   | Outputs to the given file instead of the standard output.                 |
|  `--delta, -d, -δ <δ>`  | Sets the sampler delta (δ)                                                |
| `--epsilon, -e, -ε <ε>` | Sets the sampler epsilon (ε)                                              |
|    `--period, -P <p>`   | Prints query every *p* updates. If not specified, prints only at the end. |
|   `--seed, -s <seed>`   | Sets the sampler seed. If not specified, it is chosen randomly.           |
|    `--prime, -p <p>`    | Sets the prime number used for hashing and similar purposes.              |
|   `--hash, -h <hash>`   | Sets the hash. If not specified, Murmur hash is used.                     |
|       `--gen, -g`       | Consumes generated test data.                                             |
|    `--k-mer, -k <k>`    | Reads FASTA files and samples *k*-mers.                                   |

#### Available samplers

- Distinct
- Precision

#### Available hashes

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

If you use `n` as the number of updates, each index will get only one update with the final value.

The generated data then can be read for sampling with the `--gen` flag;

#### Available formats

- Units: Each index has final frequency around one.
- UnitsAndRoots: There are about √n indices with frequency around √n, the rest are around one
- Outlier: There is one index with frequency around n, the rest are around one

### Example

```
java -jar Sampling.jar sample Distinct 42 -hLinear --seed 42 --in=data/updates.txt
```