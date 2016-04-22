#!/usr/bin/env python
# -*- coding: utf-8  -*-

# Copyright (c) 2009-2011 by Bjoern Kolbeck, Minor Gordon, Zuse Institute Berlin
# Licensed under the BSD License, see LICENSE file for details.

import unittest, os.path, sys, subprocess


# Constants
MY_DIR_PATH = os.path.dirname( os.path.abspath( sys.modules[__name__].__file__ ) )
MARKED_BLOCK_PL_FILE_PATH = os.path.join( MY_DIR_PATH, "marked_block.pl" )


class ErichsDataIntegrityTest(unittest.TestCase):
    def __init__( self, stdout=sys.stdout, stderr=sys.stderr, *args, **kwds ):
        unittest.TestCase.__init__( self )
        self.stdout = stdout
        self.stderr = stderr

    def runTest( self ):
        p = subprocess.Popen( MARKED_BLOCK_PL_FILE_PATH + " --start=1 --nfiles=20 --size=1 --group=10 --base=.", shell=True, stdout=self.stdout, stderr=self.stderr )
        retcode = p.wait()
        self.assertEqual( retcode, 0 )


def createTestSuite( *args, **kwds ):
    if not sys.platform.startswith( "win" ):
        return unittest.TestSuite( [ErichsDataIntegrityTest( *args, **kwds )] )


if __name__ == "__main__":
    if not sys.platform.startswith( "win" ):
        result = unittest.TextTestRunner( verbosity=2 ).run( createTestSuite() )
        if not result.wasSuccessful():
            sys.exit(1)
    else:
        print sys.modules[__name__].__file__.split( os.sep )[-1], "not supported on Windows"

