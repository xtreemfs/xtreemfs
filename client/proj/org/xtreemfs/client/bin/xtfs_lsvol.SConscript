import sys, os.path

SConscript( '../lib/xtreemfs-client-lib.SConscript' )    


try:
    Import( "build_env", "build_conf" )
except:
    build_env = {} # Init Environment() from this so that it doesn't start with default values for e.g. CC, which induces pkginfo popens on Sun

    include_dir_paths = os.environ.has_key( "CPPPATH" ) and os.environ["CPPPATH"].split( sys.platform.startswith( "win" ) and ';' or ':' ) or []
    
    build_env["CCFLAGS"] = os.environ.get( "CCFLAGS", "" )
    lib_dir_paths = os.environ.has_key( "LIBPATH" ) and os.environ["LIBPATH"].split( sys.platform.startswith( "win" ) and ';' or ':' ) or []
    build_env["LINKFLAGS"] = os.environ.get( "LINKFLAGS", "" )
    build_env["LIBS"] = os.environ.has_key( "LIBS" ) and os.environ["LIBS"].split( " " ) or []

    if sys.platform.startswith( "win" ):
        if os.environ.has_key( "INCLUDE" ): include_dir_paths.extend( os.environ["INCLUDE"].split( ';' ) )        
        if os.environ.has_key( "LIB" ): lib_dir_paths.extend( os.environ["LIB"].split( ';' ) )
        build_env["CCFLAGS"] += '/EHsc /GR- /D "_CRT_DISABLE_PERFCRIT_LOCKS" /D "WIN32" ' # GR- is -fno-rtti, EHsc is to enable exception handling
        if ARGUMENTS.get( "release", 0 ): build_env["CCFLAGS"] += "/MD "
        else: build_env["CCFLAGS"] += "/MDd /ZI /W3 "
    else:
        # -fPIC (Platform Independent Code) to compile a library as part of a shared object
        # -fno-rtti to disable RTTI
        # -Wall for all warnings
        build_env["CCFLAGS"] += "-fno-rtti -fPIC -Wall "
        if sys.platform == "linux2": build_env["CCFLAGS"] += "-D_FILE_OFFSET_BITS=64 "; build_env["LIBS"].extend( ( "pthread", "util", "dl", "rt", "stdc++" ) )
        elif sys.platform == "darwin": build_env["LINKFLAGS"] += "-framework Carbon "; build_env["LIBS"].append( "iconv" )
        elif sys.platform == "freebsd5": build_env["LIBS"].extend( ( "intl", "iconv" ) )
        elif sys.platform == "openbsd4": build_env["LIBS"].extend( ( "m", "pthread", "util", "iconv" ) )
        elif sys.platform == "sunos5": build_env["tools"] = ["gcc", "g++", "gnulink", "ar"]; build_env["CCFLAGS"] += "-Dupgrade_the_compiler_to_use_STL=1 -D_REENTRANT "; build_env["LIBS"].extend( ( "stdc++", "m", "socket", "nsl", "kstat", "rt", "iconv", "cpc" ) )
        if ARGUMENTS.get( "release", 0 ): build_env["CCFLAGS"] += "-O2 "
        else: build_env["CCFLAGS"] += "-g -D_DEBUG "
        if ARGUMENTS.get( "profile-cpu", 0 ):  build_env["CCFLAGS"] += "-pg "; build_env["LINKFLAGS"] += "-pg "
        if ARGUMENTS.get( "profile-heap", 0 ): build_env["CCFLAGS"] += "-fno-omit-frame-pointer "; build_env["LIBS"].append( "tcmalloc" )

    build_env["CPPPATH"] = list( set( [os.path.abspath( include_dir_path ) for include_dir_path in include_dir_paths] ) )
    build_env["LIBPATH"] = list( set( [os.path.abspath( lib_dir_path ) for lib_dir_path in lib_dir_paths] ) )        
    build_env = Environment( **build_env )
    build_conf = build_env.Configure()
    Export( "build_env", "build_conf" )

    
include_dir_paths = ['../../../../../include', '../../../../../share/yieldfs/include', '../../../../../share/yieldfs/share/yield/include']
if sys.platform.startswith( "win" ): include_dir_paths.extend( [] )
else: include_dir_paths.extend( [] )
for include_dir_path in include_dir_paths:
    include_dir_path = os.path.abspath( include_dir_path )
    if not include_dir_path in build_env["CPPPATH"]: build_env["CPPPATH"].append( include_dir_path )
    
lib_dir_paths = ['../../../../../lib']
if sys.platform.startswith( "win" ): lib_dir_paths.extend( [] )
else: lib_dir_paths.extend( [] )
for lib_dir_path in lib_dir_paths:
    lib_dir_path = os.path.abspath( lib_dir_path )
    if not lib_dir_path in build_env["LIBPATH"]: build_env["LIBPATH"].append( lib_dir_path )


for custom_SConscript in ["xtfs_lsvol_custom.SConscript"]:
    if FindFile( custom_SConscript, "." ):
        SConscript( custom_SConscript )

    
# Don't add libs until after xtfs_lsvol_custom.SConscript and dependency SConscripts, to avoid failing build_conf checks because of missing -l libs
for lib in ["xtreemfs-client"]:
   if not lib in build_env["LIBS"]: build_env["LIBS"].insert( 0, lib )

if sys.platform.startswith( "win" ):
    for lib in []:
       if not lib in build_env["LIBS"]: build_env["LIBS"].insert( 0, lib )
else:
    for lib in []:
       if not lib in build_env["LIBS"]: build_env["LIBS"].insert( 0, lib )

AlwaysBuild( build_env.Program( r"../../../../../bin/xtfs_lsvol", (
r"../../../../../src/org/xtreemfs/client/bin/xtfs_lsvol.cpp"
) ) )
