import unittest, subprocess, sys


class iozoneDiagnosticTest(unittest.TestCase):
	def runTest( self ):
		args = "iozone -a -+d"
                if __name__ == "__main__":
                    stdout = sys.stdout
                    stderr = sys.stderr
                else:
                    stdout = subprocess.PIPE
                    stderr = subprocess.STDOUT
		p = subprocess.Popen( args, shell=True, stdout=stdout, stderr=stderr )
		retcode = p.wait()
		if retcode != 0:
			print "Unexpected return code from iozone:", retcode
			print "Output:"
			print p.stdout.read()
			self.fail()


class iozoneThroughputTest(unittest.TestCase):
	def runTest( self ):
		args = "iozone -t 1 -r 128k -s 20m"
                if __name__ == "__main__":
                    stdout = sys.stdout
                    stderr = sys.stderr
                else:
                    stdout = subprocess.PIPE
                    stderr = subprocess.STDOUT
		p = subprocess.Popen( args, shell=True )#, stdout=stdout, stderr=stderr )
		retcode = p.wait()
		if retcode == 0:
			pass # TODO: parse output 
		else:
			print "Unexpected return code from iozone:", retcode
			print "Output:"
			print p.stdout.read()
			self.fail()
			
			
suite = unittest.TestSuite()
suite.addTest( iozoneDiagnosticTest() )
suite.addTest( iozoneThroughputTest() )
        

if __name__ == "__main__":
    unittest.TextTestRunner( verbosity=2 ).run( suite )
    
