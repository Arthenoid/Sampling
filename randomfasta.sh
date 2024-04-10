#!/bin/bash

echo '> Random data'
pool=CGATCGAU
for i in $(seq $1); do
    echo -n "${pool:RANDOM % ${#pool}:1}"
done