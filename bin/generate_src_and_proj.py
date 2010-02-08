import os.path, sys
from shutil import copyfile


# Constants
MY_DIR_PATH = os.path.dirname( os.path.abspath( sys.modules[__name__].__file__ ) )

GOOGLE_BREAKPAD_DIR_PATH = os.path.join( MY_DIR_PATH, "..", "share", "google-breakpad" )
GOOGLE_BREAKPAD_INCLUDE_DIR_PATHS = ( os.path.join( GOOGLE_BREAKPAD_DIR_PATH, "src" ), )                   
GOOGLE_BREAKPAD_EXCLUDED_FILE_NAMES = ( "google_breakpad", )
GOOGLE_BREAKPAD_EXCLUDED_FILE_NAMES_WINDOWS = GOOGLE_BREAKPAD_EXCLUDED_FILE_NAMES + ( "linux", "mac", "solaris", "minidump_file_writer.*", "md5.*", "crash_generation_server.cc" )
GOOGLE_BREAKPAD_OUTPUT_FILE_PATH = os.path.join( MY_DIR_PATH, "..", "lib", "google-breakpad" )
GOOGLE_BREAKPAD_SRC_DIR_PATHS = ( os.path.join( GOOGLE_BREAKPAD_DIR_PATH, "src" ), )

YIDL_DIR_PATH = os.path.abspath( os.path.join( MY_DIR_PATH, "..", "..", "yidl" ) )
YIELD_DIR_PATH = os.path.abspath( os.path.join( MY_DIR_PATH, "..", "..", "yield" ) )
YIELDFS_DIR_PATH = os.path.abspath( os.path.join( MY_DIR_PATH, "..", "..", "yieldfs" ) )

XTREEMFS_DIR_PATH = os.path.abspath( os.path.join( MY_DIR_PATH, ".." ) )


DEFINES = ( "YIELD_HAVE_OPENSSL", )

INCLUDE_DIR_PATHS = ( 
                      os.path.join( XTREEMFS_DIR_PATH, "include" ), 
                      os.path.join( XTREEMFS_DIR_PATH, "share", "yidl", "include" ),
                      os.path.join( XTREEMFS_DIR_PATH, "share", "yield", "include" ),                      
                      os.path.join( XTREEMFS_DIR_PATH, "share", "yieldfs", "include" )
                    )

IMPORTS = [
           "import java.io.StringWriter;",
           "import org.xtreemfs.interfaces.utils.*;",
           "import org.xtreemfs.common.buffer.ReusableBuffer;",
           "import yidl.runtime.PrettyPrinter;",
          ]

INTERFACES_DIR_PATH = os.path.join( XTREEMFS_DIR_PATH, "src", "interfaces", "org", "xtreemfs", "interfaces" )
                    
LIB_DIR_PATHS = ( 
                   os.path.join( XTREEMFS_DIR_PATH, "lib" ),
                )
                    
                    
try:
    import yidl
except ImportError:
    sys.path.append( os.path.join( YIDL_DIR_PATH, "src", "py" ) )

from yidl.compiler.idl_parser import parseIDL
from yidl.generators import generate_cpp, generate_proj, generate_SConscript, generate_vcproj
from yidl.utilities import format_src, pad, writeGeneratedFile 


# Copy yidl source and headers into share/
copyfile( os.path.join( YIDL_DIR_PATH, "include", "yidl.h" ), os.path.join( XTREEMFS_DIR_PATH, "share", "yidl", "include", "yidl.h" ) )

# Copy Yield source and headers into share/
copyfile( os.path.join( YIELD_DIR_PATH, "include", "yield", "main.h" ), os.path.join( XTREEMFS_DIR_PATH, "share", "yield", "include", "yield", "main.h" ) )
for file_stem in ( "concurrency", "ipc", "platform" ):
  copyfile( os.path.join( YIELD_DIR_PATH, "include", "yield", file_stem + ".h" ), os.path.join( XTREEMFS_DIR_PATH, "share", "yield", "include", "yield", file_stem + ".h" ) )
  copyfile( os.path.join( YIELD_DIR_PATH, "src", "yield", file_stem + ".cpp" ), os.path.join( XTREEMFS_DIR_PATH, "share", "yield", "src", "yield", file_stem + ".cpp" ) )
  
# Copy YieldFS source and headers into share/
copyfile( os.path.join( YIELDFS_DIR_PATH, "include", "yieldfs.h" ), os.path.join( XTREEMFS_DIR_PATH, "share", "yieldfs", "include", "yieldfs.h" ) )
copyfile( os.path.join( YIELDFS_DIR_PATH, "src", "yieldfs.cpp" ), os.path.join( XTREEMFS_DIR_PATH, "share", "yieldfs", "src", "yieldfs.cpp" ) )


# Generate .h interface definitions from .idl
for interface_idl_file_name in os.listdir( INTERFACES_DIR_PATH ):
    if interface_idl_file_name.endswith( ".idl" ):
		if interface_idl_file_name == "nettest_interface.idl":
			generate_cpp( os.path.join( INTERFACES_DIR_PATH, interface_idl_file_name ),
						  os.path.join( XTREEMFS_DIR_PATH, "src", "nettest.xtreemfs", "nettest_interface.h" ) )
		else:
			generate_cpp( 
				os.path.join( INTERFACES_DIR_PATH, interface_idl_file_name ), 
				os.path.join( XTREEMFS_DIR_PATH, "include", "xtreemfs", "interfaces", os.path.splitext( interface_idl_file_name )[0] + ".h" ) ) 


# Add copyright notices to the source, strip white space on the right        
format_src( "XtreemFS", 
            src_dir_paths=( 
                            os.path.join( XTREEMFS_DIR_PATH, "include" ),
                            os.path.join( XTREEMFS_DIR_PATH, "src", "libxtreemfs" ), 
                            os.path.join( XTREEMFS_DIR_PATH, "src", "lsfs.xtreemfs" ),
                            os.path.join( XTREEMFS_DIR_PATH, "src", "mkfs.xtreemfs" ),
                            os.path.join( XTREEMFS_DIR_PATH, "src", "mount.xtreemfs" ),
                            os.path.join( XTREEMFS_DIR_PATH, "src", "rmfs.xtreemfs" ),
                           ),
            start_year=2009 )


# Generate project files
os.chdir( os.path.join( XTREEMFS_DIR_PATH, "proj", "libxtreemfs" ) )
generate_proj( 
               "libxtreemfs",      
               defines=DEFINES,               
               include_dir_paths=INCLUDE_DIR_PATHS,
               lib_dir_paths=LIB_DIR_PATHS,
               libs_win=( "libeay32.lib", "ssleay32.lib" ),
               libs_unix=( "crypto", "fuse", "ssl", ),
               output_file_path=os.path.join( XTREEMFS_DIR_PATH, "lib", "xtreemfs" ),
               src_dir_paths=( 
                               os.path.join( XTREEMFS_DIR_PATH, "include", "xtreemfs" ),
                               os.path.join( XTREEMFS_DIR_PATH, "share", "yield", "src" ),                               
                               os.path.join( XTREEMFS_DIR_PATH, "share", "yieldfs", "src" ),
                               os.path.join( XTREEMFS_DIR_PATH, "src", "interfaces", "org", "xtreemfs" ),
                               os.path.join( XTREEMFS_DIR_PATH, "src", "libxtreemfs" ),
                             )
            )
                     
for binary_name in ( "lsfs.xtreemfs", "mkfs.xtreemfs", "mount.xtreemfs", "nettest.xtreemfs", "rmfs.xtreemfs", "xtfs_vivaldi" ):
    os.chdir( os.path.join( XTREEMFS_DIR_PATH, "proj", binary_name ) )
    generate_proj( 
                   binary_name, 
                   dependency_SConscripts=( 
                                            os.path.join( XTREEMFS_DIR_PATH, "proj", "libxtreemfs", "libxtreemfs.SConscript" ),
                                            os.path.join( XTREEMFS_DIR_PATH, "proj", "google-breakpad", "google-breakpad.SConscript" ),
                                          ),
                   defines=DEFINES,
                   include_dir_paths=INCLUDE_DIR_PATHS + ( os.path.join( GOOGLE_BREAKPAD_DIR_PATH, "src" ), ),
                   lib_dir_paths=LIB_DIR_PATHS,                   
                   libs=( "xtreemfs_d.lib", ),
                   output_file_path=os.path.join( XTREEMFS_DIR_PATH, "bin", binary_name ),
                   src_dir_paths=( os.path.join( XTREEMFS_DIR_PATH, "src", binary_name ), ),
                   type="exe",
                 )

                                      
os.chdir( os.path.join( XTREEMFS_DIR_PATH, "proj", "google-breakpad" ) )
generate_SConscript( "google-breakpad" )
generate_SConscript( 
                     "google-breakpad_linux", 
                     excluded_file_names=GOOGLE_BREAKPAD_EXCLUDED_FILE_NAMES + ( "windows", "mac", "solaris" ),
                     include_dir_paths=GOOGLE_BREAKPAD_INCLUDE_DIR_PATHS,
                     output_file_path=GOOGLE_BREAKPAD_OUTPUT_FILE_PATH,
                     src_dir_paths=GOOGLE_BREAKPAD_SRC_DIR_PATHS,
                   )
generate_SConscript( 
                     "google-breakpad_windows", 
                     defines=( "UNICODE", ),
                     excluded_file_names=GOOGLE_BREAKPAD_EXCLUDED_FILE_NAMES_WINDOWS,
                     include_dir_paths=GOOGLE_BREAKPAD_INCLUDE_DIR_PATHS,
                     output_file_path=GOOGLE_BREAKPAD_OUTPUT_FILE_PATH,
                     src_dir_paths=GOOGLE_BREAKPAD_SRC_DIR_PATHS,
                   )
generate_vcproj( 
                 "google-breakpad",
                 defines=( "UNICODE", ),
                 excluded_file_names=GOOGLE_BREAKPAD_EXCLUDED_FILE_NAMES_WINDOWS,
                 include_dir_paths=GOOGLE_BREAKPAD_INCLUDE_DIR_PATHS,
                 output_file_path=GOOGLE_BREAKPAD_OUTPUT_FILE_PATH,
                 src_dir_paths=GOOGLE_BREAKPAD_SRC_DIR_PATHS,
               )


# The former generate_xtreemfs_java
# The java_target import * must be here to avoid interfering with generate_cpp above
from yidl.compiler.targets.java_target import *


class XtreemFSJavaBufferType(JavaBufferType):
    def getDeclarationTypeName( self ): 
        return "ReusableBuffer"
    
    def getUnmarshalCall( self, decl_identifier, value_identifier ): 
        return value_identifier + """ = ( ReusableBuffer )unmarshaller.readBuffer( %(decl_identifier)s );""" % locals()
    

class XtreemFSJavaExceptionType(JavaExceptionType):
    def generate( self ): 
        XtreemFSJavaStructType( self.getScope(), self.getQualifiedName(), self.getTag(), ( "org.xtreemfs.interfaces.utils.ONCRPCException", ), self.getMembers() ).generate()
        
    def getExceptionFactory( self ): 
        return ( INDENT_SPACES * 3 ) + "case %i: return new %s();\n" % ( self.getTag(), self.getName() )


class XtreemFSJavaInterface(JavaInterface, JavaClass):    
    def generate( self ):                            
        class_header = self.getClassHeader()        
        constants = pad( "\n" + INDENT_SPACES, ( "\n" + INDENT_SPACES ).join( [repr( constant ) for constant in self.getConstants()] ), "\n\n" )
        prog = 0x20000000 + self.getTag()
        version = self.getTag()            
        out = """\
%(class_header)s%(constants)s
    public static int getProg() { return %(prog)u; }
    public static int getVersion() { return %(version)u; }
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
        return JavaClass.getImports( self ) + IMPORTS

    def getPackageDirPath( self ):                
        return os.sep.join( self.getQualifiedName() )
    
    def getPackageName( self ): 
        return ".".join( self.getQualifiedName() )


class XtreemFSJavaMapType(JavaMapType):
    def getImports( self ): 
        return JavaMapType.getImports( self ) + IMPORTS

    def getOtherMethods( self ):
        return """
    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        string_writer.append(this.getClass().getCanonicalName());
        string_writer.append(" ");
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeMap( "", this );
        return string_writer.toString();
    }
"""

                    
class XtreemFSJavaSequenceType(JavaSequenceType):
    def getImports( self ): 
        return JavaSequenceType.getImports( self ) + IMPORTS
    
    def getOtherMethods( self ):
        return """
    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        string_writer.append(this.getClass().getCanonicalName());
        string_writer.append(" ");
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeSequence( "", this );
        return string_writer.toString();
    }
"""


class XtreemFSJavaStructType(JavaStructType):        
    def getImports( self ):
        return JavaStructType.getImports( self ) + IMPORTS    

    def getOtherMethods( self ):
        return """
    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        string_writer.append(this.getClass().getCanonicalName());
        string_writer.append(" ");
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }
"""
    
class XtreemFSJavaOperation(JavaOperation):        
    def generate( self ):
        self._getRequestType().generate()
        self._getResponseType( "returnValue" ).generate()
                
    def getRequestFactory( self ): 
        return ( INDENT_SPACES * 3 ) + "case %i: return new %sRequest();\n" % ( self.getTag(), self.getName() )
                        
    def getResponseFactory( self ): 
        if self.isOneway():
            return ""
        else:
            return ( ( INDENT_SPACES * 3 ) + "case %i: return new %sResponse();" % ( self.getTag(), self.getName() ) )                 

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


# Generate .java interfaces from .idl
os.chdir( os.path.join( MY_DIR_PATH, "..", "src", "servers", "src" ) )        
for interface_idl_file_name in os.listdir( INTERFACES_DIR_PATH ):
    if interface_idl_file_name.endswith( ".idl" ):
        target = XtreemFSJavaTarget()
        parseIDL( os.path.join( INTERFACES_DIR_PATH, interface_idl_file_name ), target )
        target.generate()

