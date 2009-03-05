package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.OSDInterface.*;
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         

public class keep_file_openRequest implements Request
{
    public keep_file_openRequest() { file_id = ""; credentials = new org.xtreemfs.interfaces.FileCredentials(); }
    public keep_file_openRequest( String file_id, FileCredentials credentials ) { this.file_id = file_id; this.credentials = credentials; }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public FileCredentials getCredentials() { return credentials; }
    public void setCredentials( FileCredentials credentials ) { this.credentials = credentials; }

    // Object
    public String toString()
    {
        return "keep_file_openRequest( " + "\"" + file_id + "\"" + ", " + credentials.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::OSDInterface::keep_file_openRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(file_id,writer); }
        credentials.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        credentials = new org.xtreemfs.interfaces.FileCredentials(); credentials.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        my_size += credentials.calculateSize();
        return my_size;
    }

    private String file_id;
    private FileCredentials credentials;
    

    // Request
    public int getInterfaceVersion() { return 3; }    
    public int getOperationNumber() { return 5; }
    public Response createDefaultResponse() { return new keep_file_openResponse(); }

}

