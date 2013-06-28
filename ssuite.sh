#!/bin/bash

set +x

size="10g"
sleep=0

echo "" > result.csv
echo "" > bench.log

bin/xtfs_benchmark -sw -ssize $size --no-cleanup bench1
sleep 60
bin/xtfs_benchmark -sw -ssize $size --no-cleanup bench2
sleep 60
bin/xtfs_benchmark -sw -ssize $size --no-cleanup bench3
sleep 60
bin/xtfs_benchmark -sw -ssize $size --no-cleanup bench4
sleep 60
bin/xtfs_benchmark -sw -ssize $size --no-cleanup bench5
sleep 60
bin/xtfs_benchmark -sw -ssize $size --no-cleanup bench6
sleep 360

for i in {1,2,3,4,5,6}; do
  for j in {1,2,3}; do
    echo "Start $i-Thread-Benchmark Nr. $j"
    bin/xtfs_benchmark -sr -ssize $size -p $i --no-cleanup bench1 bench2 bench3 bench4 bench5 bench6 >> result.csv 2>> bench.log ;
    echo "Finished $i-Thread-Benchmark Nr. $j"
    sleep $(echo "2*$i*60"|bc)
    echo "Finished Sleeping"
    echo "Dropping caches"
    bash -c 'echo 3 > /proc/sys/vm/drop_caches'
  done ;
done

