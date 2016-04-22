#!/usr/bin/env python
# -*- coding: utf-8  -*-

# Copyright (c) 2009-2011 by Bjoern Kolbeck, Minor Gordon, Zuse Institute Berlin
# Licensed under the BSD License, see LICENSE file for details.

import unittest, subprocess, time, os, sys
from glob import glob


class ErichsddwriteTest(unittest.TestCase):
    def __init__( self, stdout=sys.stdout, stderr=sys.stderr, *args, **kwds ):
        unittest.TestCase.__init__( self )
        self.stdout = stdout
        self.stderr = stderr

    def setUp( self ):
        self.client_processes = []

    def tearDown( self ):
        for client_process in self.client_processes:
            client_process.terminate()
            client_process.wait()

        for file_name in glob( self.__class__.__name__ + "*" ):
            os.unlink( file_name )

    def runTest( self ):
        class_name = self.__class__.__name__
        for clients_count in xrange( 2, 4, 2 ):
            for client_i in xrange( clients_count ):
                args = "dd if=/dev/zero of=%(class_name)s_%(client_i)u bs=1MB count=10" % locals()
                client_process = subprocess.Popen( args, shell=True, stdout=self.stdout, stderr=self.stderr )
                self.client_processes.append( client_process )

            while len( self.client_processes ) > 0:
                client_i = 0
                while client_i < len( self.client_processes ):
                    retcode = self.client_processes[client_i].poll()
                    if retcode is not None:
                        if retcode != 0:
                            self.fail()
                        else:
                            del self.client_processes[client_i]
                    else:
                        client_i ++ 1

                time.sleep( 0.5 )


def createTestSuite( *args, **kwds ):
    if not sys.platform.startswith( "win" ):
        return unittest.TestSuite( [ErichsddwriteTest( *args, **kwds )] )


if __name__ == "__main__":
    if not sys.platform.startswith( "win" ):
        result = unittest.TextTestRunner( verbosity=2 ).run( createTestSuite() )
        if not result.wasSuccessful():
            sys.exit(1)
    else:
        print sys.modules[__name__].__file__.split( os.sep )[-1], "not supported on Windows"

