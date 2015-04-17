#!/bin/bash

# Copyright (c) 2015 by Robert Schmidtke, Zuse Institute Berlin
#
# Licensed under the BSD License, see LICENSE file for details.

# This test executes the coverity code scan by pushing the current
# master branch to the coverity_scan branch.

set -e

XTREEMFS_DIR="$1"

cd "$XTREEMFS_DIR"

git checkout coverity_scan

# This script exists in coverity_scan only and takes care of
# not overwriting .travis.yml when merging from master.
./merge.sh master

# Trigger the coverity scan through Travis CI.
git push origin coverity_scan

git checkout master
