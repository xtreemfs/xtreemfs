#!/usr/bin/env python
# -*- coding: utf-8  -*-

# Copyright (c) 2009-2011 by Bjoern Kolbeck, Minor Gordon, Zuse Institute Berlin
# Licensed under the BSD License, see LICENSE file for details.

import unittest, subprocess, sys, os


class iozoneDiagnosticTest(unittest.TestCase):
    def __init__( self, direct_io=True, stdout=sys.stdout, stderr=sys.stderr, *args, **kwds ):
        unittest.TestCase.__init__( self )
        self.direct_io = direct_io
        self.stdout = stdout
        self.stderr = stderr

    def runTest( self ):
        if self.direct_io:
            args = "iozone -a -+d"
            p = subprocess.Popen( args, shell=True, stdout=self.stdout, stderr=self.stderr )
            retcode = p.wait()
            self.assertEqual( retcode, 0 )
        else:
            print >>self.stdout, self.__class__.__name__ + ": skipping nondirect volume", os.getcwd()


def createTestSuite( *args, **kwds ):
    if not sys.platform.startswith( "win" ):
        return unittest.TestSuite( [iozoneDiagnosticTest( *args, **kwds )] )


if __name__ == "__main__":
    if not sys.platform.startswith( "win" ):
        result = unittest.TextTestRunner( verbosity=2 ).run( createTestSuite() )
        if not result.wasSuccessful():
            sys.exit(1)
    else:
        print sys.modules[__name__].__file__.split( os.sep )[-1], "not supported on Windows"

