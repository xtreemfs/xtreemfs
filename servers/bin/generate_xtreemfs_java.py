#!/usr/bin/env python

import sys, os.path

my_dir_path = os.path.dirname( os.path.abspath( sys.modules[__name__].__file__ ) )
try:
    import yidl
except ImportError:        
    yidl_dir_path = os.path.join( my_dir_path, "..", "..", "share", "yidl", "src" )
    if not yidl_dir_path in sys.path: sys.path.append( yidl_dir_path )
    import yidl
    
from yidl.java_target import *
from yidl.generator import *


__all__ = []


# Constants
XTREEMFS_COMMON_IMPORTS = [
                            "import org.xtreemfs.interfaces.utils.*;",
                            "import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;",
                            "import org.xtreemfs.common.buffer.ReusableBuffer;",
#                            "import org.xtreemfs.common.buffer.BufferPool;"
                          ]


class XtreemFSJavaInterface(JavaInterface, JavaClass):
    def __init__( self, *args, **kwds ):
        JavaInterface.__init__( self, *args, **kwds )
        assert self.getUID() > 0, "interface "  + self.getQualifiedName() + " requires a positive UID for the XtreemFS Java generator (current uid = %i)" % self.getUID()
    
    def generate( self ):                            
        JavaInterface.generate( self ) 
           
        class_header = self.getClassHeader()
        uid = self.getUID()            
        out = """\
%(class_header)s        
    public static int getVersion() { return %(uid)s; }
""" % locals()
                            
        request_factories = "".join( [operation.getRequestFactory() for operation in self.getOperations()] )
        if len( request_factories ) > 0:                
            out += """
    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getOperationNumber() )
        {
%(request_factories)s
            default: throw new Exception( "unknown request number " + Integer.toString( header.getOperationNumber() ) );
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
            default: throw new Exception( "unknown response number " + Integer.toString( header.getXID() ) );
        }
    }    
""" % locals()

        exception_factories = [exception_type.getExceptionFactory() for exception_type in self.getExceptionTypes()]
        if len( exception_factories ) > 0:
            exception_factories = ( "\n" + INDENT_SPACES * 2 + "else " ).join( exception_factories  )
            out += """
    public static ONCRPCException createException( String exception_type_name ) throws java.io.IOException
    {
        %(exception_factories)s
        else throw new java.io.IOException( "unknown exception type " + exception_type_name );
    }
""" % locals()

        out += self.getClassFooter()
                
        writeGeneratedFile( self.getFilePath(), out )            

    def getImports( self ): 
        return JavaClass.getImports( self ) + XTREEMFS_COMMON_IMPORTS + ["import org.xtreemfs.interfaces.Exceptions.*;"]

    
class XtreemFSJavaBoolType(JavaBoolType):
    def getBufferDeserializeCall( self, identifier ): return "%(identifier)s = buf.getInt() != 0;" % locals()
    def getBufferSerializeCall( self, identifier ): return "writer.putInt( %(identifier)s ? 1 : 0 );" % locals()
    def getSize( self, identifier ): return "4"


class XtreemFSJavaNumericType(JavaNumericType):
    def getBufferDeserializeCall( self, identifier ):
        boxed_type_name= self.getBoxedTypeName()
        if boxed_type_name == "Integer": boxed_type_name = "Int"
        return "%(identifier)s = buf.get%(boxed_type_name)s();" % locals()

    def getBufferSerializeCall( self, identifier ):
        boxed_type_name= self.getBoxedTypeName()
        if boxed_type_name == "Integer": boxed_type_name = "Int"
        return "writer.put%(boxed_type_name)s( %(identifier)s );" % locals()
        
    def getSize( self, identifier ): return "( " + self.getBoxedTypeName() + ".SIZE / 8 )"
   
                    
class XtreemFSJavaSequenceType(JavaSequenceType):
    def getBufferDeserializeCall( self, identifier ): name = self.getName(); return "%(identifier)s = new %(name)s(); %(identifier)s.deserialize( buf );" % locals()
    def getBufferSerializeCall( self, identifier ): return "%(identifier)s.serialize( writer );" % locals()

    def getDeserializeMethods( self ):
        value_declaration_type = self.getValueType().getDeclarationTypeName()
        value_deserializer = self.getValueType().getBufferDeserializeCall( "new_value" )        
        return JavaSequenceType.getDeserializeMethods( self ) + """
    public void deserialize( ReusableBuffer buf ) {
        int new_size = buf.getInt();
    if (new_size > org.xtreemfs.interfaces.utils.XDRUtils.MAX_ARRAY_ELEMS)
        throw new IllegalArgumentException("array is too large ("+this.size()+")");
        for ( int i = 0; i < new_size; i++ )
        {
            %(value_declaration_type)s new_value; %(value_deserializer)s;
            this.add( new_value );
        }
    } 
""" % locals()

    def getImports( self ): 
        return JavaSequenceType.getImports( self ) + XTREEMFS_COMMON_IMPORTS
    
    def getOtherMethods( self ):
        value_boxed_type_name = self.getValueType().getBoxedTypeName()
        next_value_size = self.getValueType().getSize( "next_value" )        
        return JavaSequenceType.getOtherMethods( self ) + """
    public int calculateSize() {
        int my_size = Integer.SIZE/8;
        for ( Iterator<%(value_boxed_type_name)s> i = iterator(); i.hasNext(); ) {
            %(value_boxed_type_name)s next_value = i.next();
            my_size += %(next_value_size)s;
        }
        return my_size;
    }
""" % locals()        
               
    def getSize( self, identifier ): 
        return "%(identifier)s.calculateSize()" % locals()

    def getSerializeMethods( self ):
        value_boxed_type_name = self.getValueType().getBoxedTypeName()
        value_serializer = self.getValueType().getBufferSerializeCall( "next_value" )        
        return JavaSequenceType.getSerializeMethods( self ) + """
    public void serialize(ONCRPCBufferWriter writer) {
        if (this.size() > org.xtreemfs.interfaces.utils.XDRUtils.MAX_ARRAY_ELEMS)
        throw new IllegalArgumentException("array is too large ("+this.size()+")");
        writer.putInt( size() );
        for ( Iterator<%(value_boxed_type_name)s> i = iterator(); i.hasNext(); )
        {
            %(value_boxed_type_name)s next_value = i.next();        
            %(value_serializer)s;
        }
    }        
""" % locals()    


class XtreemFSJavaSerializableType(JavaSerializableType):
    def getDeclarationTypeName( self ): return "ReusableBuffer"
    def getBufferDeserializeCall( self, identifier ): return "{ %(identifier)s = org.xtreemfs.interfaces.utils.XDRUtils.deserializeSerializableBuffer( buf ); }" % locals()
    def getBufferSerializeCall( self, identifier ): return "{ org.xtreemfs.interfaces.utils.XDRUtils.serializeSerializableBuffer( %(identifier)s, writer ); }" % locals()
    def getSize( self, identifier ): return "org.xtreemfs.interfaces.utils.XDRUtils.serializableBufferLength( %(identifier)s )" % locals()
        

class XtreemFSJavaStringType(JavaStringType):
    def getBufferDeserializeCall( self, identifier ): return "%(identifier)s = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );" % locals()
    def getBufferSerializeCall( self, identifier ): return "org.xtreemfs.interfaces.utils.XDRUtils.serializeString( %(identifier)s, writer );" % locals()
    # def getSize( self, identifier ): return "4 + ( %(identifier)s.length() + 4 - ( %(identifier)s.length() %% 4 ) )" % locals()
    def getSize( self, identifier ): return "org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(%(identifier)s)" % locals() 


class XtreemFSJavaStructType(JavaStructType):
    def getBufferDeserializeCall( self, identifier ): name = self.getName(); return "%(identifier)s = new %(name)s(); %(identifier)s.deserialize( buf );" % locals()    
    def getBufferSerializeCall( self, identifier ): return "%(identifier)s.serialize( writer );" % locals()
    
    def getDeserializeMethods( self ):
        buffer_deserialize_calls = "\n".join( [INDENT_SPACES * 2 + member.getType().getBufferDeserializeCall( member.getIdentifier() ) for member in self.getMembers()] )
        return JavaStructType.getDeserializeMethods( self ) + """
    public void deserialize( ReusableBuffer buf )
    {
%(buffer_deserialize_calls)s
    }
""" % locals()      
     
    def getImports( self ):
        return JavaStructType.getImports( self ) + XTREEMFS_COMMON_IMPORTS    
    
    def getOtherMethods( self ): return JavaStructType.getOtherMethods( self ) + """\
    public int calculateSize()
    {
        int my_size = 0;
%s
        return my_size;
    }
"""  % "\n".join( [INDENT_SPACES * 2 + "my_size += " + member.getType().getSize( member.getIdentifier() ) + ";" for member in self.getMembers()] )            

    def getParentTypeNames( self ):
        if len( JavaStructType.getParentTypeNames( self ) ) == 0:
            return ( None, "org.xtreemfs.interfaces.utils.Serializable" )
        else: 
            return JavaStructType.getParentTypeNames( self )
    
    def getSerializeMethods( self ): 
        buffer_serialize_calls = "\n".join( [INDENT_SPACES * 2 + member.getType().getBufferSerializeCall( member.getIdentifier() ) for member in self.getMembers()] )
        return JavaStructType.getSerializeMethods( self ) + """
    public void serialize( ONCRPCBufferWriter writer ) 
    {
%(buffer_serialize_calls)s
    }
    """ % locals() 
                     
    def getSize( self, identifier ): return "%(identifier)s.calculateSize()" % locals()


class XtreemFSJavaExceptionType(JavaExceptionType):
    def generate( self ): XtreemFSJavaStructType( self.getScope(), self.getQualifiedName(), self.getUID(), ( "org.xtreemfs.interfaces.utils.ONCRPCException", ), self.getMembers() ).generate()
    def getExceptionFactory( self ): return "if ( exception_type_name.equals(\"%s\") ) return new %s();" % ( self.getQualifiedName( "::" ), self.getName() )
    
    
class XtreemFSJavaOperation(JavaOperation):    
    def __init__( self, *args, **kwds ):
        JavaOperation.__init__( self, *args, **kwds )                
        qname = self.getQualifiedName()
        self.__request_type = XtreemFSJavaRequestType( self.getScope(), qname[:-1] + [qname[-1] + "Request"], self.getUID(), ( None, "org.xtreemfs.interfaces.utils.Request" ), [param for param in self.getParameters() if param.isInbound()] )
        if self.isOneway():
            self.__response_type = None
        else:
            params = [param for param in self.getParameters() if param.isOutbound()]
            if self.getReturnType() is not None:  
                params.append( self.getReturnValueAsOperationParameter( "returnValue" ) )
            self.__response_type = XtreemFSJavaResponseType( self.getScope(), qname[:-1] + [qname[-1] + "Response"], self.getUID(), ( None, "org.xtreemfs.interfaces.utils.Response" ), params )
    
    def generate( self ):
        self.__request_type.generate()
        if self.__response_type is not None:
            self.__response_type.generate()
                
    def getRequestFactory( self ): return ( INDENT_SPACES * 3 ) + "case %i: return new %sRequest();\n" % ( self.getUID(), self.getName() )                    
    def getResponseFactory( self ): return not self.isOneway() and ( ( INDENT_SPACES * 3 ) + "case %i: return new %sResponse();" % ( self.getUID(), self.getName() ) ) or ""                


class XtreemFSJavaRequestType(XtreemFSJavaStructType):
    def getOtherMethods( self ):
        uid = self.getUID()     
        response_type_name = self.getName()[:self.getName().index( "Request" )] + "Response"   
        return XtreemFSJavaStructType.getOtherMethods( self ) + """
    // Request
    public int getOperationNumber() { return %(uid)s; }
    public Response createDefaultResponse() { return new %(response_type_name)s(); }
""" % locals()


class XtreemFSJavaResponseType(XtreemFSJavaStructType):    
    def getOtherMethods( self ):
        uid = self.getUID()
        return XtreemFSJavaStructType.getOtherMethods( self ) + """
    // Response
    public int getOperationNumber() { return %(uid)s; }
""" % locals()


class XtreemFSJavaTarget(JavaTarget): pass
                                
           
if __name__ == "__main__":     
    if len( sys.argv ) == 1:
        sys.argv.extend( ( "-i", os.path.abspath( os.path.join( my_dir_path, "..", "..", "interfaces" ) ), 
                           "-o", os.path.abspath( os.path.join( my_dir_path, "..", "src" ) ) ) )
        
    generator_main( XtreemFSJavaTarget )
