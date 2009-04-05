@ECHO OFF

set XTREEMFS_PATH=%CD%\..
set XTREEMFS_CLIENT_PATH=%CD%
set YIELDFS_PATH=%XTREEMFS_CLIENT_PATH%\share\yieldfs
set YIELD_PATH=%YIELDFS_PATH%\share\yield

set DEPEND_YIELD_INCLUDE_FLAGS=-I %YIELD_PATH%\include -D YIELD_HAVE_OPENSSL
set DEPEND_YIELD_LIB_FLAGS=--lw libeay32.lib --lw ssleay32.lib --lwS libeay32.lib --lwS ssleay32.lib --lu ssl
set DEPEND_YIELDFS_INCLUDE_FLAGS=-I %YIELDFS_PATH%\include %DEPEND_YIELD_INCLUDE_FLAGS%
set DEPEND_YIELDFS_LIB_FLAGS=-L %YIELDFS_PATH%\lib -l yieldfs_d.lib -c %YIELDFS_PATH%\proj\yieldfs\yieldfs.SConscript %DEPEND_YIELD_LIB_FLAGS%
set DEPEND_XTREEMFS_CLIENT_FLAGS=-I %XTREEMFS_CLIENT_PATH%\include -L %XTREEMFS_CLIENT_PATH%\lib -l xtreemfs-client_d.lib -c %XTREEMFS_CLIENT_PATH%\proj\org\xtreemfs\client\lib\xtreemfs-client-lib.SConscript %DEPEND_YIELDFS_INCLUDE_FLAGS%


REM Generate source
REM Don't include share\* in the scan here
python %YIELD_PATH%\bin\generate_yield_cpp.py -i %XTREEMFS_PATH%\interfaces -o %XTREEMFS_CLIENT_PATH%\include\org\xtreemfs\interfaces --with-registerSerializableFactories
python %YIELD_PATH%\bin\generate_yield_cpp.py -i %XTREEMFS_CLIENT_PATH%\include -o %XTREEMFS_CLIENT_PATH%\include --with-registerSerializableFactories
python %YIELD_PATH%\bin\generate_yield_cpp.py -i %XTREEMFS_CLIENT_PATH%\src -o %XTREEMFS_CLIENT_PATH%\src --with-registerSerializableFactories
python %YIELD_PATH%\bin\generate_test_main_cpp.py
python %YIELD_PATH%\bin\format_src.py -n "XtreemFS" -l "GPLv2" -s %XTREEMFS_CLIENT_PATH%\include -s %XTREEMFS_CLIENT_PATH%\proj -s %XTREEMFS_CLIENT_PATH%\src
python %CD%\bin\generate_xtreemfs_fuzzer_cpp.py -i %XTREEMFS_PATH%\interfaces -o %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\bin


REM Generate project files 
REM Library projects
cd %XTREEMFS_CLIENT_PATH%\proj\org\xtreemfs\client\lib
python %YIELD_PATH%\bin\generate_proj.py -n xtreemfs-client-lib -t lib -s %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\lib -e "*_test.cpp" -s %XTREEMFS_CLIENT_PATH%\include\org\xtreemfs -s %XTREEMFS_PATH%\interfaces -I %XTREEMFS_CLIENT_PATH%\include -o %XTREEMFS_CLIENT_PATH%\lib\xtreemfs-client %DEPEND_YIELDFS_INCLUDE_FLAGS% %DEPEND_YIELDFS_LIB_FLAGS%
python %YIELD_PATH%\bin\generate_proj.py -n xtreemfs-client-lib_test -t exe -s %XTREEMFS_CLIENT_PATH%\proj\org\xtreemfs\client\lib\org_xtreemfs_client_lib_test_main.cpp -s "%XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\lib\*_test.cpp" -c xtreemfs-client-lib.SConscript -o %XTREEMFS_CLIENT_PATH%\bin %DEPEND_XTREEMFS_CLIENT_FLAGS%

REM Binary projects
cd %XTREEMFS_CLIENT_PATH%\proj\org\xtreemfs\client\bin
python %YIELD_PATH%\bin\generate_proj.py -n xtfs_mount -t exe -s %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\bin\xtfs_mount.cpp -s %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\bin\options.h -o %XTREEMFS_CLIENT_PATH%\bin %DEPEND_XTREEMFS_CLIENT_FLAGS%
python %YIELD_PATH%\bin\generate_proj.py -n xtfs_mkvol -t exe -s %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\bin\xtfs_mkvol.cpp -o %XTREEMFS_CLIENT_PATH%\bin %DEPEND_XTREEMFS_CLIENT_FLAGS%
python %YIELD_PATH%\bin\generate_proj.py -n xtfs_lsvol -t exe -s %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\bin\xtfs_lsvol.cpp -o %XTREEMFS_CLIENT_PATH%\bin %DEPEND_XTREEMFS_CLIENT_FLAGS%
python %YIELD_PATH%\bin\generate_proj.py -n xtfs_rmvol -t exe -s %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\bin\xtfs_rmvol.cpp -o %XTREEMFS_CLIENT_PATH%\bin %DEPEND_XTREEMFS_CLIENT_FLAGS%
python %YIELD_PATH%\bin\generate_proj.py -n xtfs_send -t exe -s %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\bin\xtfs_send.cpp -o %XTREEMFS_CLIENT_PATH%\bin %DEPEND_XTREEMFS_CLIENT_FLAGS%
python %YIELD_PATH%\bin\generate_proj.py -n xtfs_fuzz -t exe -s %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\bin\xtfs_fuzz.cpp -s %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\bin\*_fuzzer.h -o %XTREEMFS_CLIENT_PATH%\bin %DEPEND_XTREEMFS_CLIENT_FLAGS%

REM Policy projects
cd %XTREEMFS_CLIENT_PATH%\proj\org\xtreemfs\client\policies
python %YIELD_PATH%\bin\generate_proj.py -n xos_ams_flog -t dll -s %XTREEMFS_CLIENT_PATH%\src\org\xtreemfs\client\policies\xos_ams_flog.c -I %XTREEMFS_CLIENT_PATH%\include --lu xos_ams -o %XTREEMFS_CLIENT_PATH%\lib

 
cd %XTREEMFS_CLIENT_PATH%
