@echo off
echo setting the path to the dokanctl, which is needed for unmounting a xtreemfs volume ...

cd /D %ProgramFiles%

dir /b /s dokanctl.exe > temp
set /p DOKANCTL= < temp
del temp

copy /V /Y /B %DOKANCTL% %SystemRoot%