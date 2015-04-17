#!/bin/bash

# takes the branch to merge from as single argument

# merge branch recursively, using our version of a file
# in case of conflicts. 'our' version is the one contained
# in this 'coverity_scan' branch. this should only be the
# .travis.yml file
# Do not prompt for a commit message.
git merge -s recursive -Xours --no-edit $1
