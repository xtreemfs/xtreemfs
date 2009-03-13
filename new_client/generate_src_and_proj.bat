@ECHO OFF

REM set YIDL_PATH=%CD%\..\share\yidl
set YIDL_PATH=%CD%\..\..\yidl
set YIELD_PATH=%CD%\share\yield
set YIELDFS_PATH=%CD%\share\yieldfs

set DEPEND_YIELD_INCLUDE_FLAGS=--Iu %YIELD_PATH%\yield\include --Iw %YIELD_PATH%\yield_platform\include --Iw %YIELD_PATH%\yield_arch\include --Iw %YIELD_PATH%\yield_ipc\include
set DEPEND_YIELD_LIB_FLAGS=-c %YIELD_PATH%\yield\yield.SConscript --lu yield.lib --lwS yield.lib --Lu %YIELD_PATH%\yield\lib --LwS %YIELD_PATH%\yield\lib
set DEPEND_YIELDFS_INCLUDE_FLAGS=-I %YIELDFS_PATH%\include
set DEPEND_YIELDFS_LIB_FLAGS=-c %YIELDFS_PATH%\yieldfs.SConscript --lu yieldfs.lib --lwS yieldfs.lib --Lu %YIELDFS_PATH%\lib --LwS %YIELDFS_PATH%\lib
set DEPEND_XTREEMFS_CLIENT_FLAGS=-I ..\include -L ..\lib -c xtreemfs-client-lib.SConscript --lu xtreemfs-client.lib --lwS xtreemfs-client.lib %DEPEND_YIELD_INCLUDE_FLAGS% %DEPEND_YIELDFS_INCLUDE_FLAGS%
REM set PYTHONPATH=%YIDL_PATH%\src


REM Generate source from IDL interfaces
REM Don't include share\yield in the scan here
python %YIDL_PATH%\bin\generate_yield_cpp.py -i %CD%\..\interfaces -o include\org\xtreemfs\interfaces --with-registerSerializableFactories
python %YIDL_PATH%\bin\generate_yield_cpp.py -i include -o include --with-registerSerializableFactories
python %YIDL_PATH%\bin\generate_yield_cpp.py -i src -o src --with-registerSerializableFactories
python %CD%\bin\generate_xtreemfs_fuzzer_cpp.py -i %CD%\..\interfaces -o src\org\xtreemfs\client\bin


REM Generate project files
cd proj
python %YIDL_PATH%\bin\generate_proj.py -n xtreemfs-client-lib -t lib -s ..\src\org\xtreemfs\client\lib -s ..\include\org\xtreemfs -s ..\..\interfaces -I ..\include -o ..\lib\xtreemfs-client %DEPEND_YIELD_INCLUDE_FLAGS% %DEPEND_YIELD_LIB_FLAGS% %DEPEND_YIELDFS_INCLUDE_FLAGS% %DEPEND_YIELDFS_LIB_FLAGS%
python %YIDL_PATH%\bin\generate_proj.py -n xtreemfs-client-test -t exe -s ..\src\org\xtreemfs\client\bin\xtreemfs-client-test.cpp -o ..\bin %DEPEND_XTREEMFS_CLIENT_FLAGS%
python %YIDL_PATH%\bin\generate_proj.py -n xtfs_mount -t exe -s ..\src\org\xtreemfs\client\bin\xtfs_mount.cpp -o ..\bin %DEPEND_XTREEMFS_CLIENT_FLAGS%
python %YIDL_PATH%\bin\generate_proj.py -n xtfs_mkvol -t exe -s ..\src\org\xtreemfs\client\bin\xtfs_mkvol.cpp -o ..\bin %DEPEND_XTREEMFS_CLIENT_FLAGS%
python %YIDL_PATH%\bin\generate_proj.py -n xtfs_lsvol -t exe -s ..\src\org\xtreemfs\client\bin\xtfs_lsvol.cpp -o ..\bin %DEPEND_XTREEMFS_CLIENT_FLAGS%
python %YIDL_PATH%\bin\generate_proj.py -n xtfs_rmvol -t exe -s ..\src\org\xtreemfs\client\bin\xtfs_rmvol.cpp -o ..\bin %DEPEND_XTREEMFS_CLIENT_FLAGS%
python %YIDL_PATH%\bin\generate_proj.py -n xtfs_send -t exe -s ..\src\org\xtreemfs\client\bin\xtfs_send.cpp -o ..\bin %DEPEND_XTREEMFS_CLIENT_FLAGS%
python %YIDL_PATH%\bin\generate_proj.py -n xtfs_fuzz -t exe -s ..\src\org\xtreemfs\client\bin\xtfs_fuzz.cpp -s ..\src\org\xtreemfs\client\bin\*_fuzzer.h -o ..\bin %DEPEND_XTREEMFS_CLIENT_FLAGS%


cd ..
