#!/usr/bin/env python
# -*- coding: utf-8  -*-

# Copyright (c) 2009-2011 by Bjoern Kolbeck, Minor Gordon, Zuse Institute Berlin
# Licensed under the BSD License, see LICENSE file for details.

import distutils.ccompiler
import os.path
import unittest, subprocess, sys, os

MY_DIR_PATH = os.path.dirname( os.path.abspath( sys.modules[__name__].__file__ ) )

class fsxTest(unittest.TestCase):
    def __init__( self, direct_io=True, stdout=sys.stdout, stderr=sys.stderr, *args, **kwds ):
        unittest.TestCase.__init__( self )
        self.direct_io = direct_io
        self.stdout = stdout
        self.stderr = stderr

    def runTest( self ):
        if os.path.exists(MY_DIR_PATH+"/../utils/fsx.bin") == False:
            compiler = distutils.ccompiler.new_compiler()
            cwd = os.getcwd()
            os.chdir(MY_DIR_PATH+"/../utils")

            compiler.compile(["ltp-fsx.c"])
            compiler.link_executable(["ltp-fsx.o"],"fsx.bin")
            os.chdir(cwd)

        args = MY_DIR_PATH+"/../utils/fsx.bin -R -W -N 100000 ./fsx.tmpfile" #  -s 100"
        p = subprocess.Popen( args, shell=True, stdout=self.stdout, stderr=self.stderr )
        retcode = p.wait()
        self.assertEqual( retcode, 0 )


def createTestSuite( *args, **kwds ):
    if not sys.platform.startswith( "win" ):
        return unittest.TestSuite( [fsxTest( *args, **kwds )] )


if __name__ == "__main__":
    if not sys.platform.startswith( "win" ):
        result = unittest.TextTestRunner( verbosity=2 ).run( createTestSuite() )
        if not result.wasSuccessful():
            sys.exit(1)
    else:
        print sys.modules[__name__].__file__.split( os.sep )[-1], "not supported on Windows"

