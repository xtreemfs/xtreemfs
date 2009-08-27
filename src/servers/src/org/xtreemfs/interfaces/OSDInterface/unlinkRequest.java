package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class unlinkRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082930;

    
    public unlinkRequest() { file_credentials = new FileCredentials(); file_id = ""; }
    public unlinkRequest( FileCredentials file_credentials, String file_id ) { this.file_credentials = file_credentials; this.file_id = file_id; }
    public unlinkRequest( Object from_hash_map ) { file_credentials = new FileCredentials(); file_id = ""; this.deserialize( from_hash_map ); }
    public unlinkRequest( Object[] from_array ) { file_credentials = new FileCredentials(); file_id = "";this.deserialize( from_array ); }

    public FileCredentials getFile_credentials() { return file_credentials; }
    public void setFile_credentials( FileCredentials file_credentials ) { this.file_credentials = file_credentials; }
    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }

    // Object
    public String toString()
    {
        return "unlinkRequest( " + file_credentials.toString() + ", " + "\"" + file_id + "\"" + " )";
    }

    // Serializable
    public int getTag() { return 2009082930; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::unlinkRequest"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.file_credentials.deserialize( from_hash_map.get( "file_credentials" ) );
        this.file_id = ( String )from_hash_map.get( "file_id" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.file_credentials.deserialize( from_array[0] );
        this.file_id = ( String )from_array[1];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        file_credentials = new FileCredentials(); file_credentials.deserialize( buf );
        file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "file_credentials", file_credentials.serialize() );
        to_hash_map.put( "file_id", file_id );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        file_credentials.serialize( writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( file_id, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += file_credentials.calculateSize();
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        return my_size;
    }

    // Request
    public Response createDefaultResponse() { return new unlinkResponse(); }


    private FileCredentials file_credentials;
    private String file_id;    

}

