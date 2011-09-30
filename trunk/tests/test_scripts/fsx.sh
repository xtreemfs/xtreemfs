#!/bin/bash
XTREEMFS_DIR=$1

if [ ! -e ${XTREEMFS_DIR}/tests/utils/fsx.bin ]
then
    echo "Compiling fsx.bin..."
    current_dir=`pwd`
    cd ${XTREEMFS_DIR}/tests/utils
    gcc -o fsx.bin ltp-fsx.c
    cd $current_dir
fi

${XTREEMFS_DIR}/tests/utils/fsx.bin -R -W -N 100000 ./fsx.tmpfile