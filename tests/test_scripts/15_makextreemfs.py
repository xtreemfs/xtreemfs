#!/usr/bin/env python
# -*- coding: utf-8  -*-

# Copyright (c) 2009-2011 by Bjoern Kolbeck, Minor Gordon, Zuse Institute Berlin
# Licensed under the BSD License, see LICENSE file for details.

import unittest
import sys
import os
import subprocess


global have_called_createTestSuite
have_called_createTestSuite = False


class makextreemfsTest(unittest.TestCase):
    def __init__( self, direct_io=False, stdout=sys.stdout, stderr=sys.stderr, *args, **kwds ):
        unittest.TestCase.__init__( self )
        self.stdout = stdout
        self.stderr = stderr
        self.direct_io = direct_io

    def runTest( self ):

        if self.direct_io:
            print >>self.stdout, self.__class__.__name__ + ": skipping nondirect volume", os.getcwd()
        else:
            retcode = subprocess.call( "wget https://github.com/xtreemfs/xtreemfs/archive/master.zip >/dev/null", shell=True )
            self.assertEqual( retcode, 0 )

            retcode = subprocess.call( "unzip master.zip >/dev/null", shell=True )
            self.assertEqual( retcode, 0 )

            retcode = subprocess.call( "cd xtreemfs-master && make >/dev/null", shell=True )
            self.assertEqual( retcode, 0 )


def createTestSuite( *args, **kwds ):
    if not sys.platform.startswith( "win" ):
        if not have_called_createTestSuite:
            globals()["have_called_createTestSuite"] = True
            return unittest.TestSuite( [makextreemfsTest( *args, **kwds )] )


if __name__ == "__main__":
    if not sys.platform.startswith( "win" ):
        result = unittest.TextTestRunner( verbosity=2 ).run( createTestSuite() )
        if not result.wasSuccessful():
            sys.exit(1)
    else:
        print sys.modules[__name__].__file__.split( os.sep )[-1], "not supported on Windows"

