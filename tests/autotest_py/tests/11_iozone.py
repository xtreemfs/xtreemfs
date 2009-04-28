import unittest, subprocess


class iozoneDiagnosticTest(unittest.TestCase):
	def runTest( self ):
		args = "iozone -a -+d"
		p = subprocess.Popen( args, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT )
		retcode = p.wait()
		if retcode != 0:
			print "Unexpected return code from iozone:", retcode
			print "Output:"
			print p.stdout.read()
			self.fail()


class iozoneThroughputTest(unittest.TestCase):
	def runTest( self ):
		args = "iozone -t 1 -r 128k -s 20m"
		p = subprocess.Popen( args, shell=True )#, stdout=subprocess.PIPE, stderr=subprocess.STDOUT )
		retcode = p.wait()
		if retcode == 0:
			pass # TODO: parse output 
		else:
			print "Unexpected return code from iozone:", retcode
			print "Output:"
			print p.stdout.read()
			self.fail()
			
			
suite = unittest.TestSuite()
#suite.addTest( iozoneDiagnosticTest() )
suite.addTest( iozoneThroughputTest() )
        

if __name__ == "__main__":
    unittest.TextTestRunner( verbosity=2 ).run( suite )
    
