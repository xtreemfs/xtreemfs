import unittest, shutil, sys, os, subprocess


global have_called_createTestSuite
have_called_createTestSuite = False


class makextreemfsTest(unittest.TestCase):
    def __init__( self, stdout=sys.stdout, stderr=sys.stderr ):
        unittest.TestCase.__init__( self )
        self.stdout = stdout
        self.stderr = stderr
        
    def runTest( self ):
        try: shutil.rmtree( "xtreemfs" )
        except: pass
        retcode = subprocess.call( "svn co http://xtreemfs.googlecode.com/svn/trunk xtreemfs", shell=True ) #, stdout=PIPE, stderr=STDOUT )         #
        self.assertEqual( retcode, 0 )

        retcode = subprocess.call( "make -C xtreemfs", shell=True ) #, stdout=PIPE, stderr=STDOUT )         
        self.assertEqual( retcode, 0 )


def createTestSuite( *args, **kwds ):     
    if not sys.platform.startswith( "win" ):
        if not have_called_createTestSuite:
            globals()["have_called_createTestSuite"] = True
            return unittest.TestSuite( [makextreemfsTest( *args, **kwds )] )
               

if __name__ == "__main__":
    if not sys.platform.startswith( "win" ):
        unittest.TextTestRunner( verbosity=2 ).run( createTestSuite() )
    else:
        print sys.modules[__name__].__file__.split( os.sep )[-1], "not supported on Windows"
    
