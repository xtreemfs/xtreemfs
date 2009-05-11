@ECHO OFF

set GOOGLE_BREAKPAD_PATH=%CD%\share\google-breakpad
set GOOGLE_BREAKPAD_COMMON_SOURCE_FLAGS=-s %XTREEMFS_CLIENT_PATH%\share\google-breakpad\src -e google_breakpad -I %XTREEMFS_CLIENT_PATH%\share\google-breakpad\src
set GOOGLE_BREAKPAD_WINDOWS_SOURCE_FLAGS=%GOOGLE_BREAKPAD_COMMON_SOURCE_FLAGS% -D UNICODE -e linux -e mac -e solaris -e minidump_file_writer* -e md5.* -e crash_generation_server.cc
set GOOGLE_BREAKPAD_LINUX_SOURCE_FLAGS=%GOOGLE_BREAKPAD_COMMON_SOURCE_FLAGS% -e windows -e mac -e solaris
set XTREEMFS_PATH=%CD%\..\..
set XTREEMFS_CLIENT_PATH=%CD%
set YIELDFS_PATH=%XTREEMFS_CLIENT_PATH%\share\yieldfs
set YIELD_PATH=%XTREEMFS_PATH%\..\yield

set DEPEND_GOOGLE_BREAKPAD_FLAGS=-I %GOOGLE_BREAKPAD_PATH%\src -c %XTREEMFS_CLIENT_PATH%\proj\google-breakpad\google-breakpad.SConscript
set DEPEND_YIELD_INCLUDE_FLAGS=-I %YIELDFS_PATH%\share\yield\include
set DEPEND_YIELD_LIB_FLAGS=--lw libeay32.lib --lw ssleay32.lib --lwS libeay32.lib --lwS ssleay32.lib --lu ssl
set DEPEND_YIELDFS_INCLUDE_FLAGS=-I %YIELDFS_PATH%\include %DEPEND_YIELD_INCLUDE_FLAGS%
set DEPEND_YIELDFS_LIB_FLAGS=--lu fuse %DEPEND_YIELD_LIB_FLAGS%
set DEPEND_XTREEMFS_CLIENT_FLAGS=-I %XTREEMFS_CLIENT_PATH%\include -L %XTREEMFS_CLIENT_PATH%\lib -l xtreemfs-client_d.lib -c %XTREEMFS_CLIENT_PATH%\proj\org\xtreemfs\client\xtreemfs-client-lib.SConscript %DEPEND_YIELDFS_INCLUDE_FLAGS%


REM Generate source
REM Don't include share\* in the scan here
python %YIELD_PATH%\bin\generate_yield_cpp.py -i %XTREEMFS_PATH%\src\interfaces\org\xtreemfs\interfaces -o %XTREEMFS_CLIENT_PATH%\include\org\xtreemfs\interfaces
python %YIELD_PATH%\bin\generate_test_main_cpp.py
python %YIELD_PATH%\bin\format_src.py -n "XtreemFS" -l "GPLv2" -s %XTREEMFS_CLIENT_PATH%\include -s %XTREEMFS_CLIENT_PATH%\proj -s %XTREEMFS_CLIENT_PATH%\src
REM python %XTREEMFS_PATH%\bin\generate_xtreemfs_fuzzer_cpp.py -i %XTREEMFS_PATH%\interfaces -o %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client


REM Generate project files 
cd %XTREEMFS_CLIENT_PATH%\proj\org\xtreemfs\client
REM Library projects
python %YIELD_PATH%\bin\generate_proj.py -n xtreemfs-client-lib -t lib -s %XTREEMFS_CLIENT_PATH%\src\org -s %YIELDFS_PATH%\src -s %YIELDFS_PATH%\share\yield\src -e "xtfs_*.cpp" -e "xos*" -s %XTREEMFS_CLIENT_PATH%\include\org\xtreemfs -s %XTREEMFS_PATH%\src\interfaces\org\xtreemfs -I %XTREEMFS_CLIENT_PATH%\include -o %XTREEMFS_CLIENT_PATH%\lib\xtreemfs-client %DEPEND_YIELDFS_INCLUDE_FLAGS% %DEPEND_YIELDFS_LIB_FLAGS%

REM Binary projects
python %YIELD_PATH%\bin\generate_proj.py -n xtfs_fuzz -t exe -s %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\xtfs_fuzz.cpp -s %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\*_fuzzer.h -o %XTREEMFS_CLIENT_PATH%\bin %DEPEND_XTREEMFS_CLIENT_FLAGS%
python %YIELD_PATH%\bin\generate_proj.py -n xtfs_lsvol -t exe -s %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\xtfs_lsvol.cpp -o %XTREEMFS_CLIENT_PATH%\bin %DEPEND_XTREEMFS_CLIENT_FLAGS% %DEPEND_GOOGLE_BREAKPAD_FLAGS%
python %YIELD_PATH%\bin\generate_proj.py -n xtfs_mount -t exe -s %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\xtfs_mount.cpp -s %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\main.h -o %XTREEMFS_CLIENT_PATH%\bin %DEPEND_XTREEMFS_CLIENT_FLAGS% %DEPEND_GOOGLE_BREAKPAD_FLAGS%
python %YIELD_PATH%\bin\generate_proj.py -n xtfs_mkvol -t exe -s %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\xtfs_mkvol.cpp -o %XTREEMFS_CLIENT_PATH%\bin %DEPEND_XTREEMFS_CLIENT_FLAGS% %DEPEND_GOOGLE_BREAKPAD_FLAGS%
python %YIELD_PATH%\bin\generate_proj.py -n xtfs_rmvol -t exe -s %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\xtfs_rmvol.cpp -o %XTREEMFS_CLIENT_PATH%\bin %DEPEND_XTREEMFS_CLIENT_FLAGS% %DEPEND_GOOGLE_BREAKPAD_FLAGS%
python %YIELD_PATH%\bin\generate_proj.py -n xtfs_send -t exe -s %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\xtfs_send.cpp -o %XTREEMFS_CLIENT_PATH%\bin %DEPEND_XTREEMFS_CLIENT_FLAGS% %DEPEND_GOOGLE_BREAKPAD_FLAGS%
python %YIELD_PATH%\bin\generate_proj.py -n xtfs_stat -t exe -s %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\xtfs_stat.cpp -o %XTREEMFS_CLIENT_PATH%\bin %DEPEND_XTREEMFS_CLIENT_FLAGS% %DEPEND_GOOGLE_BREAKPAD_FLAGS%

REM Policy projects
python %YIELD_PATH%\bin\generate_proj.py -n xos_ams_flog -t dll -s %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\xos_ams_flog.c -I %XTREEMFS_CLIENT_PATH%\include --lu xos_ams -o %XTREEMFS_CLIENT_PATH%\lib


REM Google Breakpad
cd %XTREEMFS_CLIENT_PATH%\proj\google-breakpad
python %YIELD_PATH%\bin\generate_vcproj.py -n google-breakpad -t lib %GOOGLE_BREAKPAD_WINDOWS_SOURCE_FLAGS% -o %XTREEMFS_CLIENT_PATH%\lib
python %YIELD_PATH%\bin\generate_SConscript.py -n google-breakpad
python %YIELD_PATH%\bin\generate_SConscript.py -n google-breakpad_linux -t lib %GOOGLE_BREAKPAD_LINUX_SOURCE_FLAGS% -o %XTREEMFS_CLIENT_PATH%\lib\google-breakpad
python %YIELD_PATH%\bin\generate_SConscript.py -n google-breakpad_windows -t lib %GOOGLE_BREAKPAD_WINDOWS_SOURCE_FLAGS% -o %XTREEMFS_CLIENT_PATH%\lib\google-breakpad

 
cd %XTREEMFS_CLIENT_PATH%
