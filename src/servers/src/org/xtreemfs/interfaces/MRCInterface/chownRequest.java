package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class chownRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 1203;

    
    public chownRequest() { path = ""; user_id = ""; group_id = ""; }
    public chownRequest( String path, String user_id, String group_id ) { this.path = path; this.user_id = user_id; this.group_id = group_id; }
    public chownRequest( Object from_hash_map ) { path = ""; user_id = ""; group_id = ""; this.deserialize( from_hash_map ); }
    public chownRequest( Object[] from_array ) { path = ""; user_id = ""; group_id = "";this.deserialize( from_array ); }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public String getUser_id() { return user_id; }
    public void setUser_id( String user_id ) { this.user_id = user_id; }
    public String getGroup_id() { return group_id; }
    public void setGroup_id( String group_id ) { this.group_id = group_id; }

    // Object
    public String toString()
    {
        return "chownRequest( " + "\"" + path + "\"" + ", " + "\"" + user_id + "\"" + ", " + "\"" + group_id + "\"" + " )";
    }

    // Serializable
    public int getTag() { return 1203; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::chownRequest"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.path = ( String )from_hash_map.get( "path" );
        this.user_id = ( String )from_hash_map.get( "user_id" );
        this.group_id = ( String )from_hash_map.get( "group_id" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.path = ( String )from_array[0];
        this.user_id = ( String )from_array[1];
        this.group_id = ( String )from_array[2];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        user_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        group_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "path", path );
        to_hash_map.put( "user_id", user_id );
        to_hash_map.put( "group_id", group_id );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( path, writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( user_id, writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( group_id, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(path);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(user_id);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(group_id);
        return my_size;
    }

    // Request
    public Response createDefaultResponse() { return new chownResponse(); }


    private String path;
    private String user_id;
    private String group_id;    

}

