package org.xtreemfs.common.clients.scrubber;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
