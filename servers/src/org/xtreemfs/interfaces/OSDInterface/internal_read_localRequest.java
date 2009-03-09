package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class internal_read_localRequest implements org.xtreemfs.interfaces.utils.Request
{
    public internal_read_localRequest() { file_id = ""; credentials = new FileCredentials(); object_number = 0; object_version = 0; offset = 0; length = 0; }
    public internal_read_localRequest( String file_id, FileCredentials credentials, long object_number, long object_version, long offset, long length ) { this.file_id = file_id; this.credentials = credentials; this.object_number = object_number; this.object_version = object_version; this.offset = offset; this.length = length; }
    public internal_read_localRequest( Object from_hash_map ) { file_id = ""; credentials = new FileCredentials(); object_number = 0; object_version = 0; offset = 0; length = 0; this.deserialize( from_hash_map ); }
    public internal_read_localRequest( Object[] from_array ) { file_id = ""; credentials = new FileCredentials(); object_number = 0; object_version = 0; offset = 0; length = 0;this.deserialize( from_array ); }

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

    // Serializable
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::internal_read_localRequest"; }    
    public long getTypeId() { return 102; }

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
        this.offset = ( ( Long )from_hash_map.get( "offset" ) ).longValue();
        this.length = ( ( Long )from_hash_map.get( "length" ) ).longValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.file_id = ( String )from_array[0];
        this.credentials.deserialize( from_array[1] );
        this.object_number = ( ( Long )from_array[2] ).longValue();
        this.object_version = ( ( Long )from_array[3] ).longValue();
        this.offset = ( ( Long )from_array[4] ).longValue();
        this.length = ( ( Long )from_array[5] ).longValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        credentials = new FileCredentials(); credentials.deserialize( buf );
        object_number = buf.getLong();
        object_version = buf.getLong();
        offset = buf.getLong();
        length = buf.getLong();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "file_id", file_id );
        to_hash_map.put( "credentials", credentials.serialize() );
        to_hash_map.put( "object_number", new Long( object_number ) );
        to_hash_map.put( "object_version", new Long( object_version ) );
        to_hash_map.put( "offset", new Long( offset ) );
        to_hash_map.put( "length", new Long( length ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( file_id, writer );
        credentials.serialize( writer );
        writer.putLong( object_number );
        writer.putLong( object_version );
        writer.putLong( offset );
        writer.putLong( length );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        my_size += credentials.calculateSize();
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 102; }
    public Response createDefaultResponse() { return new internal_read_localResponse(); }


    private String file_id;
    private FileCredentials credentials;
    private long object_number;
    private long object_version;
    private long offset;
    private long length;

}

