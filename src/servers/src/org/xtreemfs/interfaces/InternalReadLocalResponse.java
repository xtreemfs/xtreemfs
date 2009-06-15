package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class InternalReadLocalResponse implements org.xtreemfs.interfaces.utils.Serializable
{
    public static final int TAG = 1052;

    
    public InternalReadLocalResponse() { data = new ObjectData(); object_list = new ObjectListSet(); }
    public InternalReadLocalResponse( ObjectData data, ObjectListSet object_list ) { this.data = data; this.object_list = object_list; }
    public InternalReadLocalResponse( Object from_hash_map ) { data = new ObjectData(); object_list = new ObjectListSet(); this.deserialize( from_hash_map ); }
    public InternalReadLocalResponse( Object[] from_array ) { data = new ObjectData(); object_list = new ObjectListSet();this.deserialize( from_array ); }

    public ObjectData getData() { return data; }
    public void setData( ObjectData data ) { this.data = data; }
    public ObjectListSet getObject_list() { return object_list; }
    public void setObject_list( ObjectListSet object_list ) { this.object_list = object_list; }

    // Object
    public String toString()
    {
        return "InternalReadLocalResponse( " + data.toString() + ", " + object_list.toString() + " )";
    }

    // Serializable
    public int getTag() { return 1052; }
    public String getTypeName() { return "org::xtreemfs::interfaces::InternalReadLocalResponse"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.data.deserialize( from_hash_map.get( "data" ) );
        this.object_list.deserialize( ( Object[] )from_hash_map.get( "object_list" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.data.deserialize( from_array[0] );
        this.object_list.deserialize( ( Object[] )from_array[1] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        data = new ObjectData(); data.deserialize( buf );
        object_list = new ObjectListSet(); object_list.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "data", data.serialize() );
        to_hash_map.put( "object_list", object_list.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        data.serialize( writer );
        object_list.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += data.calculateSize();
        my_size += object_list.calculateSize();
        return my_size;
    }


    private ObjectData data;
    private ObjectListSet object_list;    

}

