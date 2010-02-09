import sys, os.path, platform

SConscript( '../libxtreemfs/libxtreemfs.SConscript' )
SConscript( '../google-breakpad/google-breakpad.SConscript' )


try:
    Import( "build_env", "build_conf" )
except:
    build_env = {} # Init Environment() from this so that it doesn't start with default values for e.g. CC, which induces pkginfo popens on Sun

    include_dir_paths = os.environ.has_key( "CPPPATH" ) and os.environ["CPPPATH"].split( sys.platform.startswith( "win" ) and ';' or ':' ) or []

    if os.environ.get( "CC" ): build_env["CC"] = os.environ.get( "CC" )
    if os.environ.get( "CXX" ): build_env["CXX"] = os.environ.get( "CXX" )
    build_env["CCFLAGS"] = os.environ.get( "CCFLAGS", "" ).strip()
    lib_dir_paths = os.environ.has_key( "LIBPATH" ) and os.environ["LIBPATH"].split( sys.platform.startswith( "win" ) and ';' or ':' ) or []
    build_env["LINKFLAGS"] = os.environ.get( "LINKFLAGS", "" ).strip()
    build_env["LIBS"] = os.environ.has_key( "LIBS" ) and os.environ["LIBS"].split( " " ) or []

    if sys.platform.startswith( "win" ):
        if os.environ.has_key( "INCLUDE" ): include_dir_paths.extend( os.environ["INCLUDE"].split( ';' ) )
        if os.environ.has_key( "LIB" ): lib_dir_paths.extend( os.environ["LIB"].split( ';' ) )
        build_env["CCFLAGS"] += ' /EHsc /GR- /D "_CRT_DISABLE_PERFCRIT_LOCKS" /D "WIN32" /nologo ' # GR- is -fno-rtti, EHsc is to enable exception handling
        if ARGUMENTS.get( "release", 0 ): build_env["CCFLAGS"] += "/MD "
        else: build_env["CCFLAGS"] += "/MDd /ZI /W3 "
        if not "user32.lib" in build_env["LIBS"]: build_env["LIBS"].append( "user32.lib" )
        if not "advapi32.lib" in build_env["LIBS"]: build_env["LIBS"].append( "advapi32.lib" )
        if not "gdi32.lib" in build_env["LIBS"]: build_env["LIBS"].append( "gdi32.lib" )
    else:
        build_env["CCFLAGS"] += " -Wall -Wold-style-cast -Wunused-macros "
        if sys.platform == "linux2":
            build_env["CCFLAGS"] += "-D_FILE_OFFSET_BITS=64 "
            build_env["LIBS"].extend( ( "pthread", "util", "dl", "rt", "stdc++" ) )
        elif sys.platform == "darwin":
            build_env["CCFLAGS"] += " -D_FILE_OFFSET_BITS=64 "
            # build_env["LINKFLAGS"] += "-framework Carbon "
            build_env["LIBS"].append( "iconv" )
        elif sys.platform.startswith( "freebsd" ):
            build_env["CCFLAGS"] += "-D_FILE_OFFSET_BITS=64 "
            build_env["LIBS"].extend( ( "pthread", "intl", "iconv" ) )
        elif sys.platform == "sunos5":
            build_env["tools"] = ["gcc", "g++", "gnulink", "ar"]
            build_env["CCFLAGS"] += "-Dupgrade_the_compiler_to_use_STL=1 -D_REENTRANT "
            build_env["LIBS"].extend( ( "stdc++", "m", "socket", "nsl", "kstat", "rt", "iconv", "cpc" ) )

        if ARGUMENTS.get( "coverage", 0 ): build_env["CCFLAGS"] += "-pg --coverage "; build_env["LINKFLAGS"] += "-pg --coverage "
        if ARGUMENTS.get( "profile-cpu", 0 ):  build_env["CCFLAGS"] += "-pg "; build_env["LINKFLAGS"] += "-pg "
        if ARGUMENTS.get( "profile-heap", 0 ): build_env["CCFLAGS"] += "-fno-omit-frame-pointer "; build_env["LIBS"].append( "tcmalloc" )
        if ARGUMENTS.get( "release", 0 ): build_env["CCFLAGS"] += "-O2 "
        else: build_env["CCFLAGS"] += "-g -D_DEBUG "
        if ARGUMENTS.get( "shared", 0 ): build_env["CCFLAGS"] += "-fPIC "
        if not ARGUMENTS.get( "with-rtti", 0 ) and sys.platform != "darwin": build_env["CCFLAGS"] += "-fno-rtti " # Disable RTTI by default

    build_env["CPPPATH"] = list( set( [os.path.abspath( include_dir_path ) for include_dir_path in include_dir_paths] ) )
    build_env["LIBPATH"] = list( set( [os.path.abspath( lib_dir_path ) for lib_dir_path in lib_dir_paths] ) )
    build_env = Environment( **build_env )

    build_conf = build_env.Configure()
    if sys.platform.startswith( "linux" ) and platform.machine() == "i686": 
        build_env["CCFLAGS"] += "-march=i686 "

    Export( "build_env", "build_conf" )

defines = ["YIELD_HAVE_OPENSSL"]
if sys.platform.startswith( "win" ): defines.extend( [] )
else: defines.extend( [] )
for define in defines:
    if sys.platform.startswith( "win" ): define_switch = '/D "' + define + '"'
    else: define_switch = "-D" + define
    if not define_switch in build_env["CCFLAGS"]: build_env["CCFLAGS"] += define_switch + " "

include_dir_paths = [os.path.abspath( '../../share/yieldfs/include' ), os.path.abspath( '../../share/yidl/include' ), os.path.abspath( '../../share/yield/include' ), os.path.abspath( '../../share/google-breakpad/src' ), os.path.abspath( '../../include' )]
for include_dir_path in include_dir_paths:
    if not include_dir_path in build_env["CPPPATH"]: build_env["CPPPATH"].append( include_dir_path )

lib_dir_paths = [os.path.abspath( '../../lib' )]
for lib_dir_path in lib_dir_paths:
    if not lib_dir_path in build_env["LIBPATH"]: build_env["LIBPATH"].append( lib_dir_path )

# Don't add libs until after custom and dependency SConscripts, to avoid failing build_conf checks because of missing -l libs
for lib in ["xtreemfs"]:
   if not lib in build_env["LIBS"]: build_env["LIBS"].insert( 0, lib )

build_env.Program( "../../bin/xtfs_vivaldi", (
    r"../../src/xtfs_vivaldi/vivaldi_node.cpp",
    r"../../src/xtfs_vivaldi/xtfs_vivaldi.cpp",
    r"../../src/xtfs_vivaldi/zipf_generator.cpp" ) )
