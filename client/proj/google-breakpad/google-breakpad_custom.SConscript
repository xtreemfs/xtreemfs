import sys

Import( "build_env", "build_conf" )

if sys.platform.startswith( "win" ):
    SConscript( "google-breakpad_windows.SConscript" )
elif sys.platform.startswith( "linux" ):
    SConscript( "google-breakpad_linux.SConscript" )
else:
    raise NotImplementedError, "Google Breakpad not supported on " + sys.platform