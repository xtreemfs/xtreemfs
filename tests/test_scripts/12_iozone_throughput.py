#!/usr/bin/env python
# -*- coding: utf-8  -*-

# Copyright (c) 2009-2011 by Bjoern Kolbeck, Minor Gordon, Zuse Institute Berlin
# Licensed under the BSD License, see LICENSE file for details.

import unittest, subprocess, sys, os


class iozoneThroughputTest(unittest.TestCase):
    def __init__( self, stdout=sys.stdout, stderr=sys.stderr, *args, **kwds ):
        unittest.TestCase.__init__( self )
        self.stdout = stdout
        self.stderr = stderr

    def runTest( self ):
        args = "iozone -t 1 -r 128k -s 20m"
        p = subprocess.Popen( args, shell=True, stdout=self.stdout, stderr=self.stderr )
        retcode = p.wait()
        if retcode == 0:
            pass # TODO: parse output
        else:
            self.assertEqual( retcode, 0 )


def createTestSuite( *args, **kwds ):
    if not sys.platform.startswith( "win" ):
        return unittest.TestSuite( [iozoneThroughputTest( *args, **kwds )] )


if __name__ == "__main__":
    if not sys.platform.startswith( "win" ):
        result = unittest.TextTestRunner( verbosity=2 ).run( createTestSuite() )
        if not result.wasSuccessful():
            sys.exit(1)
    else:
        print sys.modules[__name__].__file__.split( os.sep )[-1], "not supported on Windows"

