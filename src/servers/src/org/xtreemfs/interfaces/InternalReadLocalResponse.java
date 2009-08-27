package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class InternalReadLocalResponse implements org.xtreemfs.interfaces.utils.Serializable
{
    public static final int TAG = 2009082670;

    
    public InternalReadLocalResponse() { data = new ObjectData(); object_set = new ObjectListSet(); }
    public InternalReadLocalResponse( ObjectData data, ObjectListSet object_set ) { this.data = data; this.object_set = object_set; }
    public InternalReadLocalResponse( Object from_hash_map ) { data = new ObjectData(); object_set = new ObjectListSet(); this.deserialize( from_hash_map ); }
    public InternalReadLocalResponse( Object[] from_array ) { data = new ObjectData(); object_set = new ObjectListSet();this.deserialize( from_array ); }

    public ObjectData getData() { return data; }
    public void setData( ObjectData data ) { this.data = data; }
    public ObjectListSet getObject_set() { return object_set; }
    public void setObject_set( ObjectListSet object_set ) { this.object_set = object_set; }

    // Object
    public String toString()
    {
        return "InternalReadLocalResponse( " + data.toString() + ", " + object_set.toString() + " )";
    }

    // Serializable
    public int getTag() { return 2009082670; }
    public String getTypeName() { return "org::xtreemfs::interfaces::InternalReadLocalResponse"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.data.deserialize( from_hash_map.get( "data" ) );
        this.object_set.deserialize( ( Object[] )from_hash_map.get( "object_set" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.data.deserialize( from_array[0] );
        this.object_set.deserialize( ( Object[] )from_array[1] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        data = new ObjectData(); data.deserialize( buf );
        object_set = new ObjectListSet(); object_set.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "data", data.serialize() );
        to_hash_map.put( "object_set", object_set.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        data.serialize( writer );
        object_set.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += data.calculateSize();
        my_size += object_set.calculateSize();
        return my_size;
    }


    private ObjectData data;
    private ObjectListSet object_set;    

}

