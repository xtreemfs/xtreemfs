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


         

public class unlinkResponse implements Response
{
    public unlinkResponse() { credentials = new org.xtreemfs.interfaces.FileCredentialsSet(); }
    public unlinkResponse( FileCredentialsSet credentials ) { this.credentials = credentials; }

    public FileCredentialsSet getCredentials() { return credentials; }
    public void setCredentials( FileCredentialsSet credentials ) { this.credentials = credentials; }

    // Object
    public String toString()
    {
        return "unlinkResponse( " + credentials.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::unlinkResponse"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        credentials.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        credentials = new org.xtreemfs.interfaces.FileCredentialsSet(); credentials.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += credentials.calculateSize();
        return my_size;
    }

    private FileCredentialsSet credentials;
    

    // Response
    public int getInterfaceVersion() { return 2; }
    public int getOperationNumber() { return 21; }    

}

