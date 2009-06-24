package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class ObjectList implements org.xtreemfs.interfaces.utils.Serializable
{
    public static final int TAG = 1055;

    
    public ObjectList() { object_list = null; object_list_type = 0; is_complete = false; }
    public ObjectList( ReusableBuffer object_list, int object_list_type, boolean is_complete ) { this.object_list = object_list; this.object_list_type = object_list_type; this.is_complete = is_complete; }
    public ObjectList( Object from_hash_map ) { object_list = null; object_list_type = 0; is_complete = false; this.deserialize( from_hash_map ); }
    public ObjectList( Object[] from_array ) { object_list = null; object_list_type = 0; is_complete = false;this.deserialize( from_array ); }

    public ReusableBuffer getObject_list() { return object_list; }
    public void setObject_list( ReusableBuffer object_list ) { this.object_list = object_list; }
    public int getObject_list_type() { return object_list_type; }
    public void setObject_list_type( int object_list_type ) { this.object_list_type = object_list_type; }
    public boolean getIs_complete() { return is_complete; }
    public void setIs_complete( boolean is_complete ) { this.is_complete = is_complete; }

    // Object
    public String toString()
    {
        return "ObjectList( " + "\"" + object_list + "\"" + ", " + Integer.toString( object_list_type ) + ", " + Boolean.toString( is_complete ) + " )";
    }

    // Serializable
    public int getTag() { return 1055; }
    public String getTypeName() { return "org::xtreemfs::interfaces::ObjectList"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.object_list = ( ReusableBuffer )from_hash_map.get( "object_list" );
        this.object_list_type = ( from_hash_map.get( "object_list_type" ) instanceof Integer ) ? ( ( Integer )from_hash_map.get( "object_list_type" ) ).intValue() : ( ( Long )from_hash_map.get( "object_list_type" ) ).intValue();
        this.is_complete = ( ( Boolean )from_hash_map.get( "is_complete" ) ).booleanValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.object_list = ( ReusableBuffer )from_array[0];
        this.object_list_type = ( from_array[1] instanceof Integer ) ? ( ( Integer )from_array[1] ).intValue() : ( ( Long )from_array[1] ).intValue();
        this.is_complete = ( ( Boolean )from_array[2] ).booleanValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        { object_list = org.xtreemfs.interfaces.utils.XDRUtils.deserializeSerializableBuffer( buf ); }
        object_list_type = buf.getInt();
        is_complete = buf.getInt() != 0;
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "object_list", object_list );
        to_hash_map.put( "object_list_type", new Integer( object_list_type ) );
        to_hash_map.put( "is_complete", new Boolean( is_complete ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeSerializableBuffer( object_list, writer ); }
        writer.putInt( object_list_type );
        writer.putInt( is_complete ? 1 : 0 );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.serializableBufferLength( object_list );
        my_size += ( Integer.SIZE / 8 );
        my_size += 4;
        return my_size;
    }


    private ReusableBuffer object_list;
    private int object_list_type;
    private boolean is_complete;    

}

