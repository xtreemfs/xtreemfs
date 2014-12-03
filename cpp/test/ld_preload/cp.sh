#!/bin/bash
BUILD_DIR="../../build"
#LD_DEBUG=all 
XTREEMFS_PRELOAD_OPTIONS="--log-level DEBUG demo.xtreemfs.org/demo /xtreemfs" LD_PRELOAD=$BUILD_DIR"/libxtreemfs_preload.so" cp $1 $2
