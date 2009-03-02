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


         

public class writeRequest implements Request
{
    public writeRequest() { file_id = ""; credentials = new org.xtreemfs.interfaces.FileCredentials(); object_number = 0; object_version = 0; offset = 0; length = 0; lease_timeout = 0; data = new org.xtreemfs.interfaces.ObjectData(); }
    public writeRequest( String file_id, FileCredentials credentials, long object_number, long object_version, long offset, long length, long lease_timeout, ObjectData data ) { this.file_id = file_id; this.credentials = credentials; this.object_number = object_number; this.object_version = object_version; this.offset = offset; this.length = length; this.lease_timeout = lease_timeout; this.data = data; }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public FileCredentials getCredentials() { return credentials; }
    public void setCredentials( FileCredentials credentials ) { this.credentials = credentials; }
    public long getObject_number() { return object_number; }
    public void setObject_number( long object_number ) { this.object_number = object_number; }
    public long getObject_version() { return object_version; }
    public void setObject_version( long object_version ) { this.object_version = object_version; }
    public long getOffset() { return offset; }
    public void setOffset( long offset ) { this.offset = offset; }
    public long getLength() { return length; }
    public void setLength( long length ) { this.length = length; }
    public long getLease_timeout() { return lease_timeout; }
    public void setLease_timeout( long lease_timeout ) { this.lease_timeout = lease_timeout; }
    public ObjectData getData() { return data; }
    public void setData( ObjectData data ) { this.data = data; }

    // Object
    public String toString()
    {
        return "writeRequest( " + "\"" + file_id + "\"" + ", " + credentials.toString() + ", " + Long.toString( object_number ) + ", " + Long.toString( object_version ) + ", " + Long.toString( offset ) + ", " + Long.toString( length ) + ", " + Long.toString( lease_timeout ) + ", " + data.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::OSDInterface::writeRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(file_id,writer); }
        credentials.serialize( writer );
        writer.putLong( object_number );
        writer.putLong( object_version );
        writer.putLong( offset );
        writer.putLong( length );
        writer.putLong( lease_timeout );
        data.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        credentials = new org.xtreemfs.interfaces.FileCredentials(); credentials.deserialize( buf );
        object_number = buf.getLong();
        object_version = buf.getLong();
        offset = buf.getLong();
        length = buf.getLong();
        lease_timeout = buf.getLong();
        data = new org.xtreemfs.interfaces.ObjectData(); data.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4 + ( file_id.length() + 4 - ( file_id.length() % 4 ) );
        my_size += credentials.calculateSize();
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += data.calculateSize();
        return my_size;
    }

    private String file_id;
    private FileCredentials credentials;
    private long object_number;
    private long object_version;
    private long offset;
    private long length;
    private long lease_timeout;
    private ObjectData data;
    

    // Request
    public int getInterfaceVersion() { return 3; }    
    public int getOperationNumber() { return 4; }
    public Response createDefaultResponse() { return new writeResponse(); }

}

