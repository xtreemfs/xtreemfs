#!/bin/bash

# Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
#
# Licensed under the BSD License, see LICENSE file for details.
#
# This file is only used for developers of the Java code.
# It allows to comment and uncomment the JCIP annotations (http://www.jcip.net/).
#
# If JCIP annotations are enabled, the jcip-annotations.jar is required for
# compiling and running the XtreemFS code. However, we do not want to package
# it as part of binary packages. Therefore, this script is used to comment
# the annotations in the code. We do not delete the annotations from the code
# since we want to keep them as documentation - even if currently (Feb 2012)
# there is no tool available to evaluate them.

function display_usage() {
  cat <<EOF
toogle_jcip_annotations.sh:

-h this screen
-c <path>         Comments all JCIP annotations found in .java files in path
-u <path>         Uncomments all JCIP annotations found in .java files in path
EOF
}

if [ -z "$1" ]
then
  display_usage
  exit 1
fi

while getopts ":hc:u:" opt
do
  case $opt in
    c)
      mode="comment"
      path="$OPTARG"
      ;;
    u)
      mode="uncomment"
      path="$OPTARG"
      ;;
    h)
      display_usage
      exit 1
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
    :)
      echo "Option -$OPTARG requires a path as an argument." >&2
      exit 1
      ;;
  esac
done

if [ -z "$mode" ]
then
  echo "Error: Run with -c <path> or -u <path>."
  exit 1
fi

if [ ! "$path" ]
then
  echo "Error: The given path \"$path\" does not exist."
  exit 1
fi

comment_prefix="\/\/ JCIP "
if [ "$mode" = "comment" ]
then
#  find "$path" -iname "*.java" -exec sed -r -i -e "s/^([^\/][^\/].*@(GuardedBy|Immutable|NotThreadSafe|ThreadSafe).*)$/$comment_prefix\1/" {} \;
  find "$path" -iname "*.java" -exec sed -r -i -e "s/^([^\/][^\/].*(@|net\.jcip\.annotations\.)(GuardedBy|Immutable|NotThreadSafe|ThreadSafe).*)$/$comment_prefix\1/" {} \;
else
  find "$path" -iname "*.java" -exec sed -r -i -e "s/^$comment_prefix([^\/][^\/].*(@|net\.jcip\.annotations\.)(GuardedBy|Immutable|NotThreadSafe|ThreadSafe).*)$/\1/" {} \;
fi