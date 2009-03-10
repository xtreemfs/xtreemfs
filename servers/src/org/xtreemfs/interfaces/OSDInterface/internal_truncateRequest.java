package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class internal_truncateRequest implements org.xtreemfs.interfaces.utils.Request
{
    public internal_truncateRequest() { file_id = ""; credentials = new FileCredentials(); new_file_size = 0; }
    public internal_truncateRequest( String file_id, FileCredentials credentials, long new_file_size ) { this.file_id = file_id; this.credentials = credentials; this.new_file_size = new_file_size; }
    public internal_truncateRequest( Object from_hash_map ) { file_id = ""; credentials = new FileCredentials(); new_file_size = 0; this.deserialize( from_hash_map ); }
    public internal_truncateRequest( Object[] from_array ) { file_id = ""; credentials = new FileCredentials(); new_file_size = 0;this.deserialize( from_array ); }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public FileCredentials getCredentials() { return credentials; }
    public void setCredentials( FileCredentials credentials ) { this.credentials = credentials; }
    public long getNew_file_size() { return new_file_size; }
    public void setNew_file_size( long new_file_size ) { this.new_file_size = new_file_size; }

    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::internal_truncateRequest"; }    
    public long getTypeId() { return 101; }

    public String toString()
    {
        return "internal_truncateRequest( " + "\"" + file_id + "\"" + ", " + credentials.toString() + ", " + Long.toString( new_file_size ) + " )"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.file_id = ( String )from_hash_map.get( "file_id" );
        this.credentials.deserialize( from_hash_map.get( "credentials" ) );
        this.new_file_size = ( ( Long )from_hash_map.get( "new_file_size" ) ).longValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.file_id = ( String )from_array[0];
        this.credentials.deserialize( from_array[1] );
        this.new_file_size = ( ( Long )from_array[2] ).longValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        credentials = new FileCredentials(); credentials.deserialize( buf );
        new_file_size = buf.getLong();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "file_id", file_id );
        to_hash_map.put( "credentials", credentials.serialize() );
        to_hash_map.put( "new_file_size", new Long( new_file_size ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( file_id, writer );
        credentials.serialize( writer );
        writer.putLong( new_file_size );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        my_size += credentials.calculateSize();
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 101; }
    public Response createDefaultResponse() { return new internal_truncateResponse(); }


    private String file_id;
    private FileCredentials credentials;
    private long new_file_size;

}

