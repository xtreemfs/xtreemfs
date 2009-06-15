package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_internal_get_object_listRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 1410;

    
    public xtreemfs_internal_get_object_listRequest() { file_credentials = new FileCredentials(); file_id = ""; attachObjectList = false; }
    public xtreemfs_internal_get_object_listRequest( FileCredentials file_credentials, String file_id, boolean attachObjectList ) { this.file_credentials = file_credentials; this.file_id = file_id; this.attachObjectList = attachObjectList; }
    public xtreemfs_internal_get_object_listRequest( Object from_hash_map ) { file_credentials = new FileCredentials(); file_id = ""; attachObjectList = false; this.deserialize( from_hash_map ); }
    public xtreemfs_internal_get_object_listRequest( Object[] from_array ) { file_credentials = new FileCredentials(); file_id = ""; attachObjectList = false;this.deserialize( from_array ); }

    public FileCredentials getFile_credentials() { return file_credentials; }
    public void setFile_credentials( FileCredentials file_credentials ) { this.file_credentials = file_credentials; }
    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public boolean getAttachObjectList() { return attachObjectList; }
    public void setAttachObjectList( boolean attachObjectList ) { this.attachObjectList = attachObjectList; }

    // Object
    public String toString()
    {
        return "xtreemfs_internal_get_object_listRequest( " + file_credentials.toString() + ", " + "\"" + file_id + "\"" + ", " + Boolean.toString( attachObjectList ) + " )";
    }

    // Serializable
    public int getTag() { return 1410; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_get_object_listRequest"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.file_credentials.deserialize( from_hash_map.get( "file_credentials" ) );
        this.file_id = ( String )from_hash_map.get( "file_id" );
        this.attachObjectList = ( ( Boolean )from_hash_map.get( "attachObjectList" ) ).booleanValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.file_credentials.deserialize( from_array[0] );
        this.file_id = ( String )from_array[1];
        this.attachObjectList = ( ( Boolean )from_array[2] ).booleanValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        file_credentials = new FileCredentials(); file_credentials.deserialize( buf );
        file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        attachObjectList = buf.getInt() != 0;
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "file_credentials", file_credentials.serialize() );
        to_hash_map.put( "file_id", file_id );
        to_hash_map.put( "attachObjectList", new Boolean( attachObjectList ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        file_credentials.serialize( writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( file_id, writer );
        writer.putInt( attachObjectList ? 1 : 0 );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += file_credentials.calculateSize();
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        my_size += 4;
        return my_size;
    }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_internal_get_object_listResponse(); }


    private FileCredentials file_credentials;
    private String file_id;
    private boolean attachObjectList;    

}

