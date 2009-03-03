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


         

public class internal_get_gmaxRequest implements Request
{
    public internal_get_gmaxRequest() { file_id = ""; credentials = new org.xtreemfs.interfaces.FileCredentials(); }
    public internal_get_gmaxRequest( String file_id, FileCredentials credentials ) { this.file_id = file_id; this.credentials = credentials; }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public FileCredentials getCredentials() { return credentials; }
    public void setCredentials( FileCredentials credentials ) { this.credentials = credentials; }

    // Object
    public String toString()
    {
        return "internal_get_gmaxRequest( " + "\"" + file_id + "\"" + ", " + credentials.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::OSDInterface::internal_get_gmaxRequest"; }    
    
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
        my_size += 4 + ( file_id.length() + 4 - ( file_id.length() % 4 ) );
        my_size += credentials.calculateSize();
        return my_size;
    }

    private String file_id;
    private FileCredentials credentials;
    

    // Request
    public int getInterfaceVersion() { return 3; }    
    public int getOperationNumber() { return 100; }
    public Response createDefaultResponse() { return new internal_get_gmaxResponse(); }

}

