#!/usr/bin/env python

import sys, os.path

my_dir_path = os.path.dirname( os.path.abspath( sys.modules[__name__].__file__ ) )

try: 
    import yidl
except ImportError:
    yidl_dir_path = os.path.join( my_dir_path, "..", "share" )
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
    print "wrote", file_path


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
    
    package, package_dir_path = _makePackageDir( interface )

    _generateXtreemFSJavaConstants( interface )

    for exception in interface.exceptions:
        _generateXtreemFSJavaType( exception )

    request_factories = []
    response_factories = []
    operation_i = 1   
    for operation in interface.operations:
        assert operation.uid > 0, "operation "  + operation.qname + " requires a positive UID for the XtreemFS Java generator (current uid = %i)" % operation.uid
        _generateXtreemFSJavaType( operation, "Request" )
        request_factories.append( ( INDENT_SPACES * 3 ) + "case %i: return new %sRequest();" % ( operation_i, operation.name ) )        
        _generateXtreemFSJavaType( operation, "Response" )
        response_factories.append( ( INDENT_SPACES * 3 ) + "case %i: return new %sResponse();" % ( operation_i, operation.name ) )                
        operation_i += 1
    request_factories = "\n".join( request_factories )
    response_factories = "\n".join( response_factories )
        
    _writeGeneratedFile( os.path.join( package_dir_path, interface.identifier + ".java" ), """\
package %(package)s;

import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.interfaces.utils.Request;
import org.xtreemfs.interfaces.utils.Response;


class %(interface_identifier)s
{
    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        return createRequest( header.getOperationNumber() );
    }

    public static Request createRequest( int uid ) throws Exception
    {
        switch( uid )
        {
%(request_factories)s
            default: throw new Exception( "unknown request number " + Integer.toString( uid ) );
        }
    }
    
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        if ( header.getReplyStat() == ONCRPCResponseHeader.ACCEPT_STAT_SUCCESS )
            return createResponse( header.getXID() );
        else
            throw new Exception( "not implemented" );
    }

    public static Response createResponse( int uid ) throws Exception
    {
        switch( uid )
        {
%(response_factories)s
            default: throw new Exception( "unknown response number " + Integer.toString( uid ) );
        }
    }    
}
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

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
%(type_def)s
""" % locals() )    

def _generateXtreemFSJavaConstants( module_or_interface ):
    if len( module_or_interface.constants ) > 0:
        constants_package = _getPackage( module_or_interface )
        constant_decls = ( "\n" + INDENT_SPACES ).join( ["public static final " + \
                                                     getTypeTraits( constant.type ).getDeclarationType() + " " + \
                                                     constant.identifier + " = " + \
                                                     getTypeTraits( constant.type ).getConstantValue( constant.value ) + ";"
                                                     for constant in module_or_interface.constants] )
        _writeGeneratedFile( os.path.join( _getPackageDirPath( module_or_interface ), "Constants.java" ),"""\
package %(constants_package)s;


public interface Constants
{
    %(constant_decls)s
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
    def getConstantValue( self, value ): return value
    def getDeclarationType( self ): return self.type.name

    
class StringTypeTraits(TypeTraits):
    def getConstantValue( self, value ): return "\"%(value)s\""
    def getDeclarationType( self ): return "String"
    def getDefaultInitializer( self, identifier ): return "%(identifier)s = \"\";" % locals()
    def getDeserializer( self, identifier ): return "{ int %(identifier)s_new_length = buf.getInt(); byte[] %(identifier)s_new_bytes = new byte[%(identifier)s_new_length]; buf.get( %(identifier)s_new_bytes ); %(identifier)s = new String( %(identifier)s_new_bytes ); }" % locals()
    def getSerializer( self, identifier ): return "writer.putInt( %(identifier)s.length() ); writer.put( %(identifier)s.getBytes() );" % locals()
    def getSize( self, identifier ): return "4 + ( %(identifier)s.length() + 4 - ( %(identifier)s.length() %% 4 ) )" % locals()
    def getToString( self, identifier ): return '"\\"" + %(identifier)s + "\\""' % locals()

    
class PrimitiveTypeTraits(TypeTraits): pass
   
    
class BoolTypeTraits(PrimitiveTypeTraits):
    def getBoxedType( self ): return "Boolean"
    def getDeclarationType( self ): return "boolean"
    def getDefaultInitializer( self, identifier ): return "%(identifier)s = false;" % locals(0)
    def getDeserializer( self, identifier ): return "%(identifier)s = buf.getInt() != 0;"
    def getSerializer( self, identifier ): return "writer.putInt( %(identifier)s ? 1 : 0 );" % locals()
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
        return "writer.put%(boxed_type)s( %(identifier)s );" % locals()
        
    def getSize( self, identifier ): return "( " + self.getBoxedType() + ".SIZE / 8 )"
    def getToString( self, identifier ): return self.getBoxedType() + ".toString( %(identifier)s )" % locals()


class CompoundTypeTraits(TypeTraits):
    def __init__( self, type ):
        TypeTraits.__init__( self, type )
        self.type_qname = BASE_PACKAGE + "." + self.type.qname.replace( "::", "." )
        
    def getDefaultInitializer( self, identifier ): type_qname = self.type_qname; return "%(identifier)s = new %(type_qname)s();" % locals()    
    def getDeserializer( self, identifier ): type_qname = self.type_qname; return "%(identifier)s = new %(type_qname)s(); %(identifier)s.deserialize( buf );" % locals()
    def getSerializer( self, identifier ): type_qname = self.type_qname; return "%(identifier)s.serialize( writer );" % locals()
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
    public void serialize(ONCRPCBufferWriter writer) {
        writer.putInt( size() );
        for ( Iterator<%(value_boxed_type)s> i = iterator(); i.hasNext(); )
        {
            %(value_boxed_type)s next_value = i.next();        
            %(value_serializer)s;
        }
    }

    public void deserialize( ReusableBuffer buf ) {
        int new_size = buf.getInt();
        for ( int i = 0; i < new_size; i++ )
        {
            %(value_declaration_type)s new_value; %(value_deserializer)s;
            this.add( new_value );
        }
    }
    
    public int getSize() {
        int my_size = Integer.SIZE/8;
        for ( Iterator<%(value_boxed_type)s> i = iterator(); i.hasNext(); ) {
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
    public void serialize(ONCRPCBufferWriter writer) {
%(member_serializers)s        
    }
    
    public void deserialize( ReusableBuffer buf )
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
        uid = self.type.uid
        if type_name_suffix == "Request":
            params = [param for param in self.type.params if param.in_]            
            type_name_specific_type_def = """\
    // Request
    public int getOperationNumber() { return %(uid)s; }
    public Response createDefaultResponse() { return new %(type_name)sResponse(); }
""" % locals()
        elif type_name_suffix == "Response":
            params = [param for param in self.type.params if param.out_]
            type_name_specific_type_def = """\
    // Response
    public int getOperationNumber() { return %(uid)s; }    
""" % locals()

        param_type_traits = [( param, getTypeTraits( param.type ) ) for param in params]
        struct_type_def = StructTypeTraits.getStructTypeDef( self, type_name_suffix, params )
        
                         
        return """
public class %(type_name)s%(type_name_suffix)s implements %(type_name_suffix)s
{
%(struct_type_def)s    

%(type_name_specific_type_def)s
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
       

if __name__ == "__main__":     
    src_dir_path = os.path.abspath( os.path.join( my_dir_path, "..", "src" ) )
    idl_dir_path = os.path.abspath( os.path.join( my_dir_path, "..", "..", "interfaces" ) )

    os.chdir( src_dir_path )
    for file_name in os.listdir( idl_dir_path ):
        if file_name.endswith( ".idl" ):
            file_path = os.path.join( idl_dir_path, file_name )
            try:
                parsed_idl = yidl.parseIDL( file_path )
            except: 
                print "Error parsing", file_path + ":"
                import traceback; traceback.print_exc() 
                print
                continue

            try:
                generateXtreemFSJava( parsed_idl )
                print "Successfully generated code for", file_path
            except:
                print "Error generating Java for", file_path + ":"
                import traceback; traceback.print_exc()
            print
