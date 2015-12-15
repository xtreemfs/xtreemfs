#!/bin/bash

# the timeout for one execution of xtfs_benchmark
TIMEOUT=1500

# the timeout for one cleanup
TIMEOUT_CLEANUP=300

# the time to sleep after a cleanup
SLEEPTIME=600

# if false, the script will not sleep after a cleanup and after drop_caches
# (-v will set false)
SLEEP=true

# the size of the basefile for random benchmarks
BASEFILE_SIZE="100g"

# the directories for the logfiles and the results
LOG_BASE=${BENCH_LOG:-$HOME}
LOG_DIR="$LOG_BASE/log"
RESULT_DIR="$LOG_BASE/result"

# Drops caches after each benchmark. Uncomment to activate
# cp "drop_caches" to "/usr/local/bin" and add "ALL ALL=NOPASSWD: /usr/local/bin/drop_caches" to sudoers file
DROP_CACHES=${BENCH_DROP_CACHES:-"/usr/local/bin/drop_caches"}
if [[ $DROP_CACHES != "false" ]]; then
  DROP_CACHES_CALL="sudo ${DROP_CACHES}"
fi

# IP and Port of the DIR
DIR=${BENCH_DIR:-"localhost:32638"}

# IP and Port of the MRC
MRC=${BENCH_MRC:-"localhost:32636"}

# space separed list of OSD_UUIDS, e.g. "osd1 osd2 ..."
OSD_UUIDS=${BENCH_OSD_UUIDS:-"test-osd0"}

# stripe size for a volume
STRIPE_SIZE="128K"

# request size for each I/O operation
REQUEST_SIZE=$STRIPE_SIZE

# replication settings
REPLICATION_POLICY=""
REPLICATION_FACTOR=1

# general parameters passed to xtfs_benchmark (e.g. --use-jni)
XTFS_BENCH_PARAMS=""

check_env(){
  # check XTREEMFS
  if [ -z "$XTREEMFS" ]; then
    if [ -d java -a -d cpp -a -d etc ]; then
      #echo "Looks like you are in an XtreemFS base directory..."
      XTREEMFS=`pwd`
    elif [ -d ../java -a -d ../cpp -a -d ../etc ]; then
      #echo "XTREEMFS base could be the parent directory..."
      XTREEMFS=`pwd`/..
    fi
  fi
  if [ ! -e "$XTREEMFS/java/servers/dist/XtreemFS.jar" -a ! -d "$XTREEMFS/java/lib" -a ! -f "/usr/share/java/XtreemFS.jar" ];
  then
    echo "XtreemFS jar could not be found!"
    exit 1
  fi

  # check JAVA_HOME
  if [ -z "$JAVA_HOME" -a ! -f "/usr/bin/java" ]; then
    echo "\$JAVA_HOME not set, JDK/JRE 1.6 required"
    exit 1
  fi

  if [ -z "$JAVA_HOME" ]; then
    JAVA_HOME=/usr
  fi

}

printUsage() {
  cat << EOF

Synopsis
  $(basename $0) -t TYPE -s NUMBER [-x NUMBER] [-p POLICY -f NUMBER] [-b NUMBER -e NUMBER] [-r NUMBER] [-v]
  Run a XtreemFS benchmark series, i.e. a series of benchmarks with increasing
  numbers of threads. Logs are placed in \$HOME/log/, results in \$HOME/results
  (can be changed at the head of the script).

  -t type
    Type of benchmarks to run. Type can be either of the following:
      sw sequential write
      usw unaligned sequential write
      sr sequential read
      rw random write
      rr random read

  -s size
    Size of one benchmark, modifier K (for KiB), M (for MiB) or G (for GiB) is mandatory.

  -c size
    Size of each read/write request, modifier K (for KiB), M (for MiB) or G (for GiB) is mandatory.
    Defaults to 128K.

  -i size
    Stripe size for each volume, modifier K (for KiB), M (for MiB) or G (for GiB) is mandatory.
    Defaults to 128K.

  -p policy
    Replication policy to use. Defaults to none.

  -f factor
    Replication factor to use. Defaults to 1.

  -b number of threads to beginn the benchmark series
    Minimum number of threads to be run as the benchmarks series.
    The series will run benchmarks between the 'begin' and the 'end' number of threads.

  -e number of threads to end the benchmark series
    Maximum number of threads to be run as the benchmarks series.
    The series will run benchmarks between the 'begin' and the 'end' number of threads.

  -r repetitions
    Number of times a benchmark is repeated.

  -v verbose
    If set, bash debugging is enabled ('set -x') and sleeping after the benchmarks
    is disabled.

EOF
}

init_params(){

  check_env

  if ! [ -d $LOG_DIR ]; then
    echo "$LOG_DIR doesn't existing. Creating $LOG_DIR..."
    mkdir -p $LOG_DIR
  fi
  if ! [ -d $RESULT_DIR ]; then
    echo "$RESULT_DIR doesn't existing. Creating $RESULT_DIR"
    mkdir -p $RESULT_DIR
  fi

  THREADS="$(seq $BEGIN $END)"
  REPETITIONS="$(seq 1 $REPETITIONS)"

  # use second resolution in case multiple benchmarks are run per minute
  NOW=$(date +"%y-%m-%d_%H-%M-%S")
  # redirect stdout and stderr
  exec 2> >(tee $LOG_DIR/$TYPE-$NOW.log)
  exec > >(tee $RESULT_DIR/$TYPE-$NOW.csv)

  BASEFILE_SIZE=$(parse_size $BASEFILE_SIZE)
  REQUEST_SIZE=$(parse_size $REQUEST_SIZE)
  STRIPE_SIZE=$(parse_size $STRIPE_SIZE)

}


parse_size(){
  local size_with_modifier=$1
  local index=$(echo `expr match $size_with_modifier '[0-9]\+'`)
  local size=${size_with_modifier:0:$index}
  local modifier=${size_with_modifier:$index}

  if [ $index != ${#size_with_modifier} ]; then
    if [ $modifier = "K" ] || [ $modifier = "k" ]; then
      size=$(echo "$size*2^10" | bc)
    elif [ $modifier = "M" ] || [ $modifier = "m" ]; then
      size=$(echo "$size*2^20" | bc)
    elif [ $modifier = "G" ] || [ $modifier = "g" ]; then
      size=$(echo "$size*2^30" | bc)
    else
      echo "Wrong size modifier. Only 'M' and 'G' are allowed"
      exit 1
    fi
  fi

  echo $size
}



prepare_seq_read(){

  local size=$1
  local threads=$2

  # declare array of volume names
  local volume_index=$(echo "$threads-1" | bc)
  for i in $(seq 0 $volume_index); do VOLUMES[$i]=benchmark$i; done

  echo -e "\nPreparing sequential read benchmarks\n"  >&2
  for i in $(seq 1 $threads); do
    local index=$(echo "$i-1"|bc)
    timeout --foreground $TIMEOUT $XTREEMFS/bin/xtfs_benchmark $XTFS_BENCH_PARAMS -sw -ssize $1 --no-cleanup --user $USER \
      ${VOLUMES[$index]} --stripe-size $STRIPE_SIZE --chunk-size $REQUEST_SIZE
  done
}

prepare_random(){

  local threads=$1

  # declare array of volume names
  local volume_index=$(echo "$threads-1" | bc)
  for i in $(seq 0 $volume_index); do VOLUMES[$i]=benchmark$i; done

  # calc basefile size and round to a number divideable through REQUEST_SIZE
  local basefile_size=$(echo "(($BASEFILE_SIZE/$threads)/$REQUEST_SIZE)*$REQUEST_SIZE" | bc)

  echo -e "\nPreparing random benchmark: Creating a basefiles\n"  >&2
  for i in $(seq 1 $threads); do
    local index=$(echo "$i-1"|bc)
    timeout --foreground $TIMEOUT $XTREEMFS/bin/xtfs_benchmark $XTFS_BENCH_PARAMS -rr -rsize $REQUEST_SIZE --no-cleanup-basefile --no-cleanup-volumes --user $USER \
      --basefile-size $basefile_size ${VOLUMES[$index]} --stripe-size $STRIPE_SIZE --chunk-size $REQUEST_SIZE
  done
}


run_benchmark(){
  local benchType=$1
  local size=$2
  local threads=$3
  local replicationOpt=""
  if [[ $REPLICATION_POLICY != "" ]]; then
    replicationOpt="--replication-policy $REPLICATION_POLICY"
  fi

  if [ $benchType = "sr" ]; then
    XTREEMFS=$XTREEMFS timeout --foreground $TIMEOUT $XTREEMFS/bin/xtfs_benchmark $XTFS_BENCH_PARAMS -$benchType -ssize $size -n $threads --no-cleanup-volumes --user $USER \
     $replicationOpt --replication-factor $REPLICATION_FACTOR --chunk-size $REQUEST_SIZE --stripe-size $STRIPE_SIZE
  elif [ $benchType = "sw" ] || [ $benchType = "usw" ]; then
    XTREEMFS=$XTREEMFS timeout --foreground $TIMEOUT $XTREEMFS/bin/xtfs_benchmark $XTFS_BENCH_PARAMS -$benchType -ssize $size -n $threads --user $USER \
     $replicationOpt --replication-factor $REPLICATION_FACTOR --chunk-size $REQUEST_SIZE --stripe-size $STRIPE_SIZE
  elif [ $benchType = "rw" ] || [ $benchType = "rr" ]; then
    # calc basefile size and round to a number divideable through REQUEST_SIZE
    local basefile_size=$(echo "(($BASEFILE_SIZE/$threads)/$REQUEST_SIZE)*$REQUEST_SIZE" | bc)
    XTREEMFS=$XTREEMFS timeout --foreground $TIMEOUT $XTREEMFS/bin/xtfs_benchmark $XTFS_BENCH_PARAMS -$benchType -rsize $size --basefile-size $basefile_size -n $threads \
      --no-cleanup-basefile --no-cleanup-volumes --user $USER \
      $replicationOpt --replication-factor $REPLICATION_FACTOR --chunk-size $REQUEST_SIZE --stripe-size $STRIPE_SIZE
  fi

  local bench_exit_status=$?
  if [ $bench_exit_status -eq 124 ]; then
    echo "The benchmark timed out (Timeout: $TIMEOUT)" >&2
    interrupted_exit
  elif [ $bench_exit_status -ne 0 ]; then
    echo "The benchmark did not finish with exit status 0" >&2
    interrupted_exit
  fi

  # cleanup after *every* benchmark only for seq write benchmark
  if [ $benchType = "sw" ] || [ $benchType = "usw" ]; then
    cleanup_osd
  fi
}

delete_volumes(){
  local number_of_threads=$1
  local volume_index=$(echo "$number_of_threads-1" | bc)
  for i in $(seq 0 $volume_index); do
    rmfs.xtreemfs -f $MRC/benchmark$i >/dev/null
    if [ $? -eq 0 ]; then
      echo "Removed volume benchmark$i"  >&2
    fi
  done
}

cleanup_osd(){
  for osd in $OSD_UUIDS; do
    timeout --foreground $TIMEOUT_CLEANUP $XTREEMFS/bin/xtfs_cleanup -dir pbrpc://$DIR -wait -e -delete_volumes uuid:$osd  >&2
  done
  if $SLEEP; then
    echo "Start Sleeping for $(echo "$SLEEPTIME/60"|bc) minutes at $(date)" >&2
    sleep $SLEEPTIME
    echo "Finished Sleeping at $(date)" >&2
  fi
  drop_caches
}

interrupted_exit(){
  echo "Unexpected exit, cleaning up..."  >&2
  SLEEP=false
  delete_volumes $END
  cleanup_osd
  exit 1
}

drop_caches(){
  if [ -n "$DROP_CACHES_CALL" ]; then
    echo "Dropping caches" >&2
    $DROP_CACHES_CALL
    if $SLEEP; then
      sleep 10
    fi
  fi
}

##### main ###

trap "echo; echo 'Interrupt received '; interrupted_exit" INT

# show usage if invoked without options/arguments
if [ $# -eq 0 ]; then
  printUsage
  exit 1
fi

# default values
BEGIN=1
END=1
REPETITIONS=1


# parse options
while getopts ":t:s:c:i:b:e:r:p:f:v" opt; do
  case $opt in
    t)
      if [ $OPTARG = "sw" ] || [ $OPTARG = "usw" ] || [ $OPTARG = "sr" ]  || [ $OPTARG = "rw" ] || [ $OPTARG = "rr" ]; then
        TYPE=$OPTARG
      else
        echo 'wrong argument to -t. Needs to be either "sw", "usw", "sr", "rw" or "rr"'
        exit 1
      fi
      ;;
    s)
      SIZE=$(parse_size $OPTARG)
      ;;
    c)
      REQUEST_SIZE=$(parse_size $OPTARG)
      ;;
    i)
      STRIPE_SIZE=$(parse_size $OPTARG)
      ;;
    b)
      BEGIN=$OPTARG
      ;;
    e)
      END=$OPTARG
      ;;
    r)
      REPETITIONS=$OPTARG
      ;;
    p)
      REPLICATION_POLICY=$OPTARG
      ;;
    f)
      REPLICATION_FACTOR=$OPTARG
      ;;
    v)
      SLEEP=false
      set -x
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
drop_caches

echo "Running:" $0 $@  >&2

for i in $THREADS; do
  size="$(echo "$SIZE/$i"|bc)"
  if [ $TYPE != "usw" ]; then
    size="$(echo "($size/$REQUEST_SIZE)*$REQUEST_SIZE" | bc)" # round down to a size divideable through the REQUEST_SIZE
  fi

  if [ $TYPE = "sr" ]; then
    prepare_seq_read $size $i
    cleanup_osd
  elif [ $TYPE = "rw" ] || [ $TYPE = "rr" ]; then
    prepare_random $i
  fi

  for j in $REPETITIONS; do
    echo "Start $i-Thread-Benchmark Nr. $j" >&2

    run_benchmark $TYPE $size $i

    echo "Finished $i-Thread-Benchmark Nr. $j" >&2

  done

  # seq write benchmarks run cleanup after every benchmark, so this would be redundant
  if [ $TYPE != "sw" ] && [ $TYPE != "usw" ]; then
    volume_index=$(echo "$i-1" | bc)
    for i in $(seq 0 $volume_index); do
      rmfs.xtreemfs -f $MRC/benchmark$i >&2
      echo "Remove volume benchmark$i"  >&2
    done
    cleanup_osd
  fi

done
