import unittest, shutil
from subprocess import call, PIPE, STDOUT


class makextreemfsTest(unittest.TestCase):
	def runTest( self ):
		try: shutil.rmtree( "xtreemfs" )
		except: pass
		retcode = call( "svn co http://xtreemfs.googlecode.com/svn/trunk xtreemfs", shell=True ) #, stdout=PIPE, stderr=STDOUT )		 #
		self.assertEqual( retcode, 0 )

		retcode = call( "make -C xtreemfs", shell=True ) #, stdout=PIPE, stderr=STDOUT )		 
		self.assertEqual( retcode, 0 )


suite = unittest.TestSuite()
suite.addTest( makextreemfsTest() )
        

if __name__ == "__main__":
    unittest.TextTestRunner( verbosity=2 ).run( suite )
    
