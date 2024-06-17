#!/bin/bash

shopt -s globstar
set -e

javac -Xlint -d build/ src/**/*.java
cd build/
jar --create --file=../Sampling.jar --main-class=arthenoid.hellwire.sampling.cli.CLI .