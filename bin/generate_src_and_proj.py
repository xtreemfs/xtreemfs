import os.path, sys
from shutil import copyfile


# Constants
MY_DIR_PATH = os.path.dirname( os.path.abspath( sys.modules[__name__].__file__ ) )
GOOGLE_BREAKPAD_DIR_PATH = os.path.join( MY_DIR_PATH, "..", "share", "google-breakpad" )
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
                    
LIB_DIR_PATHS = ( 
                   os.path.join( XTREEMFS_DIR_PATH, "lib" ),
                )
                    
try:
    import yidl
except ImportError:
    sys.path.append( os.path.join( YIDL_DIR_PATH, "src", "py" ) )

from yidl.generators import generate_cpp, generate_proj, generate_SConscript, generate_vcproj
from yidl.utilities import format_src    


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
interfaces_dir_path = os.path.join( XTREEMFS_DIR_PATH, "src", "interfaces", "org", "xtreemfs", "interfaces" )
for interface_idl_file_name in os.listdir( interfaces_dir_path ):
    if interface_idl_file_name.endswith( ".idl" ):
        generate_cpp( os.path.join( interfaces_dir_path, interface_idl_file_name ), os.path.join( XTREEMFS_DIR_PATH, "include", "xtreemfs", "interfaces", os.path.splitext( interface_idl_file_name )[0] + ".h" ) ) 


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
                     
for binary_name in ( "lsfs.xtreemfs", "mkfs.xtreemfs", "mount.xtreemfs", "rmfs.xtreemfs", "xtfs_vivaldi" ):
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

                   
GOOGLE_BREAKPAD_INCLUDE_DIR_PATHS = ( os.path.join( GOOGLE_BREAKPAD_DIR_PATH, "src" ), )                   
GOOGLE_BREAKPAD_EXCLUDED_FILE_NAMES = ( "google_breakpad", )
GOOGLE_BREAKPAD_EXCLUDED_FILE_NAMES_WINDOWS = GOOGLE_BREAKPAD_EXCLUDED_FILE_NAMES + ( "linux", "mac", "solaris", "minidump_file_writer.*", "md5.*", "crash_generation_server.cc" )
GOOGLE_BREAKPAD_OUTPUT_FILE_PATH = os.path.join( XTREEMFS_DIR_PATH, "lib", "google-breakpad" )
GOOGLE_BREAKPAD_SRC_DIR_PATHS = ( os.path.join( GOOGLE_BREAKPAD_DIR_PATH, "src" ), )
                   
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
