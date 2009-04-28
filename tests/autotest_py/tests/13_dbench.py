import unittest, os.path, sys, subprocess, gzip


# Constants
DBENCH_BIN_FILE_PATH = "dbench"
MY_DIR_PATH = os.path.dirname( os.path.abspath( sys.modules[__name__].__file__ ) )
DBENCH_CLIENT_TXT_GZ_FILE_PATH = os.path.join( MY_DIR_PATH, "dbench-client.txt.gz" )
                               

class dbenchTest(unittest.TestCase):
    def runTest( self ):
        gzip_client_txt_gz_data = gzip.GzipFile( DBENCH_CLIENT_TXT_GZ_FILE_PATH, mode="rb" ).read()
        assert len( gzip_client_txt_gz_data ) > 0
        open( "dbench-client.txt", "wb" ).write( gzip_client_txt_gz_data )
        assert os.stat( "dbench-client.txt" ).st_size > 0
        
        args = "%(DBENCH_BIN_FILE_PATH)s -c dbench-client.txt -D . 5" % globals()
        p = subprocess.Popen( args, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT )
        retcode = p.wait()
        if retcode != 0:
            print "Unexpected return code from marked_block.pl: " + str( retcode )
            print "Output:"
            print p.stdout.read()
            self.assertEqual( retcode, 0 )

                
suite = unittest.TestSuite()
suite.addTest( dbenchTest() )
        

if __name__ == "__main__":
    unittest.TextTestRunner( verbosity=2 ).run( suite )
