package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class Context implements org.xtreemfs.interfaces.utils.Serializable
{
    public Context() { user_id = ""; group_ids = new StringSet(); }
    public Context( String user_id, StringSet group_ids ) { this.user_id = user_id; this.group_ids = group_ids; }
    public Context( Object from_hash_map ) { user_id = ""; group_ids = new StringSet(); this.deserialize( from_hash_map ); }
    public Context( Object[] from_array ) { user_id = ""; group_ids = new StringSet();this.deserialize( from_array ); }

    public String getUser_id() { return user_id; }
    public void setUser_id( String user_id ) { this.user_id = user_id; }
    public StringSet getGroup_ids() { return group_ids; }
    public void setGroup_ids( StringSet group_ids ) { this.group_ids = group_ids; }

    // Serializable
    public String getTypeName() { return "org::xtreemfs::interfaces::Context"; }    
    public long getTypeId() { return 0; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.user_id = ( String )from_hash_map.get( "user_id" );
        this.group_ids.deserialize( from_hash_map.get( "group_ids" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.user_id = ( String )from_array[0];
        this.group_ids.deserialize( from_array[1] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        user_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        group_ids = new StringSet(); group_ids.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "user_id", user_id );
        to_hash_map.put( "group_ids", group_ids.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( user_id, writer );
        group_ids.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(user_id);
        my_size += group_ids.calculateSize();
        return my_size;
    }


    private String user_id;
    private StringSet group_ids;

}

