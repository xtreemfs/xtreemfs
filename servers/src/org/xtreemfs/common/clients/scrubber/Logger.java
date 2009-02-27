/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Nele Andersen (ZIB)
 */
package org.xtreemfs.common.clients.scrubber;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

public class Logger extends PrintStream {

    FileWriter writer;
    public Logger(String logFileName) {
        super(System.err);
        
        if(logFileName != null) {
            File logFile = new File(logFileName); 
            Date date = new Date();
            try {
                writer = new FileWriter(logFile);
                writer.write("Date: " + date.toString() + "\n");
            } catch (IOException e) {
                System.err.println("Could not create log file.");
                e.printStackTrace();
            }
        }
    }
    
    public void logError(String message) {
        super.println(message);
        
        try {
            if(writer != null)
                writer.write(message + "\n");
        } catch (IOException e) {
            System.err.println("Could not write to log!");
            e.printStackTrace();
        }
    }
    
    public void closeFileWriter(){
        try {
            if(writer != null)
                writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
