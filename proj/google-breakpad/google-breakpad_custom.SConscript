import sys, platform

Import( "build_env", "build_conf" )


if sys.platform.startswith( "win" ):
    SConscript( "google-breakpad_windows.SConscript" )
    if ARGUMENTS.get( "release", 0 ): build_env["LIBS"].append( "google-breakpad.lib" )
    else: build_env["LIBS"].append( "google-breakpad_d.lib" )
elif sys.platform.startswith( "linux" ): # and platform.machine() != "x86_64":
    SConscript( "google-breakpad_linux.SConscript" )
    build_env["LIBS"].append( "google-breakpad" )
#else:
#    raise NotImplementedError, "Google Breakpad not supported on " + sys.platform
