import unittest, urllib2, os.path
from subprocess import call, PIPE, STDOUT


TAR_GZ_URL = "ftp://ftp.hpl.hp.com/pub/httperf/httperf-0.9.0.tar.gz"


class findgreptarTest(unittest.TestCase):
	def runTest( self ):
		tar_gz_file_name = TAR_GZ_URL.split( '/' )[-1]		
		if not os.path.exists( tar_gz_file_name ):
			tar_gz_data = urllib2.urlopen( TAR_GZ_URL ).read()
			open( tar_gz_file_name, "wb" ).write( tar_gz_data )
		
		retcode = call( "tar zxf " + tar_gz_file_name, shell=True, stdout=PIPE, stderr=STDOUT )		 
		self.assertEqual( retcode, 0 )

		retcode = call( "find . -name '*.cpp'", shell=True, stdout=PIPE, stderr=STDOUT )		 
		self.assertEqual( retcode, 0 )
		
		retcode = call( "grep -R 'ttest' .", shell=True, stdout=PIPE, stderr=STDOUT )		 		
		self.assertEqual( retcode, 0 )


suite = unittest.TestSuite()
suite.addTest( findgreptarTest() )
        

if __name__ == "__main__":
    unittest.TextTestRunner( verbosity=2 ).run( suite )
    
