# Copyright (c) 2010 Minor Gordon
# All rights reserved
# 
# This source file is part of the XtreemFS project.
# It is licensed under the New BSD license:
# 
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
# * Redistributions of source code must retain the above copyright
# notice, this list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright
# notice, this list of conditions and the following disclaimer in the
# documentation and/or other materials provided with the distribution.
# * Neither the name of the XtreemFS project nor the
# names of its contributors may be used to endorse or promote products
# derived from this software without specific prior written permission.
# 
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
# WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL Minor Gordon BE LIABLE FOR ANY
# DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
# (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
# ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


import sys
from os import chdir, listdir, sep as os_sep
from os.path import abspath, dirname, exists, join, splitext
from optparse import OptionParser


# Constants
MY_DIR_PATH = dirname( abspath( sys.modules[__name__].__file__ ) )

GOOGLE_BREAKPAD_DIR_PATH = join( MY_DIR_PATH, "..", "share", "google-breakpad" )
GOOGLE_BREAKPAD_INCLUDE_DIR_PATHS = ( join( GOOGLE_BREAKPAD_DIR_PATH, "src" ), )
GOOGLE_BREAKPAD_OUTPUT_FILE_PATH = join( MY_DIR_PATH, "..", "lib", "google-breakpad" )

YIDL_DIR_PATH = abspath( join( MY_DIR_PATH, "..", "..", "yidl" ) )
YIELD_DIR_PATH = abspath( join( MY_DIR_PATH, "..", "..", "yield" ) )
YIELDFS_DIR_PATH = abspath( join( MY_DIR_PATH, "..", "..", "yieldfs" ) )

XTREEMFS_DIR_PATH = abspath( join( MY_DIR_PATH, ".." ) )


DEFINES = ( "YIELD_PLATFORM_HAVE_OPENSSL", )

INCLUDE_DIR_PATHS = \
(
    join( XTREEMFS_DIR_PATH, "include" ),
    join( XTREEMFS_DIR_PATH, "share", "yidl", "include" ),
    join( XTREEMFS_DIR_PATH, "share", "yield", "include" ),
    join( XTREEMFS_DIR_PATH, "share", "yieldfs", "include" ),
    join( GOOGLE_BREAKPAD_DIR_PATH, "src" )
)

IMPORTS = \
[
    "import java.io.StringWriter;",
    "import org.xtreemfs.interfaces.utils.*;",
    "import org.xtreemfs.common.buffer.ReusableBuffer;",
    "import yidl.runtime.PrettyPrinter;",
]

INTERFACES_DIR_PATH = join( XTREEMFS_DIR_PATH, "src", "interfaces", "org", "xtreemfs", "interfaces" )

LIB_DIR_PATHS = ( join( XTREEMFS_DIR_PATH, "lib" ), )


try:
    import yidl
except ImportError:
    sys.path.append( join( YIDL_DIR_PATH, "src", "py" ) )

from yidl.compiler.idl_parser import parse_idl
from yidl.generators import generate_proj, generate_SConscript, generate_vcproj
from yidl.generators import generate_yield_cpp
from yidl.utilities import copy_file, format_src, indent, pad, write_file


assert __name__ == "__main__"


option_parser = OptionParser()
option_parser.add_option( "-f", "--force", action="store_true", dest="force" )
options, ignore = option_parser.parse_args()


copy_file_paths = {}
# yidl
copy_file_paths[join( YIDL_DIR_PATH, "include", "yidl.h" )] = join( XTREEMFS_DIR_PATH, "share", "yidl", "include", "yidl.h" )
# yunit
copy_file_paths[join( YIDL_DIR_PATH, "include", "yunit.h" )] = join( XTREEMFS_DIR_PATH, "share", "yidl", "include", "yunit.h" )
# yield/main.h
copy_file_paths[join( YIELD_DIR_PATH, "include", "yield", "main.h" )] = join( XTREEMFS_DIR_PATH, "share", "yield", "include", "yield", "main.h" )
# Yield sub-project umbrella includes
for file_stem in ( "concurrency", "ipc", "platform" ):
  copy_file_paths[join( YIELD_DIR_PATH, "include", "yield", file_stem + ".h" )] = join( XTREEMFS_DIR_PATH, "share", "yield", "include", "yield", file_stem + ".h" )
  copy_file_paths[join( YIELD_DIR_PATH, "src", "yield", file_stem + ".cpp" )] = join( XTREEMFS_DIR_PATH, "share", "yield", "src", "yield", file_stem + ".cpp" )
# yield/platform _test.h's
for test_h_file_prefix in ( "directory", "file", "volume" ):
    copy_file_paths[join( YIELD_DIR_PATH, "src", "yield", "platform", test_h_file_prefix + "_test.h" )] = join( XTREEMFS_DIR_PATH, "share", "yield", "src", "yield", "platform", test_h_file_prefix + "_test.h" )

# YieldFS
copy_file_paths[join( YIELDFS_DIR_PATH, "include", "yieldfs.h" )] = join( XTREEMFS_DIR_PATH, "share", "yieldfs", "include", "yieldfs.h" )
copy_file_paths[join( YIELDFS_DIR_PATH, "src", "yieldfs.cpp" )] = join( XTREEMFS_DIR_PATH, "share", "yieldfs", "src", "yieldfs.cpp" )

for source_file_path, target_file_path in copy_file_paths.iteritems():
    if exists( source_file_path ):
        copy_file( source_file_path, target_file_path )


# Generate .h interface definitions from .idl
for interface_idl_file_name in listdir( INTERFACES_DIR_PATH ):
    if interface_idl_file_name.endswith( ".idl" ):
        if interface_idl_file_name == "nettest_interface.idl":
            generate_yield_cpp(
              join( INTERFACES_DIR_PATH, interface_idl_file_name ),
              join( XTREEMFS_DIR_PATH, "src", "nettest.xtreemfs", "nettest_interface.h" ),
              force=options.force
            )
        else:
            generate_yield_cpp(
                join( INTERFACES_DIR_PATH, interface_idl_file_name ),
                join( XTREEMFS_DIR_PATH, "include", "xtreemfs", "interfaces", splitext( interface_idl_file_name )[0] + ".h" ),
                force=options.force
            )


# Add copyright notices to the source, strip white space on the right
format_src(
    author="Minor Gordon",
    force=options.force,
    project="XtreemFS",
    src_paths=(
        join( XTREEMFS_DIR_PATH, "bin", "generate_src_and_proj.py" ),
        join( XTREEMFS_DIR_PATH, "include" ),
        join( XTREEMFS_DIR_PATH, "include", "xtreemfs" ),
        join( XTREEMFS_DIR_PATH, "src", "libxtreemfs" ),
        join( XTREEMFS_DIR_PATH, "src", "lsfs.xtreemfs" ),
        join( XTREEMFS_DIR_PATH, "src", "mkfs.xtreemfs" ),
        join( XTREEMFS_DIR_PATH, "src", "mount.xtreemfs" ),
        join( XTREEMFS_DIR_PATH, "src", "nettest.xtreemfs", "nettest.xtreemfs.cpp" ),
        join( XTREEMFS_DIR_PATH, "src", "nettest.xtreemfs", "nettest_proxy.h" ),
        join( XTREEMFS_DIR_PATH, "src", "rmfs.xtreemfs" ),
    )
)


# Generate project files
chdir( join( XTREEMFS_DIR_PATH, "proj", "libxtreemfs" ) )
generate_proj(
    "libxtreemfs",
    defines=DEFINES,
    force=options.force,
    include_dir_paths=INCLUDE_DIR_PATHS,
    lib_dir_paths=LIB_DIR_PATHS,
    libs_win=( "libeay32.lib", "ssleay32.lib" ),
    libs_unix=( "crypto", "fuse", "ssl", ),
    output_file_path=join( XTREEMFS_DIR_PATH, "lib", "xtreemfs" ),
    src_paths=(
        join( XTREEMFS_DIR_PATH, "include" ),
        join( XTREEMFS_DIR_PATH, "share", "yield" ),
        join( XTREEMFS_DIR_PATH, "share", "yieldfs" ),
        join( XTREEMFS_DIR_PATH, "src", "interfaces" ),
        join( XTREEMFS_DIR_PATH, "src", "libxtreemfs" ),
    )
)

for binary_name in ( "lsfs.xtreemfs", "mkfs.xtreemfs", "mount.xtreemfs", "nettest.xtreemfs", "rmfs.xtreemfs", "xtfs_vivaldi" ):
    chdir( join( XTREEMFS_DIR_PATH, "proj", binary_name ) )
    generate_proj(
                   binary_name,
                   dependency_SConscripts=(
                                            join( XTREEMFS_DIR_PATH, "proj", "libxtreemfs", "libxtreemfs.SConscript" ),
                                            join( XTREEMFS_DIR_PATH, "proj", "google-breakpad", "google-breakpad.SConscript" ),
                                          ),
                   defines=DEFINES,
                   include_dir_paths=INCLUDE_DIR_PATHS,
                   force=options.force,
                   lib_dir_paths=LIB_DIR_PATHS,
                   libs=( "xtreemfs_d.lib", ),
                   output_file_path=join( XTREEMFS_DIR_PATH, "bin", binary_name ),
                   src_paths=( join( XTREEMFS_DIR_PATH, "src", binary_name ), ),
                   type="exe",
                 )


chdir( join( XTREEMFS_DIR_PATH, "proj", "google-breakpad" ) )

generate_SConscript( "google-breakpad", force=options.force )

generate_SConscript(
    "google-breakpad_linux",
    force=options.force,
    include_dir_paths=GOOGLE_BREAKPAD_INCLUDE_DIR_PATHS,
    output_file_path=GOOGLE_BREAKPAD_OUTPUT_FILE_PATH,
    src_paths=
    (
        join( GOOGLE_BREAKPAD_DIR_PATH, "src", "client", "minidump_file_writer.cc" ),
        join( GOOGLE_BREAKPAD_DIR_PATH, "src", "client", "linux" ),
        join( GOOGLE_BREAKPAD_DIR_PATH, "src", "common", "*.c" ),
        join( GOOGLE_BREAKPAD_DIR_PATH, "src", "common", "*.cc" ),
        join( GOOGLE_BREAKPAD_DIR_PATH, "src", "common", "dwarf" ),
        join( GOOGLE_BREAKPAD_DIR_PATH, "src", "common", "linux" ),
    )
)

generate_SConscript(
    "google-breakpad_windows",
    defines=( "UNICODE", ),
    force=options.force,
    include_dir_paths=GOOGLE_BREAKPAD_INCLUDE_DIR_PATHS,
    output_file_path=GOOGLE_BREAKPAD_OUTPUT_FILE_PATH,
    src_paths=
    (
        join( GOOGLE_BREAKPAD_DIR_PATH, "src", "client", "windows", "crash_generation", "*.cc" ),
        join( GOOGLE_BREAKPAD_DIR_PATH, "src", "client", "windows", "handler", "*.cc" ),
        join( GOOGLE_BREAKPAD_DIR_PATH, "src", "common", "convert_UTF.c" ),
        join( GOOGLE_BREAKPAD_DIR_PATH, "src", "common", "*.cc" ),
        join( GOOGLE_BREAKPAD_DIR_PATH, "src", "common", "windows", "*.cc" ),
    )
)

generate_vcproj(
    "google-breakpad",
    defines=( "UNICODE", ),
    force=options.force,
    include_dir_paths=GOOGLE_BREAKPAD_INCLUDE_DIR_PATHS,
    output_file_path=GOOGLE_BREAKPAD_OUTPUT_FILE_PATH,
    src_paths=
    (
        join( GOOGLE_BREAKPAD_DIR_PATH, "src", "client", "windows", "crash_generation", "*.cc" ),
        join( GOOGLE_BREAKPAD_DIR_PATH, "src", "client", "windows", "handler", "*.cc" ),
        join( GOOGLE_BREAKPAD_DIR_PATH, "src", "common", "convert_UTF.c" ),
        join( GOOGLE_BREAKPAD_DIR_PATH, "src", "common", "*.cc" ),
        join( GOOGLE_BREAKPAD_DIR_PATH, "src", "common", "windows", "*.cc" ),
    )
)


# The java_target import * must be here to avoid interfering with generate_cpp above
from yidl.compiler.targets.java_target import *


class XtreemFSJavaBufferType(JavaBufferType):
    def get_java_name( self ):
        return "ReusableBuffer"

    def get_unmarshal_call( self, decl_identifier, value_identifier ):
        return value_identifier + """ = ( ReusableBuffer )unmarshaller.readBuffer( %(decl_identifier)s );""" % locals()


class XtreemFSJavaExceptionType(JavaExceptionType):
    def generate( self ):
        XtreemFSJavaStructType( self.get_scope(), self.get_qname(), self.get_tag(), ( "org.xtreemfs.interfaces.utils.ONCRPCException", ), self.get_members() ).generate()

    def get_factory( self ):
        return "case %i: return new %s();" % ( self.get_tag(), self.get_name() )


class XtreemFSJavaInterface(JavaInterface, JavaClass):
    def generate( self ):
        class_header = self.get_class_header()
        constants = indent( INDENT_SPACES, pad( "\n", "\n".join( [repr( constant ) for constant in self.get_constants()] ), "\n\n" ) )
        prog = 0x20000000 + self.get_tag()
        version = self.get_tag()
        out = """\
%(class_header)s%(constants)s
    public static long getProg() { return %(prog)ul; }
    public static int getVersion() { return %(version)u; }
""" % locals()

        exception_factories = indent( INDENT_SPACES * 3, "\n".join( [exception_type.get_factory() for exception_type in self.get_exception_types()] ) )
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

        request_factories = indent( INDENT_SPACES * 3, "\n".join( [operation.get_request_type().get_factory() for operation in self.get_operations()] ) )
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

        response_factories = indent( INDENT_SPACES * 3, "\n".join( [operation.get_response_type().get_factory() for operation in self.get_operations() if not operation.is_oneway()] ) )
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

        out += self.get_class_footer()

        write_file( self.get_file_path(), out )

        for operation in self.get_operations():
            operation.generate()

        for exception_type in self.get_exception_types():
            exception_type.generate()

    def get_imports( self ):
        return JavaClass.get_imports( self ) + IMPORTS

    def get_package_dir_path( self ):
        return os_sep.join( self.get_qname() )

    def get_package_name( self ):
        return ".".join( self.get_qname() )


class XtreemFSJavaMapType(JavaMapType):
    def get_imports( self ):
        return JavaMapType.get_imports( self ) + IMPORTS

    def get_other_methods( self ):
        return """\
// java.lang.Object
public String toString()
{
    StringWriter string_writer = new StringWriter();
    string_writer.append(this.getClass().getCanonicalName());
    string_writer.append(" ");
    PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
    pretty_printer.writeMap( "", this );
    return string_writer.toString();
}"""


class XtreemFSJavaSequenceType(JavaSequenceType):
    def get_imports( self ):
        return JavaSequenceType.get_imports( self ) + IMPORTS

    def get_other_methods( self ):
        return """\
// java.lang.Object
public String toString()
{
    StringWriter string_writer = new StringWriter();
    string_writer.append(this.getClass().getCanonicalName());
    string_writer.append(" ");
    PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
    pretty_printer.writeSequence( "", this );
    return string_writer.toString();
}"""


class XtreemFSJavaStructType(JavaStructType):
    def get_imports( self ):
        return JavaStructType.get_imports( self ) + IMPORTS

    def get_other_methods( self ):
        return """\
// java.lang.Object
public String toString()
{
    StringWriter string_writer = new StringWriter();
    string_writer.append(this.getClass().getCanonicalName());
    string_writer.append(" ");
    PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
    pretty_printer.writeStruct( "", this );
    return string_writer.toString();
}"""

class XtreemFSJavaOperation(JavaOperation):
    def generate( self ):
        self.get_request_type().generate()
        self.get_response_type().generate()

    def get_request_type( self ):
        try:
            return self.__request_type
        except AttributeError:
            request_type_name = self.get_name() + "Request"
            request_params = [] 
            for in_param in self.get_in_parameters():
                if not in_param in self.get_out_parameters():
                    request_params.append( in_param )
            self.__request_type = self._create_construct( "RequestType", XtreemFSJavaRequestType, self.get_qname()[:-1] + [request_type_name], self.get_tag(), None, request_params )
            return self.__request_type        

    def get_response_type( self ):
        return self._get_response_type( "returnValue" )


class XtreemFSJavaRequestType(XtreemFSJavaStructType):
    def get_factory( self ):
        return "case %i: return new %s();" % ( self.get_tag(), self.get_name() )

    def get_other_methods( self ):
        response_type_name = self.get_name()[:self.get_name().index( "Request" )] + "Response"
        return XtreemFSJavaStructType.get_other_methods( self ) + """

// Request
public Response createDefaultResponse() { return new %(response_type_name)s(); }""" % locals()

    def get_parent_names( self ):
        return ( "org.xtreemfs.interfaces.utils.Request", )


class XtreemFSJavaResponseType(XtreemFSJavaStructType):
    def get_factory( self ):
        return "case %i: return new %s();" % ( self.get_tag(), self.get_name() )

    def get_parent_names( self ):
        return ( "org.xtreemfs.interfaces.utils.Response", )


class XtreemFSJavaTarget(JavaTarget): pass


# Generate .java interfaces from .idl
chdir( join( MY_DIR_PATH, "..", "src", "servers", "src" ) )
for interface_idl_file_name in listdir( INTERFACES_DIR_PATH ):
    if interface_idl_file_name.endswith( ".idl" ):
        target = XtreemFSJavaTarget()
        parse_idl( join( INTERFACES_DIR_PATH, interface_idl_file_name ), target )
        target.generate()
