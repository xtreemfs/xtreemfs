import unittest, subprocess, sys, os


class iozoneThroughputTest(unittest.TestCase):
    def __init__( self, stdout=sys.stdout, stderr=sys.stderr ):
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
        unittest.TextTestRunner( verbosity=2 ).run( createTestSuite() )
    else:
        print sys.modules[__name__].__file__.split( os.sep )[-1], "not supported on Windows"
    
