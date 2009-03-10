package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class internal_get_gmaxRequest implements org.xtreemfs.interfaces.utils.Request
{
    public internal_get_gmaxRequest() { file_id = ""; credentials = new FileCredentials(); }
    public internal_get_gmaxRequest( String file_id, FileCredentials credentials ) { this.file_id = file_id; this.credentials = credentials; }
    public internal_get_gmaxRequest( Object from_hash_map ) { file_id = ""; credentials = new FileCredentials(); this.deserialize( from_hash_map ); }
    public internal_get_gmaxRequest( Object[] from_array ) { file_id = ""; credentials = new FileCredentials();this.deserialize( from_array ); }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public FileCredentials getCredentials() { return credentials; }
    public void setCredentials( FileCredentials credentials ) { this.credentials = credentials; }

    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::internal_get_gmaxRequest"; }    
    public long getTypeId() { return 100; }

    public String toString()
    {
        return "internal_get_gmaxRequest( " + "\"" + file_id + "\"" + ", " + credentials.toString() + " )"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.file_id = ( String )from_hash_map.get( "file_id" );
        this.credentials.deserialize( from_hash_map.get( "credentials" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.file_id = ( String )from_array[0];
        this.credentials.deserialize( from_array[1] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        credentials = new FileCredentials(); credentials.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "file_id", file_id );
        to_hash_map.put( "credentials", credentials.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( file_id, writer );
        credentials.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        my_size += credentials.calculateSize();
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 100; }
    public Response createDefaultResponse() { return new internal_get_gmaxResponse(); }


    private String file_id;
    private FileCredentials credentials;

}

