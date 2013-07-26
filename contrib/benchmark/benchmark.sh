#!/bin/bash

set -x

check_env(){
  if [ -z $XTREEMFS ]; then
    echo "\$XTREEMFS not set. Set \$XTREEMFS or invoke with XTREEMFS=/path/to/XTREEMFS ./benchmark.sh ..."
    exit 1
  fi
  if [ -z $JAVA_HOME ]; then
    echo "\$JAVA_HOME not set. Set \$JAVA_HOME or invoke with JAVA_HOME=/path/to/XTREEMFS ./benchmark.sh ..."
    exit 1
  fi
}



printUsage() {
  cat << EOF

Synopsis
  `basename $0` -t TYPE -s NUMBER [-b NUMBER -e NUMBER] [-r NUMBER] -c CONFIGFILE [-v]
  Run a XtreemFS benchmark suite, i.e. a series of benchmarks with increasing
  numbers of threads. Logs are placed in PWD/log/, results in PWD/results.

  -t type
    Type of benchmarks to run. Type can be either of the following:
      sw sequential write
      sr sequential read
      rw random write
      rr random read

  -s size
  Size of one benchmark, modifier m (for MiB) or g (for GiB) is mandatory.

  -b number of threads to beginn the benchmark suite
    Minimum number of threads to be run as the benchmarks suite.
    The suite will run benchmarks between the 'begin' and the 'end' number of threads.

  -e number of threads to end the benchmark suite
    Maximum number of threads to be run as the benchmarks suite.
    The suite will run benchmarks between the 'begin' and the 'end' number of threads.

  -r repetitions
    Number of times a benchmark is repeated.

  -c config file
    Config file to use.

  -v verbose
    If set, bash debugging is enabled ('set -x') and sleeping after the benchmarks
    is disabled.

EOF
}

init_params(){

  if [ -f $CONFIG ]; then
    source $CONFIG
  else
    echo "Configuration file not found" >&2
    exit 1
  fi

  check_env

  if ! [ -d $LOG_DIR ]; then
    echo "$LOG_DIR doesn't existing. Creating $LOG_DIR..."
    mkdir -p $LOG_DIR
  fi
  if ! [ -d $RESULT_DIR ]; then
    echo "$RESULT_DIR doesn't existing. Creating $RESULT_DIR"
    mkdir -p $LOG_DIR
  fi

  THREADS="$(seq $BEGIN $END)"
  REPETITIONS="$(seq 1 $REPETITIONS)"
  NOW=$(date +"%y-%m-%d_%H-%M")
  LOGFILE="$LOG_DIR/$TYPE-$NOW.log"
  RESULTS="$RESULT_DIR/$TYPE-$NOW.csv"

}

prepare_seq_read(){
  declare -a VOLUMES=(bench1 bench2 bench3 bench4 bench5 bench6 bench7 bench8 bench9 bench10);
  size=$1
  threads=$2
  echo -e "\nPreparing sequential read benchmarks\n" | tee -a $LOGFILE
  for i in $(seq 1 $threads); do
    index=$(echo "$i-1"|bc)
    timeout $TIMEOUT $XTREEMFS/bin/xtfs_benchmark -sw -ssize $1 --no-cleanup \
      ${VOLUMES[$index]} >> $LOGFILE 2>> $LOGFILE
  done
}

run_benchmark(){
  benchType=$1
  size=$2
  threads=$3
  export $XTREEMFS
  if [ $benchType = "sw" ] || [ $benchType = "sr" ]; then
    XTREEMFS=$XTREEMFS timeout $TIMEOUT $XTREEMFS/bin/xtfs_benchmark -$benchType -ssize $size -p $threads --no-cleanup-volumes \
      bench1 bench2 bench3 bench4 bench5 bench6 bench7 bench8 bench9 bench10 >> $RESULTS 2>> $LOGFILE
  elif [ $benchType = "rw" ] || [ $benchType = "rr" ]; then
    # assumes existing basefiles. If no basefiles exits the first run will produce invalid results
    timeout $TIMEOUT $XTREEMFS/bin/xtfs_benchmark -$benchType -rsize $size --basefile-size $BASEFILE_SIZE -p $threads \
      --no-cleanup-basefile --no-cleanup-volumes \
      bench1 bench2 bench3 bench4 bench5 bench6 bench7 bench8 bench9 bench10 >> $RESULTS 2>> $LOGFILE
  fi

  if [ $? -eq 0 ]; then
    until cleanup_osd; do
      echo "ERROR: Cleanup of OSD failed. Repeating cleanup... " | tee -a $LOGFILE
      check_osd
    done
    echo "Cleanup succeded" | tee -a $LOGFILE
  else
    return 1
  fi
}

cleanup_osd(){
  timeout $TIMEOUT_CLEANUP $CLEANUP_CALL
  for osd in $OSD_UUIDS; do
    timeout $TIMEOUT_CLEANUP $XTREEMFS/bin/xtfs_cleanup -dir pbrpc://$DIR -wait -e -delete_volumes uuid:$osd
  done
}

restart_osd(){
  $RESTART_OSD_CALL
  echo "OSD restarted" | tee -a $LOGFILE
  sleep 10
}

check_osd(){
  osd_pid=$(ps aux | grep -v grep | grep osd0.properties | awk '{ print $2 }')
  if [ -z $osd_pid ]; then
    echo -n "ERROR: OSD not running. Restarting OSD... " | tee -a $LOGFILE
    restart_osd
  fi
}

drop_caches(){
  $DROP_CACHES_CALL
}

##### main ###

# show usage if invoked without options/arguments
if [ $NUMBER_OF_ARGUMENTS -eq 0 ]; then
  printUsage
  exit 1
fi

# default values
BEGIN=1
END=1
REPETITIONS=1
SLEEP=true


# parse options
while getopts ":t:s:b:e:r:c:v" opt; do
  case $opt in
    t)
      if [ $OPTARG = "sw" ] || [ $OPTARG = "sr" ]  || [ $OPTARG = "rw" ] || [ $OPTARG = "rr" ]; then
        TYPE=$OPTARG
      else
        echo 'wrong argument to -t. Needs to be either "sw", "sr", "rw" or "rr"'
        exit 1
      fi
      echo $TYPE
      ;;
    s)
      index=$(echo `expr match $OPTARG '[0-9]\+'`)
      modifier=${OPTARG:$index}
      if [ $modifier = "m" ]; then
        SIZE_MODIFIER="m"
      elif [ $modifier = "g" ]; then
        SIZE_MODIFIER="g"
      else
        echo "Wrong size modifier. Only 'm' and 'g' are allowed"
        exit 1
      fi
      SIZE=${OPTARG:0:$index}
      echo $SIZE $MODIFIER
      ;;
    b)
      BEGIN=$OPTARG
      echo $BEGIN
      ;;
    e)
      END=$OPTARG
      echo $END
      ;;
    r)
      REPETITIONS=$OPTARG
      echo $REPETITIONS
      ;;
    v)
      SLEEP=false
      set -x
      ;;
    c)
      CONFIG=$OPTARG
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      exit 1
      ;;
  esac
done

init_params
check_osd
drop_caches

if $SLEEP; then
  sleep 10
fi

for i in $THREADS; do
  size="$(echo "$SIZE/$i"|bc)$SIZE_MODIFIER"

  if [ $TYPE = "sr" ]; then
    until prepare_seq_read $size $i; do
      echo "ERROR: Preparing seq read benchmark failed. Repeating..." | tee -a $LOGFILE
    done
  fi

  for j in $REPETITIONS; do
    echo "Start $i-Thread-Benchmark Nr. $j"

    until run_benchmark $TYPE $size $i; do
      echo "ERROR: Benchmark failed. Repeating benchmark... " | tee -a $LOGFILE
      check_osd
    done

    echo "Finished $i-Thread-Benchmark Nr. $j"
    if $SLEEP; then
      echo "Start Sleeping for 15 minutes at $(date)"
      sleep $SLEEPTIME
      echo "Finished Sleeping at $(date)"
    fi
    echo "Dropping caches"
    drop_caches
  done ;
done

