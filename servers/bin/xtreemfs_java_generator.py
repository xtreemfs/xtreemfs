#!/usr/bin/env python

import sys, os.path

try: 
    import yidl
except ImportError:
    my_dir_path = os.path.dirname( os.path.abspath( sys.modules[__name__].__file__ ) )
    yidl_dir_path = os.path.join( my_dir_path, "..", "share", "yidl", "src" )
    if not yidl_dir_path in sys.path: sys.path.append( yidl_dir_path )
    import yidl
    
from yidl.generator_helpers import *


__all__ = ["generateXtreemFSJava"]


# Constants
INDENT_SPACES = " " * 4
BASE_PACKAGE = "org"


# Helper functions
def _getPackage( module_or_interface ):
    assert isinstance( module_or_interface, Module ) or isinstance( module_or_interface, Interface )
    return BASE_PACKAGE + "." + module_or_interface.qidentifier.replace( "::", "." )

def _getPackageDirPath( module_or_interface ):
    assert isinstance( module_or_interface, Module ) or isinstance( module_or_interface, Interface )
    return os.path.join( BASE_PACKAGE.replace( ".", os.sep ), os.sep.join( module_or_interface.qidentifier.split( "::" ) ) )

def _makePackageDir( module_or_interface ):
    assert isinstance( module_or_interface, Module ) or isinstance( module_or_interface, Interface )
    package = _getPackage( module_or_interface )
    package_dir_path = _getPackageDirPath( module_or_interface )
    try: os.makedirs( package_dir_path )
    except: pass
    return package, package_dir_path

def _getParentImports( type ):
    assert isinstance( type, Type )
    parent_imports = []
    next_parent = type.parent
    while next_parent:        
        parent_package = _getPackage( next_parent )
        if parent_package != "org.xtreemfs":
            parent_imports.append( "import " + parent_package + ".*;" )
        next_parent = next_parent.parent
    parent_imports.reverse()
    parent_imports = "\n".join( parent_imports )
    return parent_imports
    
def _writeGeneratedFile( file_path, file_contents ):
    open( file_path, "w" ).write( file_contents )


# The main entry point
def generateXtreemFSJava( parsed_idl=True ):
    includes, modules = parsed_idl
    for module in modules:
        _generateXtreemFSJavaModule( module )

def _generateXtreemFSJavaModule( module ):
    _makePackageDir( module )

    _generateXtreemFSJavaConstants( module )
        
    for type in module.types:
        _generateXtreemFSJavaType( type )
        
    for interface in module.interfaces:
        _generateXtreemFSJavaInterface( interface )
    
    for module in module.modules:
        _generateXtreemFSJavaModule( module )
            
def _generateXtreemFSJavaInterface( interface ):
    assert interface.uid > 0, "interface "  + interface.qidentifier + " requires a positive UID for the XtreemFS Java generator (current uid = %i)" % interface.uid    
    interface_identifier= interface.identifier    
    
    parent_package = _getPackage( interface.parent )
    parent_dir_path = _getPackageDirPath( interface.parent )
    
    _writeGeneratedFile( os.path.join( parent_dir_path, "Serializable.java" ), """\
package %(parent_package)s;

import java.nio.ByteBuffer;


public interface Serializable
{
    public void serialize( ByteBuffer buf );
    public void deserialize( ByteBuffer buf );
    public int getSize();
};   
""" % locals() )    
    
    _writeGeneratedFile( os.path.join( parent_dir_path, "Request.java" ), """\
package %(parent_package)s;


public interface Request extends Serializable
{
    Response createDefaultResponse();
};   
""" % locals() )    

    _writeGeneratedFile( os.path.join( parent_dir_path, "Response.java" ), """\
package %(parent_package)s;


public interface Response extends Serializable
{
};   
""" % locals() )    
    
    _writeGeneratedFile( os.path.join( parent_dir_path, "ONCRPCRecordFragmentHeader.java" ), """\
package %(parent_package)s;

import java.nio.ByteBuffer;


public class ONCRPCRecordFragmentHeader implements Serializable
{
    public ONCRPCRecordFragmentHeader()
    {
        length = 0;
        is_last = true;
    }

    public int getRecordFragmentLength() { return length; }
    public boolean isLastRecordFragment() { return is_last; }
    
    // Serializable
    public void serialize( ByteBuffer buf )
    { }
    
    public void deserialize( ByteBuffer buf )
    {
        int record_fragment_marker = buf.getInt(); 
        is_last = ( record_fragment_marker << 31 ) != 0;
        length = record_fragment_marker ^ ( 1 << 31 );
        System.out.println( "Length " + Integer.toString( length ) );
        if ( is_last )
            System.out.println( "Last" );
    }

    public int getSize()
    {
        return Integer.SIZE;
    }

    private int length;
    private boolean is_last;
};
""" % locals() )    
    
    _writeGeneratedFile( os.path.join( parent_dir_path, "ONCRPCRequestHeader.java" ), """\
package %(parent_package)s;

import java.nio.ByteBuffer;


public class ONCRPCRequestHeader extends ONCRPCRecordFragmentHeader
{    
    public ONCRPCRequestHeader()
    {
        xid = prog = vers = proc = 0;        
    }

    public int getXID() { return xid; }
    public int getInterfaceNumber() { return prog - 20000000; }
    public int getInterfaceVersion() { return vers; }
    public int getOperationNumber() { return proc; }

    public String toString()
    {
        return "ONCRPCRequestHeader( " + Integer.toString( proc ) + " with record fragment size " + Integer.toString( getRecordFragmentLength() ) + " )";
    }
        
    // Serializable    
    public void deserialize( ByteBuffer buf )
    {        
        super.deserialize( buf );
        xid = buf.getInt();
        System.out.println( "XID " + Integer.toString( xid ) );
        int msg_type = buf.getInt();
        assert msg_type == 0; // CALL    
        int rpcvers = buf.getInt();
        System.out.println( "RPC version " + Integer.toString( rpcvers ) );
        assert rpcvers == 2;
        prog = buf.getInt();
        System.out.println( "Prog " + Integer.toString( prog ) );        
        vers = buf.getInt();
        System.out.println( "Vers " + Integer.toString( vers ) );        
        proc = buf.getInt();    
        System.out.println( "proc " + Integer.toString( proc ) );        
        buf.getInt(); // cred_auth_flavor
        buf.getInt(); // verf_auth_flavor
    }

    public int getSize()
    {
        return super.getSize() + Integer.SIZE * 4;
    }

    
    private int xid;
    private int prog;
    private int vers;
    private int proc;    
}
""" % locals() )

    package, package_dir_path = _makePackageDir( interface )

    _generateXtreemFSJavaConstants( interface )

    for exception in interface.exceptions:
        _generateXtreemFSJavaType( exception )

    request_factories = []
    operation_i = 1   
    for operation in interface.operations:
        assert operation.uid > 0, "operation "  + operation.qname + " requires a positive UID for the XtreemFS Java generator (current uid = %i)" % operation.uid
        _generateXtreemFSJavaType( operation, "Request" )
        request_factories.append( ( INDENT_SPACES * 3 ) + "case %i: return new %sRequest();" % ( operation_i, operation.name ) )        
        _generateXtreemFSJavaType( operation, "Response" )
        operation_i += 1
    request_factories = "\n".join( request_factories )
    
    _writeGeneratedFile( os.path.join( package_dir_path, interface.identifier + ".java" ), """\
package %(package)s;

import %(parent_package)s.Request;
import %(parent_package)s.ONCRPCRequestHeader;


class %(interface_identifier)s
{
    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        return createRequest( header.getOperationNumber() );
    }

    public static Request createRequest( int proc ) throws Exception
    {
        switch( proc )
        {
%(request_factories)s
            default: throw new Exception( "unknown request number " + Integer.toString( proc ) );
        }
    }
}
""" % locals() )

    _writeGeneratedFile( os.path.join( package_dir_path, interface.identifier + "_test.java" ), """\
package %(package)s;

import %(parent_package)s.Request;
import %(parent_package)s.Response;
import %(parent_package)s.ONCRPCRequestHeader;
import %(package)s.%(interface_identifier)s;

import java.nio.ByteBuffer;
import java.net.ServerSocket;
import java.net.Socket;


public class %(interface_identifier)s_test
{
    public static void main( String[] args ) throws Exception
    {
        System.out.println( "%(interface_identifier)s_test: waiting for incoming socket connection." );
    
        ServerSocket server_socket = new ServerSocket( 27095 );
        
        Socket peer_socket = server_socket.accept();

        System.out.println( "%(interface_identifier)s_test: accepted incoming socket connection." );
            
        byte[] read_bytes = new byte[64 * 1024];
        int read_len = peer_socket.getInputStream().read( read_bytes );
        System.out.println( "%(interface_identifier)s_test: read " + Integer.toString( read_len ) + " bytes from the socket " );
        
        ByteBuffer read_buf = ByteBuffer.wrap( read_bytes );
        
        // Read a request            
        ONCRPCRequestHeader header = new ONCRPCRequestHeader();
        header.deserialize( read_buf );
        Request req = %(interface_identifier)s.createRequest( header );
        System.out.println( "%(interface_identifier)s_test: got ONC-RPC request header " + header.toString() );        
        req.deserialize( read_buf );
        System.out.println( "%(interface_identifier)s_test: got ONC-RPC request " + req.toString() );

        byte[] write_bytes = new byte[64 * 1024];        
        ByteBuffer write_buf = ByteBuffer.wrap( write_bytes );
        Response resp = req.createDefaultResponse();
        resp.serialize( write_buf );
        peer_socket.getOutputStream().write( write_bytes );        
    }
};
""" % locals() )

        
def _generateXtreemFSJavaType( type, type_name_suffix="" ):        
    parent_imports = _getParentImports( type )    
    type_package = _getPackage( type.parent )    
    if len( type_name_suffix ) == 0:        
        type_def = getTypeTraits( type ).getTypeDef()
    else:
        type_def = getTypeTraits( type ).getTypeDef( type_name_suffix )
    
    _writeGeneratedFile( os.path.join( _getPackageDirPath( type.parent ), type.name + type_name_suffix + ".java" ),"""\
package %(type_package)s;

%(parent_imports)s

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.ArrayList;


         
%(type_def)s
""" % locals() )    

def _generateXtreemFSJavaConstants( module_or_interface ):
    if len( module_or_interface.constants ) > 0:
        constants_package = _getPackage( module_or_interface )
        constant_decls = ( "\n" + INDENT_SPACES ).join( ["public static final " + \
                                                     getTypeTraits( constant.type ).getDeclarationType() + \
                                                     constant.identifier                                                     
                                                     for constant in module_or_interface.constants] )
        _writeGeneratedFile( os.path.join( _getPackageDirpath( module_or_interface ), "Constants.java" ),"""\
package %(constants)s package;

public interface Constants
{
    %(constant_decls)s;
};
""" % locals() )    
            

def getTypeTraits( type ):
    if isinstance( type, MapType ): raise NotImplementedError
    
    try: 
        type_traits_type = globals()[type.__class__.__name__ + "Traits"]
        if type_traits_type == TypeTraits: raise KeyError
        return type_traits_type( type )
    except KeyError:
        pass
        
    if type.name == "string" or type.name == "wstring": return StringTypeTraits( type )
    elif type.name == "bool": return BoolTypeTraits( type )
    elif isNumericType( type.name ): return NumericTypeTraits( type )
    else: return StructTypeTraits( type )
    
    
class TypeTraits(dict):
    def __init__( self, type ):
        self.type = type

    def getBoxedType( self ): return self.getDeclarationType()
    def getDeclarationType( self ): return self.type.name

    
class StringTypeTraits(TypeTraits):
    def getDeclarationType( self ): return "String"
    def getDefaultInitializer( self, identifier ): return "%(identifier)s = \"\";" % locals()
    def getDeserializer( self, identifier ): return "{ int %(identifier)s_new_length = buf.getInt(); byte[] %(identifier)s_new_bytes = new byte[%(identifier)s_new_length]; buf.get( %(identifier)s_new_bytes ); %(identifier)s = new String( %(identifier)s_new_bytes ); }" % locals()
    def getSerializer( self, identifier ): return "buf.putInt( %(identifier)s.length() ); buf.put( %(identifier)s.getBytes() );" % locals()
    def getSize( self, identifier ): return "4 + ( %(identifier)s.length() + 4 - ( %(identifier)s.length() %% 4 ) )" % locals()
    def getToString( self, identifier ): return '"\\"" + %(identifier)s + "\\""' % locals()

    
class PrimitiveTypeTraits(TypeTraits): pass
   
    
class BoolTypeTraits(PrimitiveTypeTraits):
    def getBoxedType( self ): return "Boolean"
    def getDeclarationType( self ): return "boolean"
    def getDefaultInitializer( self, identifier ): return "%(identifier)s = false;" % locals(0)
    def getDeserializer( self, identifier ): return "%(identifier)s = buf.getInt() != 0;"
    def getSerializer( self, identifier ): return "buf.putInt( %(identifier)s ? 1 : 0 );" % locals()
    def getSize( self, identifier ): return "4"
    def getToString( self, identifier ): return "Boolean.toString( %(identifier)s )" % locals()

class NumericTypeTraits(PrimitiveTypeTraits):
    def getBoxedType( self ):
        decl_type = self.getDeclarationType()
        if decl_type == "int": return "Integer"
        else: return decl_type[0].upper() + decl_type[1:]
    
    def getDeclarationType( self ):
        if self.type.name == "float" or self.type.name == "double": return self.type.name
        elif self.type.name.endswith( "int8" ): return "byte"
        elif self.type.name.endswith( "int16" ): return "short"
        elif self.type.name.endswith( "int32" ): return "int"
        elif self.type.name.endswith( "int64" ): return "long"
        else: return "long" # mode_t, etc.

    def getDefaultInitializer( self, identifier ): return "%(identifier)s = 0;" % locals()
    
    def getDeserializer( self, identifier ):
        boxed_type= self.getBoxedType()
        if boxed_type == "Integer": boxed_type = "Int"
        return "%(identifier)s = buf.get%(boxed_type)s();" % locals()

    def getSerializer( self, identifier ):
        boxed_type= self.getBoxedType()
        if boxed_type == "Integer": boxed_type = "Int"
        return "buf.put%(boxed_type)s( %(identifier)s );" % locals()
        
    def getSize( self, identifier ): return self.getBoxedType() + ".SIZE"
    def getToString( self, identifier ): return self.getBoxedType() + ".toString( %(identifier)s )" % locals()


class CompoundTypeTraits(TypeTraits):
    def __init__( self, type ):
        TypeTraits.__init__( self, type )
        self.type_qname = BASE_PACKAGE + "." + self.type.qname.replace( "::", "." )
        
    def getDefaultInitializer( self, identifier ): type_qname = self.type_qname; return "%(identifier)s = new %(type_qname)s();" % locals()    
    def getDeserializer( self, identifier ): type_qname = self.type_qname; return "%(identifier)s = new %(type_qname)s(); %(identifier)s.deserialize( buf );" % locals()
    def getSerializer( self, identifier ): type_qname = self.type_qname; return "%(identifier)s.serialize( buf );" % locals()
    def getSize( self, identifier ): return "%(identifier)s.getSize()" % locals()
    def getToString( self, identifier ): return "%(identifier)s.toString()" % locals()
    
    
class SequenceTypeTraits(CompoundTypeTraits):
    def getTypeDef( self ):
        type_name = self.type.name
        value_type_traits = getTypeTraits( self.type.value_type )
        value_declaration_type = value_type_traits.getDeclarationType()
        value_boxed_type = value_type_traits.getBoxedType()
        value_serializer = value_type_traits.getSerializer( "next_value" )
        value_deserializer = value_type_traits.getDeserializer( "new_value" )
        next_value_size = value_type_traits.getSize( "next_value" )
        return """\
public class %(type_name)s extends ArrayList<%(value_boxed_type)s>
{    
    public void serialize( ByteBuffer buf )
    {
        buf.putInt( size() );
        for ( Iterator<%(value_boxed_type)s> i = iterator(); i.hasNext(); )
        {
            %(value_boxed_type)s next_value = i.next();        
            %(value_serializer)s;
        }
    }

    public void deserialize( ByteBuffer buf )
    {
        int new_size = buf.getInt();
        for ( int i = 0; i < new_size; i++ )
        {
            %(value_declaration_type)s new_value; %(value_deserializer)s;
            this.add( new_value );
        }
    }
    
    public int getSize()
    {
        int my_size= 0;
        for ( Iterator<%(value_boxed_type)s> i = iterator(); i.hasNext(); )
        {
            %(value_boxed_type)s next_value = i.next();
            my_size += %(next_value_size)s;
        }
        return my_size;
    }
}
""" % locals()
        

class StructTypeTraits(CompoundTypeTraits):
    def getTypeDef( self ):
        type_name = self.type.name
        struct_type_def = self.getStructTypeDef()
        return """\
public class %(type_name)s implements Serializable
{
%(struct_type_def)s
}
""" % locals()

    def getStructTypeDef( self, type_name_suffix="", members=None ):        
        if members is None: members = self.type.members
        type_name = self.type.name
        constructors = self.getConstructors( type_name_suffix, members )
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

    // Object
    public String toString()
    {
%(member_tostrings)s
    }    

    // Serializable
    public void serialize( ByteBuffer buf )
    {
%(member_serializers)s        
    }
    
    public void deserialize( ByteBuffer buf )
    {
%(member_deserializers)s    
    }
    
    public int getSize()
    {
        int my_size = 0;
%(member_sizes)s
        return my_size;
    }

%(member_declarations)s
""" % locals()

    def getConstructors( self, type_name_suffix="", members=None ):
        if members is None: members = self.type.members
        type_name = self.type.name                
        if len( members ) > 0:
            constructor = "\n" + INDENT_SPACES + "public " + self.type.name + type_name_suffix + "( " + \
                          ", ".join( [getTypeTraits( member.type ).getDeclarationType() + " " + member.identifier for member in members ] ) + \
                          " ) { " + " ".join( ["this." + member.identifier + " = " + member.identifier + ";" for member in members] ) + " }"
        else:
            constructor = ""           

        member_default_initializers = " ".join( [getTypeTraits( member.type ).getDefaultInitializer( member.identifier ) for member in members] )            
        return """\
    public %(type_name)s%(type_name_suffix)s() { %(member_default_initializers)s }%(constructor)s
""" % locals()
        
    def getMemberDeclarations( self, members=None ):
        if members is None: members = self.type.members
        return "\n".join( [INDENT_SPACES + "public " + getTypeTraits( member.type ).getDeclarationType() + " " + member.identifier + ";" for member in members] )        

    def getMemberToStrings( self, members=None ):
        if members is None: members = self.type.members
        return " + \", \" + ".join( [getTypeTraits( member.type ).getToString( member.identifier ) for member in members] )

    def getMemberDeserializers( self, members=None ):
        if members is None: members = self.type.members
        return "\n".join( [INDENT_SPACES * 2 + getTypeTraits( member.type ).getDeserializer( member.identifier ) for member in members] )        

    def getMemberSerializers( self, members=None ):
        if members is None: members = self.type.members
        return "\n".join( [INDENT_SPACES * 2 + getTypeTraits( member.type ).getSerializer( member.identifier ) for member in members] )        

    def getMemberSizes( self, members=None ):
        if members is None: members = self.type.members
        return "\n".join( [INDENT_SPACES * 2 + "my_size += " + getTypeTraits( member.type ).getSize( member.identifier ) + ";" for member in members] )        
        
        
class OperationTypeTraits(StructTypeTraits):
    def getTypeDef( self, type_name_suffix="Request" ):
        type_name = self.type.name
        if type_name_suffix == "Request":
            params = [param for param in self.type.params if param.in_]            
            respond = """\
    // Request
    public Response createDefaultResponse() { return new %(type_name)sResponse(); }
""" % locals()
        elif type_name_suffix == "Response":
            params = [param for param in self.type.params if param.out_]
            respond = ""

        param_type_traits = [( param, getTypeTraits( param.type ) ) for param in params]
        struct_type_def = StructTypeTraits.getStructTypeDef( self, type_name_suffix, params )
        
                         
        return """
public class %(type_name)s%(type_name_suffix)s implements %(type_name_suffix)s
{
%(struct_type_def)s    

%(respond)s
}
""" % locals()


class ExceptionTypeTraits(StructTypeTraits):
    def getTypeDef( self ):
        type_name = self.type.name
        struct_type_def= StructTypeTraits.getStructTypeDef( self )
        return """\
public class %(type_name)s extends Exception implements Serializable
{
    public %(type_name)s() { }
    public %(type_name)s( String msg ) { super( msg ); }        
%(struct_type_def)s    
}
""" % locals()
       
       
        

if __name__ == "__main__" and len( sys.argv ) >= 2:
    generateXtreemFSJava( yidl.parseIDL( sys.argv[1] ) )
