package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_check_objectRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082939;

    
    public xtreemfs_check_objectRequest() { file_credentials = new FileCredentials(); file_id = ""; object_number = 0; object_version = 0; }
    public xtreemfs_check_objectRequest( FileCredentials file_credentials, String file_id, long object_number, long object_version ) { this.file_credentials = file_credentials; this.file_id = file_id; this.object_number = object_number; this.object_version = object_version; }
    public xtreemfs_check_objectRequest( Object from_hash_map ) { file_credentials = new FileCredentials(); file_id = ""; object_number = 0; object_version = 0; this.deserialize( from_hash_map ); }
    public xtreemfs_check_objectRequest( Object[] from_array ) { file_credentials = new FileCredentials(); file_id = ""; object_number = 0; object_version = 0;this.deserialize( from_array ); }

    public FileCredentials getFile_credentials() { return file_credentials; }
    public void setFile_credentials( FileCredentials file_credentials ) { this.file_credentials = file_credentials; }
    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public long getObject_number() { return object_number; }
    public void setObject_number( long object_number ) { this.object_number = object_number; }
    public long getObject_version() { return object_version; }
    public void setObject_version( long object_version ) { this.object_version = object_version; }

    // Object
    public String toString()
    {
        return "xtreemfs_check_objectRequest( " + file_credentials.toString() + ", " + "\"" + file_id + "\"" + ", " + Long.toString( object_number ) + ", " + Long.toString( object_version ) + " )";
    }

    // Serializable
    public int getTag() { return 2009082939; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_check_objectRequest"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.file_credentials.deserialize( from_hash_map.get( "file_credentials" ) );
        this.file_id = ( String )from_hash_map.get( "file_id" );
        this.object_number = ( ( Long )from_hash_map.get( "object_number" ) ).longValue();
        this.object_version = ( ( Long )from_hash_map.get( "object_version" ) ).longValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.file_credentials.deserialize( from_array[0] );
        this.file_id = ( String )from_array[1];
        this.object_number = ( ( Long )from_array[2] ).longValue();
        this.object_version = ( ( Long )from_array[3] ).longValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        file_credentials = new FileCredentials(); file_credentials.deserialize( buf );
        file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        object_number = buf.getLong();
        object_version = buf.getLong();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "file_credentials", file_credentials.serialize() );
        to_hash_map.put( "file_id", file_id );
        to_hash_map.put( "object_number", new Long( object_number ) );
        to_hash_map.put( "object_version", new Long( object_version ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        file_credentials.serialize( writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( file_id, writer );
        writer.putLong( object_number );
        writer.putLong( object_version );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += file_credentials.calculateSize();
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_check_objectResponse(); }


    private FileCredentials file_credentials;
    private String file_id;
    private long object_number;
    private long object_version;    

}

