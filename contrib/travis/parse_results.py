#!/usr/bin/env python

# Copyright (c) 2014 by Johannes Dillmann, Zuse Institute Berlin
# Licensed under the BSD License, see LICENSE file for details.

import sys

import argparse
import json


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("file", type=argparse.FileType('r'))
    parser.add_argument("test")
    args = parser.parse_args()

    results = json.load(args.file)
    result = None

    if args.test in results:
        result = results[args.test]

    if type(result) == bool and result:
        print "true"
        sys.exit(0)

    if type(result) == dict and all(result.values()):
        print "true"
        sys.exit(0)

    print "false"
    sys.exit(1)

