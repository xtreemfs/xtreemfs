#!/usr/bin/env python

import sys, os.path

my_dir_path = os.path.dirname( os.path.abspath( sys.modules[__name__].__file__ ) )
try:
    import yidl
except ImportError:        
    yidl_dir_path = os.path.join( my_dir_path, "..", "share" )
    if not yidl_dir_path in sys.path: sys.path.append( yidl_dir_path )
    import yidl
    
from yidl.java_types import *
from yidl.generator import *


__all__ = []


class XtreemFSJavaTypeFactory(JavaTypeFactory):
    def createInterfaceType( self, *args, **kwds ): return XtreemFSJavaInterfaceType( self.base_package_name, *args, **kwds )
    

class XtreemFSJavaTypeCommon:
    def getImports( self ):        
        return "\n".join( JavaCompoundTypeCommon._getImports( self, exclude_package_names=( "org.xtreemfs", ) ) + 
                          [
                            "",
                            "import org.xtreemfs.interfaces.utils.*;",
                            "import org.xtreemfs.interfaces.Exceptions.*;",                            
                            "import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;",                            
                            "import org.xtreemfs.common.buffer.ReusableBuffer;",
                            "import org.xtreemfs.common.buffer.BufferPool;",
                            "import java.util.List;",
                            "import java.util.Iterator;",
                            "import java.util.ArrayList;",
                            ] )            

    
class XtreemFSJavaBoolType(JavaBoolType, XtreemFSJavaTypeCommon):
    def getDeserializer( self, identifier ): return "%(identifier)s = buf.getInt() != 0;" % locals()
    def getSerializer( self, identifier ): return "writer.putInt( %(identifier)s ? 1 : 0 );" % locals()
    def getSize( self, identifier ): return "4"


class XtreemFSJavaNumericType(JavaNumericType, XtreemFSJavaTypeCommon):
    def getDeserializer( self, identifier ):
        boxed_type= self.getBoxedType()
        if boxed_type == "Integer": boxed_type = "Int"
        return "%(identifier)s = buf.get%(boxed_type)s();" % locals()

    def getSerializer( self, identifier ):
        boxed_type= self.getBoxedType()
        if boxed_type == "Integer": boxed_type = "Int"
        return "writer.put%(boxed_type)s( %(identifier)s );" % locals()
        
    def getSize( self, identifier ): return "( " + self.getBoxedType() + ".SIZE / 8 )"


class XtreemFSJavaInterfaceType(JavaInterfaceType, XtreemFSJavaTypeCommon):
    def generate( self ):        
        assert self.uid > 0, "interface "  + self.qname + " requires a positive UID for the XtreemFS Java generator (current uid = %i)" % self.uid    
        
        JavaInterfaceType.generate( self )

        package_name = self.getPackageName()
        imports = self.getImports()
        name = self.name    
        uid = self.uid        
    
        out = """\
package %(package_name)s;

%(imports)s

public class %(name)s
{
    public static int getVersion() { return %(uid)s; }
""" % locals()
                            
        request_factories = "".join( [operation_type.getRequestFactory() for operation_type in self.child_types if isinstance( operation_type, XtreemFSJavaOperationType )] )
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

        response_factories = "".join( [operation_type.getResponseFactory() for operation_type in self.child_types if isinstance( operation_type, XtreemFSJavaOperationType )] )
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

        
        exception_factories = [operation_type.getExceptionFactory() for exception_type in self.child_types if isinstance( exception_type, XtreemFSJavaExceptionType )]
        if len( exception_factories ) > 0:
            exception_factories = ( "\n" + INDENT_SPACES * 2 + "else " ).join( exception_factories  )
            out += """
    public static ONCRPCException createException( String exception_type_name ) throws java.io.IOException
    {
        %(exception_factories)s
        else throw new java.io.IOException( "unknown exception type " + exception_type_name );
    }
""" % locals()

        out += """\
}
"""
                
        writeGeneratedFile( self.getFilePath(), out ) 
    
                    
class XtreemFSJavaSequenceType(JavaSequenceType, XtreemFSJavaTypeCommon):
    def generate( self ):
        package_name = self.getPackageName()
        imports = self.getImports()
        type_name = self.name
        type_qname = self.qname
        value_declaration_type = self.value_type.getDeclarationType()
        value_boxed_type = self.value_type.getBoxedType()
        value_serializer = self.value_type.getSerializer( "next_value" )
        value_deserializer = self.value_type.getDeserializer( "new_value" )
        next_value_size = self.value_type.getSize( "next_value" )
        writeGeneratedFile( self.getFilePath(), """\
package %(package_name)s;        

%(imports)s
        
        
public class %(type_name)s extends ArrayList<%(value_boxed_type)s>
{    
    // Serializable
    public String getTypeName() { return "%(type_qname)s"; }
    
    public void serialize(ONCRPCBufferWriter writer) {
        if (this.size() > org.xtreemfs.interfaces.utils.XDRUtils.MAX_ARRAY_ELEMS)
        throw new IllegalArgumentException("array is too large ("+this.size()+")");
        writer.putInt( size() );
        for ( Iterator<%(value_boxed_type)s> i = iterator(); i.hasNext(); )
        {
            %(value_boxed_type)s next_value = i.next();        
            %(value_serializer)s;
        }
    }

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
    
    public int calculateSize() {
        int my_size = Integer.SIZE/8;
        for ( Iterator<%(value_boxed_type)s> i = iterator(); i.hasNext(); ) {
            %(value_boxed_type)s next_value = i.next();
            my_size += %(next_value_size)s;
        }
        return my_size;
    }
}
""" % locals() )    
    
    def getDeserializer( self, identifier ): type_name = self.name; return "%(identifier)s = new %(type_name)s(); %(identifier)s.deserialize( buf );" % locals()
    def getSerializer( self, identifier ): return "%(identifier)s.serialize( writer );" % locals()
    def getSize( self, identifier ): return "%(identifier)s.calculateSize()" % locals()
    

class XtreemFSJavaSerializableType(JavaSerializableType, XtreemFSJavaTypeCommon):
    def getDeclarationType( self ): return "ReusableBuffer"
    def getDeserializer( self, identifier ): return "{ %(identifier)s = org.xtreemfs.interfaces.utils.XDRUtils.deserializeSerializableBuffer(buf); }" % locals()
    def getSerializer( self, identifier ): return "{ org.xtreemfs.interfaces.utils.XDRUtils.serializeSerializableBuffer(%(identifier)s,writer); }" % locals()
    def getSize( self, identifier ): return "org.xtreemfs.interfaces.utils.XDRUtils.serializableBufferLength(%(identifier)s)" % locals()
        

class XtreemFSJavaStringType(JavaStringType, XtreemFSJavaTypeCommon):
    def getDeserializer( self, identifier ): return "{ %(identifier)s = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }" % locals()
    def getSerializer( self, identifier ): return "{ org.xtreemfs.interfaces.utils.XDRUtils.serializeString(%(identifier)s,writer); }" % locals()
    def getSize( self, identifier ): return "4 + ( %(identifier)s.length() + 4 - ( %(identifier)s.length() %% 4 ) )" % locals()


class XtreemFSJavaStructTypeCommon(XtreemFSJavaTypeCommon):
    def getStructBody( self, type_name_suffix="", members=None ):        
        if members is None: members = self.members
        type_name = self.name
        type_qname = self.qname
        constructors = self.getConstructors( type_name_suffix, members )
        member_accessors = self.getMemberAccessors( members )
        member_tostrings = self.getMemberToStrings( members )
        if len( member_tostrings ) > 0:
            member_tostrings = INDENT_SPACES * 2 + "return \"%(type_name)s%(type_name_suffix)s( \" + " % locals() + member_tostrings + " + \" )\";"
        else:
            member_tostrings = INDENT_SPACES * 2 + "return \"%(type_name)s%(type_name_suffix)s()\";" % locals()
        
        member_declarations = self.getMemberDeclarations( members )
        member_serializers = self.getMemberSerializers( members )        
        member_deserializers = self.getMemberDeserializers( members )        
        member_sizes = self.getMemberSizes( members )
        
        return """\
%(constructors)s
%(member_accessors)s

    // Object
    public String toString()
    {
%(member_tostrings)s
    }    

    // Serializable
    public String getTypeName() { return "%(type_qname)s%(type_name_suffix)s"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
%(member_serializers)s        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
%(member_deserializers)s    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
%(member_sizes)s
        return my_size;
    }

%(member_declarations)s
""" % locals()


class XtreemFSJavaStructType(JavaStructType, XtreemFSJavaStructTypeCommon):
    def generate( self, parents=" implements org.xtreemfs.interfaces.utils.Serializable" ):
        JavaStructType.generate( self, parents  )    
        
    def getDeserializer( self, identifier ): type_name = self.name; return "%(identifier)s = new %(type_name)s(); %(identifier)s.deserialize( buf );" % locals()    
    def getMemberDeserializers( self, members=None ): return "\n".join( [INDENT_SPACES * 2 + member.type.getDeserializer( member.identifier ) for member in self.members] )        
    def getMemberSerializers( self, members=None ): return "\n".join( [INDENT_SPACES * 2 + member.type.getSerializer( member.identifier ) for member in self.members] )        
    def getMemberSizes( self, members=None ): return "\n".join( [INDENT_SPACES * 2 + "my_size += " + member.type.getSize( member.identifier ) + ";" for member in self.members] )        
    def getSerializer( self, identifier ): return "%(identifier)s.serialize( writer );" % locals()
    def getSize( self, identifier ): return "%(identifier)s.calculateSize()" % locals()    



class XtreemFSJavaExceptionType(JavaExceptionType, XtreemFSJavaStructTypeCommon):
    def generate( self ):
        XtreemFSJavaStructType.generate( self, " extends org.xtreemfs.interfaces.utils.ONCRPCException" )

    def getExceptionFactory( self ):
        return "if ( exception_type_name.equals(\"%s\") ) return new %s();" % ( exception.qname, exception.name )
    
    
class XtreemFSJavaOperationType(JavaOperationType, XtreemFSJavaStructTypeCommon):
    def generate( self ):
        assert self.uid > 0, "operation "  + self.qname + " requires a positive UID for the XtreemFS Java generator (current uid = %i)" % self.uid
        for type_name_suffix in ( self.oneway and ( "Request", ) or ( "Request", "Response" ) ):
            type_name = self.name
            uid = self.uid
            parent_type_uid = self.parent_type.uid
            if type_name_suffix == "Request":
                params = [param for param in self.params if param.in_]   
                type_name_specific_type_def = """\
    // Request
    public int getInterfaceVersion() { return %(parent_type_uid)s; }    
    public int getOperationNumber() { return %(uid)s; }
    public Response createDefaultResponse() { return new %(type_name)sResponse(); }
""" % locals()
            elif type_name_suffix == "Response":
                params = [param for param in self.params if param.out_]            
                if self.return_type is not None: 
                    params.append( JavaOperationParameter( self.return_type, "returnValue", out_=True ) )
                type_name_specific_type_def = """\
    // Response
    public int getInterfaceVersion() { return %(parent_type_uid)s; }
    public int getOperationNumber() { return %(uid)s; }    
""" % locals()

            struct_body = self.getStructBody( type_name_suffix, params )
                                     
            writeGeneratedFile( self.getFilePath( type_name_suffix ), """
public class %(type_name)s%(type_name_suffix)s implements %(type_name_suffix)s
{
%(struct_body)s    

%(type_name_specific_type_def)s
}
""" % locals() )
        
    def getRequestFactory( self ): return ( INDENT_SPACES * 3 ) + "case %i: return new %sRequest();\n" % ( self.uid, self.name )                    
    def getResponseFactory( self ): return not self.oneway and ( ( INDENT_SPACES * 3 ) + "case %i: return new %sResponse();" % ( self.uid, self.name ) ) or ""                
        
           
if __name__ == "__main__":     
    if len( sys.argv ) == 1:
        sys.argv.extend( ( "-i", os.path.abspath( os.path.join( my_dir_path, "..", "..", "interfaces" ) ), 
                           "-o", os.path.abspath( os.path.join( my_dir_path, "..", "src" ) ) ) )
        
    generator_main( XtreemFSJavaTypeFactory( "org" ) )
