from yidl.fuzzer_cpp_target import *


class XtreemFSFuzzerCPPInterface(FuzzerCPPInterface):
    def getOperations( self ):
        return [operation for operation in FuzzerCPPInterface.getOperations( self ) if not operation.getName().startswith( "admin" )]    


class XtreemFSFuzzerCPPStringType(FuzzerCPPStringType):
    def getTestValues( self, identifier ):         
        if identifier == "volume_name" or identifier == "path":
            test_values = []
            test_values.append( "std::string %(identifier)s;" % locals() )            
            test_values.append( 'std::string %(identifier)s( "testvol" );' % locals() )
            test_values.append( 'std::string %(identifier)s( "testvol/" );' % locals() )
            test_values.append( 'std::string %(identifier)s( "testvol//" );' % locals() )
            test_values.append( 'std::string %(identifier)s( "testvol\\\"\" );' % locals() )
            return test_values
        else:
            return FuzzerCPPStringType.getTestValues( self, identifier )


class XtreemFSFuzzerCPPStructType(FuzzerCPPStructType):
    def getTestValues( self, identifier ):
        if identifier == "user_credentials":
            declaration_type_name = self.getDeclarationTypeName()
            return ['%(declaration_type_name)s %(identifier)s( "test", StringSet( "test" ) );' % locals()]
        else:            
            return FuzzerCPPStructType.getTestValues( self, identifier )


class XtreemFSFuzzerCPPTarget(FuzzerCPPTarget): pass




if __name__ == "__main__":
    from yidl.generator import generator_main
    generator_main( XtreemFSFuzzerCPPTarget, "_fuzzer.h" )
