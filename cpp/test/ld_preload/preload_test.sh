#!/bin/bash
BUILD_DIR="../../build"
#LD_DEBUG=all 
XTREEMFS_PRELOAD_OPTIONS="--log-level ERR demo.xtreemfs.org/demo /xtreemfs" LD_PRELOAD=$BUILD_DIR"/libxtreemfs_preload.so" $BUILD_DIR/preload_test $1
