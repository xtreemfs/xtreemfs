#!/usr/bin/env python
# -*- coding: utf-8  -*-

# Copyright (c) 2009-2011 by Bjoern Kolbeck, Minor Gordon, Zuse Institute Berlin
# Licensed under the BSD License, see LICENSE file for details.

import os, shutil, stat, unittest, sys


TEST_FILE_NAME = "simple_metadata_file.txt"
TEST_DIR_NAME = "simple_metadata_dir"
TEST_LINK_NAME = "simple_metadata_link"
TEST_SUBDIR_NAME = "simple_metadata_subdir"
TEST_SUBDIR_PATH = os.path.join( TEST_DIR_NAME, TEST_SUBDIR_NAME )
TEST_SUBDIRNONASCII_NAME = "subdir_ÄöíŁ€"
TEST_SUBDIRNONASCII_PATH = os.path.join( TEST_DIR_NAME, TEST_SUBDIRNONASCII_NAME )


class SimpleMetadataTestCase(unittest.TestCase):
    def tearDown( self ):
        try: os.unlink( TEST_FILE_NAME )
        except: pass
        try: os.unlink( TEST_LINK_NAME )
        except: pass
        try: shutil.rmtree( TEST_DIR_NAME )
        except: pass


class chmodTest(SimpleMetadataTestCase):
    def runTest( self ):
        open( TEST_FILE_NAME, "w+" ).close()
        os.chmod( TEST_FILE_NAME, stat.S_IWRITE | stat.S_IREAD )

class chownTest(SimpleMetadataTestCase):
    def runTest( self ):
        open( TEST_FILE_NAME, "w+" ).close()
        os.chown( TEST_FILE_NAME, 1001, 100 )
        open( TEST_FILE_NAME, "r" ).close()


class creatTest(SimpleMetadataTestCase):
    def runTest( self ):
        open( TEST_FILE_NAME, "w+" ).close()
        assert os.path.exists( TEST_FILE_NAME )


class linkTest(SimpleMetadataTestCase):
    def runTest(self ):
        open( TEST_FILE_NAME, "w+" ).close()
        assert os.path.exists( TEST_FILE_NAME )
        os.link( TEST_FILE_NAME, TEST_LINK_NAME )
        assert os.path.exists( TEST_LINK_NAME )
        os.unlink( TEST_LINK_NAME )
        assert not os.path.exists( TEST_LINK_NAME )
        assert os.path.exists( TEST_FILE_NAME )


class mkdirTest(SimpleMetadataTestCase):
    def runTest( self ):
        os.mkdir( TEST_DIR_NAME )
        assert os.path.exists( TEST_DIR_NAME )
        os.mkdir( TEST_SUBDIR_PATH )
        assert os.path.exists( TEST_SUBDIR_PATH )

class mkdirNonASCIITest(SimpleMetadataTestCase):
    def runTest( self ):
        os.mkdir( TEST_DIR_NAME )
        assert os.path.exists( TEST_DIR_NAME )
        os.mkdir( TEST_SUBDIRNONASCII_PATH )
        assert os.path.exists( TEST_SUBDIRNONASCII_PATH )


class readdirTest(SimpleMetadataTestCase):
    def runTest( self ):
        os.mkdir( TEST_DIR_NAME )
        os.mkdir( TEST_SUBDIR_PATH )
        assert len( os.listdir( TEST_DIR_NAME ) ) >= 1


class renamedirTest(SimpleMetadataTestCase):
    def runTest( self ):
        os.mkdir( TEST_DIR_NAME )
        assert os.path.exists( TEST_DIR_NAME )
        os.rename( TEST_DIR_NAME, "renameddir" )
        assert not os.path.exists( TEST_DIR_NAME )
        assert os.path.exists( "renameddir" )
        os.rename( "renameddir", TEST_DIR_NAME )
        assert os.path.exists( TEST_DIR_NAME )
        assert not os.path.exists( "renameddir" )


class renamefileTest(SimpleMetadataTestCase):
    def runTest( self ):
        open( TEST_FILE_NAME, "w+" ).close()
        assert os.path.exists( TEST_FILE_NAME )
        os.rename( TEST_FILE_NAME, "renamefile" )
        assert not os.path.exists( TEST_FILE_NAME )
        assert os.path.exists( "renamefile" )
        os.unlink( "renamefile" )
        assert not os.path.exists( "renamefile" )
        open( TEST_FILE_NAME, "w+" ).close()
        assert os.path.exists( TEST_FILE_NAME )


class rmdirTest(SimpleMetadataTestCase):
    def runTest( self ):
        os.mkdir( TEST_DIR_NAME )
        os.mkdir( os.path.join( TEST_DIR_NAME, TEST_SUBDIR_NAME ) )
        os.rmdir( os.path.join( TEST_DIR_NAME, TEST_SUBDIR_NAME ) )
        os.rmdir( TEST_DIR_NAME )


class symlinkTest(SimpleMetadataTestCase):
    def runTest( self ):
        open( TEST_FILE_NAME, "w+" ).close()
        assert os.path.exists( TEST_FILE_NAME )
        os.symlink( TEST_FILE_NAME, TEST_LINK_NAME )
        assert os.path.exists( TEST_LINK_NAME )
        assert os.readlink( TEST_LINK_NAME ) == TEST_FILE_NAME
        os.rename( TEST_LINK_NAME, "renamedlink" )
        assert os.readlink( "renamedlink" ) == TEST_FILE_NAME
        os.unlink( "renamedlink" )
        assert os.path.exists( TEST_FILE_NAME )


class unlinkTest(SimpleMetadataTestCase):
    def runTest( self ):
        open( TEST_FILE_NAME, "w+" ).close()
        assert os.path.exists( TEST_FILE_NAME )
        os.unlink( TEST_FILE_NAME )
        assert not os.path.exists( TEST_FILE_NAME )


def createTestSuite( *args, **kwds ):
    test_suite = unittest.TestSuite()
    test_suite.addTest( chmodTest() )
    test_suite.addTest( creatTest() )
    if hasattr( os, "link" ): test_suite.addTest( linkTest() )
    test_suite.addTest( mkdirTest() )
    test_suite.addTest( mkdirNonASCIITest() )
    test_suite.addTest( readdirTest() )
    test_suite.addTest( renamedirTest() )
    test_suite.addTest( renamefileTest() )
    test_suite.addTest( rmdirTest() )
    if hasattr( os, "symlink" ): test_suite.addTest( symlinkTest() )
    test_suite.addTest( unlinkTest() )
    return test_suite


if __name__ == "__main__":
    result = unittest.TextTestRunner( verbosity=2 ).run( createTestSuite() )
    if not result.wasSuccessful():
        sys.exit(1)

