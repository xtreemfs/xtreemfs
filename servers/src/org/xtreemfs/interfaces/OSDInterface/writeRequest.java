package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class writeRequest implements org.xtreemfs.interfaces.utils.Request
{
    public writeRequest() { file_id = ""; credentials = new FileCredentials(); object_number = 0; object_version = 0; offset = 0; lease_timeout = 0; object_data = new ObjectData(); }
    public writeRequest( String file_id, FileCredentials credentials, long object_number, long object_version, int offset, long lease_timeout, ObjectData object_data ) { this.file_id = file_id; this.credentials = credentials; this.object_number = object_number; this.object_version = object_version; this.offset = offset; this.lease_timeout = lease_timeout; this.object_data = object_data; }
    public writeRequest( Object from_hash_map ) { file_id = ""; credentials = new FileCredentials(); object_number = 0; object_version = 0; offset = 0; lease_timeout = 0; object_data = new ObjectData(); this.deserialize( from_hash_map ); }
    public writeRequest( Object[] from_array ) { file_id = ""; credentials = new FileCredentials(); object_number = 0; object_version = 0; offset = 0; lease_timeout = 0; object_data = new ObjectData();this.deserialize( from_array ); }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public FileCredentials getCredentials() { return credentials; }
    public void setCredentials( FileCredentials credentials ) { this.credentials = credentials; }
    public long getObject_number() { return object_number; }
    public void setObject_number( long object_number ) { this.object_number = object_number; }
    public long getObject_version() { return object_version; }
    public void setObject_version( long object_version ) { this.object_version = object_version; }
    public int getOffset() { return offset; }
    public void setOffset( int offset ) { this.offset = offset; }
    public long getLease_timeout() { return lease_timeout; }
    public void setLease_timeout( long lease_timeout ) { this.lease_timeout = lease_timeout; }
    public ObjectData getObject_data() { return object_data; }
    public void setObject_data( ObjectData object_data ) { this.object_data = object_data; }

    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::writeRequest"; }    
    public long getTypeId() { return 4; }

    public String toString()
    {
        return "writeRequest( " + "\"" + file_id + "\"" + ", " + credentials.toString() + ", " + Long.toString( object_number ) + ", " + Long.toString( object_version ) + ", " + Integer.toString( offset ) + ", " + Long.toString( lease_timeout ) + ", " + object_data.toString() + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.file_id = ( String )from_hash_map.get( "file_id" );
        this.credentials.deserialize( from_hash_map.get( "credentials" ) );
        this.object_number = ( ( Long )from_hash_map.get( "object_number" ) ).longValue();
        this.object_version = ( ( Long )from_hash_map.get( "object_version" ) ).longValue();
        this.offset = ( ( Integer )from_hash_map.get( "offset" ) ).intValue();
        this.lease_timeout = ( ( Long )from_hash_map.get( "lease_timeout" ) ).longValue();
        this.object_data.deserialize( from_hash_map.get( "object_data" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.file_id = ( String )from_array[0];
        this.credentials.deserialize( from_array[1] );
        this.object_number = ( ( Long )from_array[2] ).longValue();
        this.object_version = ( ( Long )from_array[3] ).longValue();
        this.offset = ( ( Integer )from_array[4] ).intValue();
        this.lease_timeout = ( ( Long )from_array[5] ).longValue();
        this.object_data.deserialize( from_array[6] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        credentials = new FileCredentials(); credentials.deserialize( buf );
        object_number = buf.getLong();
        object_version = buf.getLong();
        offset = buf.getInt();
        lease_timeout = buf.getLong();
        object_data = new ObjectData(); object_data.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "file_id", file_id );
        to_hash_map.put( "credentials", credentials.serialize() );
        to_hash_map.put( "object_number", new Long( object_number ) );
        to_hash_map.put( "object_version", new Long( object_version ) );
        to_hash_map.put( "offset", new Integer( offset ) );
        to_hash_map.put( "lease_timeout", new Long( lease_timeout ) );
        to_hash_map.put( "object_data", object_data.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( file_id, writer );
        credentials.serialize( writer );
        writer.putLong( object_number );
        writer.putLong( object_version );
        writer.putInt( offset );
        writer.putLong( lease_timeout );
        object_data.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        my_size += credentials.calculateSize();
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += object_data.calculateSize();
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 4; }
    public Response createDefaultResponse() { return new writeResponse(); }


    private String file_id;
    private FileCredentials credentials;
    private long object_number;
    private long object_version;
    private int offset;
    private long lease_timeout;
    private ObjectData object_data;

}

