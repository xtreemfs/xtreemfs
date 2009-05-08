import unittest, urllib2, os.path, sys, subprocess


TAR_GZ_URL = "ftp://ftp.hpl.hp.com/pub/httperf/httperf-0.9.0.tar.gz"


class findgreptarTest(unittest.TestCase):
    def __init__( self, stdout=sys.stdout, stderr=sys.stderr ):
        unittest.TestCase.__init__( self )
        self.stdout = stdout
        self.stderr = stderr
    	
	def runTest( self ):
		tar_gz_file_name = TAR_GZ_URL.split( '/' )[-1]		
		if not os.path.exists( tar_gz_file_name ):
			tar_gz_data = urllib2.urlopen( TAR_GZ_URL ).read()
			open( tar_gz_file_name, "wb" ).write( tar_gz_data )
		
		retcode = subprocess.call( "tar zxf " + tar_gz_file_name, shell=True, stdout=self.stdout, stderr=self.stderr )		 
		self.assertEqual( retcode, 0 )

		retcode = subprocess.call( "find . -name '*.cpp'", shell=True, stdout=self.stdout, stderr=self.stderr )		 
		self.assertEqual( retcode, 0 )
		
		retcode = subprocess.call( "grep -R 'ttest' .", shell=True, stdout=self.stdout, stderr=self.stderr )		 		
		self.assertEqual( retcode, 0 )


def createTestSuite( *args, **kwds ): 
    if not sys.platform.startswith( "win" ):
    	return unittest.TestSuite( [findgreptarTest( *args, **kwds )] )
        

if __name__ == "__main__":
    if not sys.platform.startswith( "win" ):
        unittest.TextTestRunner( verbosity=2 ).run( createTestSuite() )
    else:
        print sys.modules[__name__].__file__.split( os.sep )[-1], "not supported on Windows"
    
