@echo off
rem -----------------------------------------------------------------------
rem 
rem DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
rem 
rem Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
rem 
rem The contents of this file are subject to the terms of either the GNU General
rem Public License Version 2 only ("GPL") or the Common Development and
rem Distribution License("CDDL")(collectively, the "License"). You may not use
rem this file except in compliance with the License. You can obtain a copy of the
rem License at http://opendmk.dev.java.net/legal_notices/licenses.txt or in the 
rem LEGAL_NOTICES folder that accompanied this code. See the License for the 
rem specific language governing permissions and limitations under the License.
rem 
rem When distributing the software, include this License Header Notice in each
rem file and include the License file found at
rem     http://opendmk.dev.java.net/legal_notices/licenses.txt
rem or in the LEGAL_NOTICES folder that accompanied this code.
rem Sun designates this particular file as subject to the "Classpath" exception
rem as provided by Sun in the GPL Version 2 section of the License file that
rem accompanied this code.
rem 
rem If applicable, add the following below the License Header, with the fields
rem enclosed by brackets [] replaced by your own identifying information:
rem 
rem       "Portions Copyrighted [year] [name of copyright owner]"
rem 
rem Contributor(s):
rem 
rem If you wish your version of this file to be governed by only the CDDL or
rem only the GPL Version 2, indicate your decision by adding
rem 
rem       "[Contributor] elects to include this software in this distribution
rem        under the [CDDL or GPL Version 2] license."
rem 
rem If you don't indicate a single choice of license, a recipient has the option
rem to distribute your version of this file under either the CDDL or the GPL
rem Version 2, or to extend the choice of license to its licensees as provided
rem above. However, if you add GPL Version 2 code and therefore, elected the
rem GPL Version 2 license, then the option applies only if the new code is made
rem subject to such option by the copyright holder.
rem 
rem -----------------------------------------------------------------------

rem *************************************************************
rem *** JDMK_HOME points to the root of the Java DMK installation
rem *************************************************************
set JDMK_HOME=C:\Program Files\SUNWjdmk\5.1
set CHECK=%JDMK_HOME%UNDEFINED
if "%CHECK%" EQU "UNDEFINED" goto jdmk_home_error

rem *******************************************************************
rem *** Begins localization of environment variables in the batch file
rem *******************************************************************
setlocal

set PATH=%JDMK_HOME%\bin;%PATH%
set CLASSPATH=%JDMK_HOME%\lib\jdmkrt.jar;%JDMK_HOME%\lib\jdmktk.jar;%CLASSPATH%

rem **************************************
rem *** Lauch the Java application
rem **************************************
java com.sun.jdmk.tools.MibGen %*

rem ***************************************************************
rem *** Ends localization of environment changes in the batch file
rem ***************************************************************
endlocal

goto end

:jdmk_home_error
echo Error: The JDMK_HOME variable does not point to
echo Error: the Java DMK installation. Please edit your
echo Error: startup batch file.

:end
