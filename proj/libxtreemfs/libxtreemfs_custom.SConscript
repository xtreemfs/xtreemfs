import sys

Import( "build_env", "build_conf" )

if not sys.platform.startswith( "win") and not sys.platform.startswith( "sunos" ):
    if not "-DYIELD_HAVE_XATTR_H " in build_env["CCFLAGS"] and build_conf.CheckCHeader( "sys/xattr.h" ):
        build_env["CCFLAGS"] += "-DYIELD_HAVE_XATTR_H "
