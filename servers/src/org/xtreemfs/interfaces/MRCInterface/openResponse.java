package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.MRCInterface.*;
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         

public class openResponse implements Response
{
    public openResponse() { credentials = new org.xtreemfs.interfaces.FileCredentials(); }
    public openResponse( FileCredentials credentials ) { this.credentials = credentials; }

    public FileCredentials getCredentials() { return credentials; }
    public void setCredentials( FileCredentials credentials ) { this.credentials = credentials; }

    // Object
    public String toString()
    {
        return "openResponse( " + credentials.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::openResponse"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        credentials.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        credentials = new org.xtreemfs.interfaces.FileCredentials(); credentials.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += credentials.calculateSize();
        return my_size;
    }

    private FileCredentials credentials;
    

    // Response
    public int getInterfaceVersion() { return 2; }
    public int getOperationNumber() { return 11; }    

}

