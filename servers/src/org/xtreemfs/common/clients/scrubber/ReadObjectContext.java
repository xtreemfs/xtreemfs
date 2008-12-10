package org.xtreemfs.common.clients.scrubber;

/**
 * Holds information to identify an object and the connection used for 
 * the read request after receiving a response from the osd.
 */
public class ReadObjectContext {
    public long readStart;
    public int connectionNo;
    public int objectNo;
    public ScrubbedFile file;
    
    ReadObjectContext(int connectionNo, ScrubbedFile file, int objectNo,
            long readStart) {
        this.readStart = readStart;
        this.connectionNo = connectionNo;
        this.file = file;
        this.objectNo = objectNo;
    }
}