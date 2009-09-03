#!/usr/bin/env python

from yidl.generator import *
from yidl.string_utils import *
from yidl.targets.java_target import *


__all__ = []


# Constants
XTREEMFS_IMPORTS = [
                    "import org.xtreemfs.interfaces.utils.*;",
                    "import org.xtreemfs.common.buffer.ReusableBuffer;",
                   ]


class XtreemFSJavaBufferType(JavaBufferType):
    def getDeclarationTypeName( self ): return "ReusableBuffer"
    

class XtreemFSJavaExceptionType(JavaExceptionType):
    def generate( self ): XtreemFSJavaStructType( self.getScope(), self.getQualifiedName(), self.getTag(), ( "org.xtreemfs.interfaces.utils.ONCRPCException", ), self.getMembers() ).generate()
    def getExceptionFactory( self ): return ( INDENT_SPACES * 3 ) + "case %i: return new %s();\n" % ( self.getTag(), self.getName() )


class XtreemFSJavaInterface(JavaInterface, JavaClass):    
    def generate( self ):                            
        class_header = self.getClassHeader()        
        constants = pad( "\n" + INDENT_SPACES, ( "\n" + INDENT_SPACES ).join( [repr( constant ) for constant in self.getConstants()] ), "\n\n" )        
        tag = self.getTag()            
        out = """\
%(class_header)s%(constants)s
    public static int getVersion() { return %(tag)s; }
""" % locals()

        exception_factories = "".join( [exception_type.getExceptionFactory() for exception_type in self.getExceptionTypes()] )
        if len( exception_factories ) > 0:                
            out += """
    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
%(exception_factories)s
            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }
""" % locals()
        
        request_factories = "".join( [operation.getRequestFactory() for operation in self.getOperations()] )
        if len( request_factories ) > 0:                
            out += """
    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
%(request_factories)s
            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }
""" % locals()

        response_factories = "".join( [operation.getResponseFactory() for operation in self.getOperations()] )
        if len( response_factories ) > 0:    
                out += """            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
%(response_factories)s
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }    
""" % locals()

        out += self.getClassFooter()
                
        writeGeneratedFile( self.getFilePath(), out )            

        for operation in self.getOperations():
            operation.generate()
            
        for exception_type in self.getExceptionTypes():
            exception_type.generate()
            
    def getImports( self ): 
        return JavaClass.getImports( self ) + XTREEMFS_IMPORTS

    def getPackageDirPath( self ):                
        return os.sep.join( self.getQualifiedName() )
    
    def getPackageName( self ): 
        return ".".join( self.getQualifiedName() )



class XtreemFSJavaMapType(JavaMapType):
    def getImports( self ): 
        return JavaMapType.getImports( self ) + XTREEMFS_IMPORTS

                    
class XtreemFSJavaSequenceType(JavaSequenceType):
    def getImports( self ): 
        return JavaSequenceType.getImports( self ) + XTREEMFS_IMPORTS
    

class XtreemFSJavaStructType(JavaStructType):        
    def getImports( self ):
        return JavaStructType.getImports( self ) + XTREEMFS_IMPORTS    
                                
    
class XtreemFSJavaOperation(JavaOperation):        
    def generate( self ):
        self._getRequestType().generate()
        self._getResponseType( "returnValue" ).generate()
                
    def getRequestFactory( self ): return ( INDENT_SPACES * 3 ) + "case %i: return new %sRequest();\n" % ( self.getTag(), self.getName() )                    
    def getResponseFactory( self ): return not self.isOneway() and ( ( INDENT_SPACES * 3 ) + "case %i: return new %sResponse();" % ( self.getTag(), self.getName() ) ) or ""                

class XtreemFSJavaRequestType(XtreemFSJavaStructType):
    def getOtherMethods( self ):        
        response_type_name = self.getName()[:self.getName().index( "Request" )] + "Response"   
        return XtreemFSJavaStructType.getOtherMethods( self ) + """
    // Request
    public Response createDefaultResponse() { return new %(response_type_name)s(); }
""" % locals()

    def getParentTypeNames( self ):
        return ( "org.xtreemfs.interfaces.utils.Request", )            

class XtreemFSJavaResponseType(XtreemFSJavaStructType):
    def getParentTypeNames( self ):
        return ( "org.xtreemfs.interfaces.utils.Response", )            


class XtreemFSJavaTarget(JavaTarget): pass
                                
           
if __name__ == "__main__":
    import sys, os.path
         
    if len( sys.argv ) == 1:    
        my_dir_path = os.path.dirname( os.path.abspath( sys.modules[__name__].__file__ ) )
        os.chdir( os.path.join( my_dir_path, "..", "src", "servers" ) )
        sys.argv.extend( ( "-i", os.path.abspath( os.path.join( my_dir_path, "..", "src", "interfaces" ) ), 
                           "-o", os.path.abspath( os.path.join( my_dir_path, "..", "src", "servers", "src" ) ) ) )
        
    generator_main( XtreemFSJavaTarget )
