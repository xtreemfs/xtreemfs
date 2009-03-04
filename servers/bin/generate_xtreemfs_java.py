#!/usr/bin/env python

import sys, os.path

my_dir_path = os.path.dirname( os.path.abspath( sys.modules[__name__].__file__ ) )
try:
    import yidl
except ImportError:        
    yidl_dir_path = os.path.join( my_dir_path, "..", "share" )
    if not yidl_dir_path in sys.path: sys.path.append( yidl_dir_path )
    import yidl
    
from yidl.java_generator import *


__all__ = ["XtreemFSJavaGenerator"]


class XtreemFSJavaGenerator(JavaGenerator):
    def __init__( self, base_package_name="org" ):
        JavaGenerator.__init__( self, base_package_name )

    # Generator
    def generateType( self, type, type_name_suffix="" ):        
        package_name = self._getModulePackageName( type.parent )    
        imports = self._getTypeImports( type, exclude_package_names=( "org.xtreemfs", ) )    
        type_def = len( type_name_suffix ) == 0 and self._getTypeTraits( type ).getTypeDef() or self._getTypeTraits( type ).getTypeDef( type_name_suffix )    
    
        writeGeneratedFile( os.path.join( self._getModulePackageDirPath( type.parent ), type.name + type_name_suffix + ".java" ),"""\
package %(package_name)s;

%(imports)s
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
%(type_def)s
""" % locals() )    
            
    def generateInterface( self, interface ):                        
        assert interface.uid > 0, "interface "  + interface.qidentifier + " requires a positive UID for the XtreemFS Java generator (current uid = %i)" % interface.uid    
        
        package_name, package_dir_path = self._makeModulePackageDir( interface )
    
        self._generateConstants( interface )
    
        interface_identifier= interface.identifier    
        interface_uid = interface.uid
    
        out = """\
package %(package_name)s;

import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.interfaces.utils.Request;
import org.xtreemfs.interfaces.utils.Response;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.interfaces.Exceptions.*;


public class %(interface_identifier)s
{
    public static int getVersion() { return %(interface_uid)s; }
""" % locals()

        if len( interface.operations ) > 0:        
            request_factories = []
            response_factories = []
            for operation in interface.operations:
                assert operation.uid > 0, "operation "  + operation.qname + " requires a positive UID for the XtreemFS Java generator (current uid = %i)" % operation.uid
                self.generateType( operation, "Request" )
                request_factories.append( ( INDENT_SPACES * 3 ) + "case %i: return new %sRequest();" % ( operation.uid, operation.name ) )        
                if not operation.oneway:
                    self.generateType( operation, "Response" )
                    response_factories.append( ( INDENT_SPACES * 3 ) + "case %i: return new %sResponse();" % ( operation.uid, operation.name ) )                
            request_factories = "\n".join( request_factories )
            response_factories = "\n".join( response_factories )
        
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

        if len( interface.exceptions ) > 0:
            exception_factories = []
            for exception in interface.exceptions:
    #            assert operation.uid > 0, "operation "  + operation.qname + " requires a positive UID for the XtreemFS Java generator (current uid = %i)" % operation.uid
                self.generateType( exception )
                exception_factories.append( "if ( exception_type_name.equals(\"%s\") ) return new %s();" % ( exception.qname, exception.name ) )
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
                
        writeGeneratedFile( os.path.join( package_dir_path, interface.identifier + ".java" ), out ) 


class XtreemFSJavaBoolTypeTraits(JavaBoolTypeTraits):
    def getDeserializer( self, identifier ): return "%(identifier)s = buf.getInt() != 0;" % locals()
    def getSerializer( self, identifier ): return "writer.putInt( %(identifier)s ? 1 : 0 );" % locals()
    def getSize( self, identifier ): return "4"




class XtreemFSJavaNumericTypeTraits(JavaNumericTypeTraits):
    def getDeserializer( self, identifier ):
        boxed_type= self.getBoxedType()
        if boxed_type == "Integer": boxed_type = "Int"
        return "%(identifier)s = buf.get%(boxed_type)s();" % locals()

    def getSerializer( self, identifier ):
        boxed_type= self.getBoxedType()
        if boxed_type == "Integer": boxed_type = "Int"
        return "writer.put%(boxed_type)s( %(identifier)s );" % locals()
        
    def getSize( self, identifier ): return "( " + self.getBoxedType() + ".SIZE / 8 )"

                    
class XtreemFSJavaSequenceTypeTraits(JavaSequenceTypeTraits):
    def getDeserializer( self, identifier ): type_qname = self.type_qname; return "%(identifier)s = new %(type_qname)s(); %(identifier)s.deserialize( buf );" % locals()
    def getSerializer( self, identifier ): type_qname = self.type_qname; return "%(identifier)s.serialize( writer );" % locals()
    def getSize( self, identifier ): return "%(identifier)s.calculateSize()" % locals()
    
    def getTypeDef( self ):
        type_name = self.type.name
        type_qname = self.type.qname
        value_type_traits = self._getValueTypeTraits()
        value_declaration_type = value_type_traits.getDeclarationType()
        value_boxed_type = value_type_traits.getBoxedType()
        value_serializer = value_type_traits.getSerializer( "next_value" )
        value_deserializer = value_type_traits.getDeserializer( "new_value" )
        next_value_size = value_type_traits.getSize( "next_value" )
        return """\
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
""" % locals()


class XtreemFSJavaSerializableTypeTraits(JavaSerializableTypeTraits):
    def getDeclarationType( self ): return "ReusableBuffer"
    def getDeserializer( self, identifier ): return "{ %(identifier)s = org.xtreemfs.interfaces.utils.XDRUtils.deserializeSerializableBuffer(buf); }" % locals()
    def getSerializer( self, identifier ): return "{ org.xtreemfs.interfaces.utils.XDRUtils.serializeSerializableBuffer(%(identifier)s,writer); }" % locals()
    def getSize( self, identifier ): return "org.xtreemfs.interfaces.utils.XDRUtils.serializableBufferLength(%(identifier)s)" % locals()
        

class XtreemFSJavaStringTypeTraits(JavaStringTypeTraits):
    def getDeserializer( self, identifier ): return "{ %(identifier)s = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }" % locals()
    def getSerializer( self, identifier ): return "{ org.xtreemfs.interfaces.utils.XDRUtils.serializeString(%(identifier)s,writer); }" % locals()
    def getSize( self, identifier ): return "4 + ( %(identifier)s.length() + 4 - ( %(identifier)s.length() %% 4 ) )" % locals()


class XtreemFSJavaStructTypeTraits(JavaStructTypeTraits):    
    def getDeserializer( self, identifier ): type_qname = self.type_qname; return "%(identifier)s = new %(type_qname)s(); %(identifier)s.deserialize( buf );" % locals()    
    def getMemberDeserializers( self, members=None ): return "\n".join( [INDENT_SPACES * 2 + member_type_traits.getDeserializer( member.identifier ) for member, member_type_traits in self._getMemberTypeTraits( members )] )        
    def getMemberSerializers( self, members=None ): return "\n".join( [INDENT_SPACES * 2 + member_type_traits.getSerializer( member.identifier ) for member, member_type_traits in self._getMemberTypeTraits( members )] )        
    def getMemberSizes( self, members=None ): return "\n".join( [INDENT_SPACES * 2 + "my_size += " + member_type_traits.getSize( member.identifier ) + ";" for member, member_type_traits in self._getMemberTypeTraits( members )] )        
    def getSerializer( self, identifier ): type_qname = self.type_qname; return "%(identifier)s.serialize( writer );" % locals()
    def getSize( self, identifier ): return "%(identifier)s.calculateSize()" % locals()    

    def getTypeDef( self ):
        type_name = self.type.name
        struct_type_def = self._getTypeDef()
        return """\
   
public class %(type_name)s implements org.xtreemfs.interfaces.utils.Serializable
{
%(struct_type_def)s
}
""" % locals()

    def _getTypeDef( self, type_name_suffix="", members=None ):        
        if members is None: members = self.type.members
        type_name = self.type.name
        type_qname = self.type.qname
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

class XtreemFSJavaExceptionTypeTraits(XtreemFSJavaStructTypeTraits):
    def getTypeDef( self ):
        type_name = self.type.name
        struct_type_def= XtreemFSJavaStructTypeTraits._getTypeDef( self )
        return """\
public class %(type_name)s extends org.xtreemfs.interfaces.utils.ONCRPCException 
{
%(struct_type_def)s    
}
""" % locals()


class XtreemFSJavaOperationTypeTraits(XtreemFSJavaStructTypeTraits):
    def getTypeDef( self, type_name_suffix="Request" ):
        type_name = self.type.name
        uid = self.type.uid
        parent_uid = self.type.parent.uid
        if type_name_suffix == "Request":
            params = [param for param in self.type.params if param.in_]   
            type_name_specific_type_def = """\
    // Request
    public int getInterfaceVersion() { return %(parent_uid)s; }    
    public int getOperationNumber() { return %(uid)s; }
    public Response createDefaultResponse() { return new %(type_name)sResponse(); }
""" % locals()
        elif type_name_suffix == "Response":
            params = [param for param in self.type.params if param.out_]            
            if self.type.return_type is not None: 
                params.append( OperationParameter( self.type, self.type.return_type, "returnValue", out_=True ) )
            type_name_specific_type_def = """\
    // Response
    public int getInterfaceVersion() { return %(parent_uid)s; }
    public int getOperationNumber() { return %(uid)s; }    
""" % locals()

        param_type_traits = XtreemFSJavaStructTypeTraits._getMemberTypeTraits( self, params )
        struct_type_def = XtreemFSJavaStructTypeTraits._getTypeDef( self, type_name_suffix, params )
                                 
        return """
public class %(type_name)s%(type_name_suffix)s implements %(type_name_suffix)s
{
%(struct_type_def)s    

%(type_name_specific_type_def)s
}
""" % locals()
        
           
if __name__ == "__main__":     
    if len( sys.argv ) == 1:
        sys.argv.extend( ( "-i", os.path.abspath( os.path.join( my_dir_path, "..", "..", "interfaces" ) ), 
                           "-o", os.path.abspath( os.path.join( my_dir_path, "..", "src" ) ) ) )
        
    generator_main( XtreemFSJavaGenerator() )
