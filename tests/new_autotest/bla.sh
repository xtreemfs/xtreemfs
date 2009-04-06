#!/bin/bash

echo $0
tmp=`readlink -f $0`
echo `dirname $tmp`