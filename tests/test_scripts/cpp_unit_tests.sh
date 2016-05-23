#!/bin/bash

# Copyright (c) 2013 by Michael Berlin, Zuse Institute Berlin
#
# Licensed under the BSD License, see LICENSE file for details.

# This test executes all C++ unit tests. Make sure that you did run
# export BUILD_CLIENT_TESTS=true before running "make client_debug".
# Otherwise, the unit tests won't be built.

set -e

XTREEMFS_DIR="$1"

cd "$XTREEMFS_DIR"

export XTREEMFS_DIR_URL="$2"
export XTREEMFS_MRC_URL="$3"
export XTREEMFS_TEST_DIR="$4"

cd cpp/build
make test ARGS=-VV