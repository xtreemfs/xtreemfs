#!/bin/bash

size="28k"

log=rfwbench.log
result=rfwesult.csv
echo "" > $log
echo "" > $result
# bin/xtfs_benchmark -rfw -ssize $size -p 6 --no-cleanup bench1 bench2 bench3 bench4 bench5 bench6 >> bench.log ;

for i in {1,2,3,4,5,6}; do
  for j in {1,2,3}; do
    echo "Start $i-Thread-Benchmark Nr. $j"
    bin/xtfs_benchmark -rfw -rsize $size -p $i --no-cleanup bench1 bench2 bench3 bench4 bench5 bench6 >> $result 2>> $log ;
    echo "Finished $i-Thread-Benchmark Nr. $j"
    sleeptime=$(echo "2*$i*60"|bc)
    echo "Going to sleep for $sleeptime Secends"
    # sleep $sleeptime
    echo "Finished Sleeping"
  done ;
done


log=rfrbech.log
result=rfresult.csv
echo "" > $log
echo "" > $result

bin/xtfs_benchmark -rfw -ssize $size -p 6 --no-cleanup bench1 bench2 bench3 bench4 bench5 bench6 ;

for i in {1,2}; do
  for j in {1,2,3}; do
    echo "Start $i-Thread-Benchmark Nr. $j"
    bin/xtfs_benchmark -rfw -rsize $size -p $i --no-cleanup bench1 bench2 bench3 bench4 bench5 bench6 >> $result 2>> $log ;
    echo "Finished $i-Thread-Benchmark Nr. $j"
    sleeptime=$(echo "2*$i*60"|bc)
    echo "Going to sleep for $sleeptime Secends"
    # sleep $sleeptime
    echo "Finished Sleeping"
  done ;
done
