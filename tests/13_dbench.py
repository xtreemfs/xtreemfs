import unittest, os.path, sys, subprocess, gzip


# Constants
MY_DIR_PATH = os.path.dirname( os.path.abspath( sys.modules[__name__].__file__ ) )
DBENCH_CLIENT_TXT_GZ_FILE_PATH = os.path.join( MY_DIR_PATH, "dbench-client.txt.gz" )
                               

class dbenchTest(unittest.TestCase):
    def __init__( self, direct_io=True, stdout=sys.stdout, stderr=sys.stderr, *args, **kwds ):
        unittest.TestCase.__init__( self )
        self.direct_io = direct_io
        self.stdout = stdout
        self.stderr = stderr

    def runTest( self ):
        if self.direct_io:
           gzip_client_txt_gz_data = gzip.GzipFile( DBENCH_CLIENT_TXT_GZ_FILE_PATH, mode="rb" ).read()
           assert len( gzip_client_txt_gz_data ) > 0
           open( "dbench-client.txt", "wb" ).write( gzip_client_txt_gz_data )
           assert os.stat( "dbench-client.txt" ).st_size > 0
        
           args = "dbench -c dbench-client.txt -D . 5"
           stdout = open( "dbench-stdout.txt", "a+" )
           p = subprocess.Popen( args, shell=True, stdout=stdout, stderr=subprocess.STDOUT )
           retcode = p.wait()
           self.assertEqual( retcode, 0 )
        else:
            print >>self.stdout, self.__class__.__name__ + ": skipping nondirect volume", os.getcwd()


def createTestSuite( *args, **kwds ): 
    if not sys.platform.startswith( "win" ):
        return unittest.TestSuite( [dbenchTest( *args, **kwds )] )
                

if __name__ == "__main__":
    if not sys.platform.startswith( "win" ):
        unittest.TextTestRunner( verbosity=2 ).run( createTestSuite() )
    else:
        print sys.modules[__name__].__file__.split( os.sep )[-1], "not supported on Windows"
