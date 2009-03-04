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


         

public class internal_truncateRequest implements Request
{
    public internal_truncateRequest() { file_id = ""; credentials = new org.xtreemfs.interfaces.FileCredentials(); new_file_size = 0; }
    public internal_truncateRequest( String file_id, FileCredentials credentials, long new_file_size ) { this.file_id = file_id; this.credentials = credentials; this.new_file_size = new_file_size; }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public FileCredentials getCredentials() { return credentials; }
    public void setCredentials( FileCredentials credentials ) { this.credentials = credentials; }
    public long getNew_file_size() { return new_file_size; }
    public void setNew_file_size( long new_file_size ) { this.new_file_size = new_file_size; }

    // Object
    public String toString()
    {
        return "internal_truncateRequest( " + "\"" + file_id + "\"" + ", " + credentials.toString() + ", " + Long.toString( new_file_size ) + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::OSDInterface::internal_truncateRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(file_id,writer); }
        credentials.serialize( writer );
        writer.putLong( new_file_size );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        credentials = new org.xtreemfs.interfaces.FileCredentials(); credentials.deserialize( buf );
        new_file_size = buf.getLong();    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4 + ( file_id.length() + 4 - ( file_id.length() % 4 ) );
        my_size += credentials.calculateSize();
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }

    private String file_id;
    private FileCredentials credentials;
    private long new_file_size;
    

    // Request
    public int getInterfaceVersion() { return 3; }    
    public int getOperationNumber() { return 101; }
    public Response createDefaultResponse() { return new internal_truncateResponse(); }

}

