package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_internal_truncateRequest implements org.xtreemfs.interfaces.utils.Request
{
    public xtreemfs_internal_truncateRequest() { file_credentials = new FileCredentials(); file_id = ""; new_file_size = 0; }
    public xtreemfs_internal_truncateRequest( FileCredentials file_credentials, String file_id, long new_file_size ) { this.file_credentials = file_credentials; this.file_id = file_id; this.new_file_size = new_file_size; }
    public xtreemfs_internal_truncateRequest( Object from_hash_map ) { file_credentials = new FileCredentials(); file_id = ""; new_file_size = 0; this.deserialize( from_hash_map ); }
    public xtreemfs_internal_truncateRequest( Object[] from_array ) { file_credentials = new FileCredentials(); file_id = ""; new_file_size = 0;this.deserialize( from_array ); }

    public FileCredentials getFile_credentials() { return file_credentials; }
    public void setFile_credentials( FileCredentials file_credentials ) { this.file_credentials = file_credentials; }
    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public long getNew_file_size() { return new_file_size; }
    public void setNew_file_size( long new_file_size ) { this.new_file_size = new_file_size; }

    public long getTag() { return 1401; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_truncateRequest"; }

    public String toString()
    {
        return "xtreemfs_internal_truncateRequest( " + file_credentials.toString() + ", " + "\"" + file_id + "\"" + ", " + Long.toString( new_file_size ) + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.file_credentials.deserialize( from_hash_map.get( "file_credentials" ) );
        this.file_id = ( String )from_hash_map.get( "file_id" );
        this.new_file_size = ( ( Long )from_hash_map.get( "new_file_size" ) ).longValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.file_credentials.deserialize( from_array[0] );
        this.file_id = ( String )from_array[1];
        this.new_file_size = ( ( Long )from_array[2] ).longValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        file_credentials = new FileCredentials(); file_credentials.deserialize( buf );
        file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        new_file_size = buf.getLong();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "file_credentials", file_credentials.serialize() );
        to_hash_map.put( "file_id", file_id );
        to_hash_map.put( "new_file_size", new Long( new_file_size ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        file_credentials.serialize( writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( file_id, writer );
        writer.putLong( new_file_size );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += file_credentials.calculateSize();
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 1401; }
    public Response createDefaultResponse() { return new xtreemfs_internal_truncateResponse(); }


    private FileCredentials file_credentials;
    private String file_id;
    private long new_file_size;    

}

