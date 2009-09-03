package org.xtreemfs.interfaces;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class Lock extends Struct
{
    public static final int TAG = 2009082672;
    
    public Lock() {  }
    public Lock( String client_uuid, int client_pid, long offset, long length ) { this.client_uuid = client_uuid; this.client_pid = client_pid; this.offset = offset; this.length = length; }

    public String getClient_uuid() { return client_uuid; }
    public void setClient_uuid( String client_uuid ) { this.client_uuid = client_uuid; }
    public int getClient_pid() { return client_pid; }
    public void setClient_pid( int client_pid ) { this.client_pid = client_pid; }
    public long getOffset() { return offset; }
    public void setOffset( long offset ) { this.offset = offset; }
    public long getLength() { return length; }
    public void setLength( long length ) { this.length = length; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082672;    

    // yidl.Object
    public int getTag() { return 2009082672; }
    public String getTypeName() { return "org::xtreemfs::interfaces::Lock"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += client_uuid != null ? ( ( client_uuid.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( client_uuid.getBytes().length + Integer.SIZE/8 ) : ( client_uuid.getBytes().length + Integer.SIZE/8 + 4 - ( client_uuid.getBytes().length + Integer.SIZE/8 ) % 4 ) : 0;
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "client_uuid", client_uuid );
        marshaller.writeUint32( "client_pid", client_pid );
        marshaller.writeUint64( "offset", offset );
        marshaller.writeUint64( "length", length );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        client_uuid = unmarshaller.readString( "client_uuid" );
        client_pid = unmarshaller.readUint32( "client_pid" );
        offset = unmarshaller.readUint64( "offset" );
        length = unmarshaller.readUint64( "length" );    
    }
        
    

    private String client_uuid;
    private int client_pid;
    private long offset;
    private long length;    

}

