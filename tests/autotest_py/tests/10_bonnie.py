import unittest, subprocess


BONNIE_BIN_FILE_PATH = "bonnie"


class bonnieTest(unittest.TestCase):
	def runTest( self ):
		args = BONNIE_BIN_FILE_PATH + " -d ." #  -s 100"
		p = subprocess.Popen( args, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT )
		retcode = p.wait()
		if retcode != 0:
			print "Unexpected return code from Bonnie:", retcode
			print "Output:"
			print p.stdout.read()
			self.fail()


suite = unittest.TestSuite()
suite.addTest( bonnieTest() )
        

if __name__ == "__main__":
    unittest.TextTestRunner( verbosity=2 ).run( suite )
    
