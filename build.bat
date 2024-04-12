@echo off

for /R src\ %%f in (*.java) do javac -Xlint -d build\ %%f
cd build\
jar --create --file=..\Sampling.jar --main-class=arthenoid.hellwire.sampling.cli.CLI .