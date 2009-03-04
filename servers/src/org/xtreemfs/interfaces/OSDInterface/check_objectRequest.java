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


         

public class check_objectRequest implements Request
{
    public check_objectRequest() { file_id = ""; credentials = new org.xtreemfs.interfaces.FileCredentials(); object_number = 0; object_version = 0; }
    public check_objectRequest( String file_id, FileCredentials credentials, long object_number, long object_version ) { this.file_id = file_id; this.credentials = credentials; this.object_number = object_number; this.object_version = object_version; }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public FileCredentials getCredentials() { return credentials; }
    public void setCredentials( FileCredentials credentials ) { this.credentials = credentials; }
    public long getObject_number() { return object_number; }
    public void setObject_number( long object_number ) { this.object_number = object_number; }
    public long getObject_version() { return object_version; }
    public void setObject_version( long object_version ) { this.object_version = object_version; }

    // Object
    public String toString()
    {
        return "check_objectRequest( " + "\"" + file_id + "\"" + ", " + credentials.toString() + ", " + Long.toString( object_number ) + ", " + Long.toString( object_version ) + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::OSDInterface::check_objectRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(file_id,writer); }
        credentials.serialize( writer );
        writer.putLong( object_number );
        writer.putLong( object_version );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        credentials = new org.xtreemfs.interfaces.FileCredentials(); credentials.deserialize( buf );
        object_number = buf.getLong();
        object_version = buf.getLong();    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        my_size += credentials.calculateSize();
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }

    private String file_id;
    private FileCredentials credentials;
    private long object_number;
    private long object_version;
    

    // Request
    public int getInterfaceVersion() { return 3; }    
    public int getOperationNumber() { return 103; }
    public Response createDefaultResponse() { return new check_objectResponse(); }

}

