#!/bin/bash

# Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
#
# Licensed under the BSD License, see LICENSE file for details.

# Use this script to kill servers which were not correctly shutdown by a failed xtestenv run.

for pid in $(ps faux | grep XtreemFS.jar | grep -v grep | awk '{ print $2 }')
do
  kill $pid
done

for pid in $(ps faux | grep XtreemFS.jar | grep -v grep | awk '{ print $2 }')
do
  kill -9 $pid
done
