package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class InternalGmax implements org.xtreemfs.interfaces.utils.Serializable
{
    public static final int TAG = 1050;

    
    public InternalGmax() { epoch = 0; last_object_id = 0; file_size = 0; }
    public InternalGmax( long epoch, long last_object_id, long file_size ) { this.epoch = epoch; this.last_object_id = last_object_id; this.file_size = file_size; }
    public InternalGmax( Object from_hash_map ) { epoch = 0; last_object_id = 0; file_size = 0; this.deserialize( from_hash_map ); }
    public InternalGmax( Object[] from_array ) { epoch = 0; last_object_id = 0; file_size = 0;this.deserialize( from_array ); }

    public long getEpoch() { return epoch; }
    public void setEpoch( long epoch ) { this.epoch = epoch; }
    public long getLast_object_id() { return last_object_id; }
    public void setLast_object_id( long last_object_id ) { this.last_object_id = last_object_id; }
    public long getFile_size() { return file_size; }
    public void setFile_size( long file_size ) { this.file_size = file_size; }

    // Object
    public String toString()
    {
        return "InternalGmax( " + Long.toString( epoch ) + ", " + Long.toString( last_object_id ) + ", " + Long.toString( file_size ) + " )";
    }

    // Serializable
    public int getTag() { return 1050; }
    public String getTypeName() { return "org::xtreemfs::interfaces::InternalGmax"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.epoch = ( from_hash_map.get( "epoch" ) instanceof Integer ) ? ( ( Integer )from_hash_map.get( "epoch" ) ).longValue() : ( ( Long )from_hash_map.get( "epoch" ) ).longValue();
        this.last_object_id = ( from_hash_map.get( "last_object_id" ) instanceof Integer ) ? ( ( Integer )from_hash_map.get( "last_object_id" ) ).longValue() : ( ( Long )from_hash_map.get( "last_object_id" ) ).longValue();
        this.file_size = ( from_hash_map.get( "file_size" ) instanceof Integer ) ? ( ( Integer )from_hash_map.get( "file_size" ) ).longValue() : ( ( Long )from_hash_map.get( "file_size" ) ).longValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.epoch = ( from_array[0] instanceof Integer ) ? ( ( Integer )from_array[0] ).longValue() : ( ( Long )from_array[0] ).longValue();
        this.last_object_id = ( from_array[1] instanceof Integer ) ? ( ( Integer )from_array[1] ).longValue() : ( ( Long )from_array[1] ).longValue();
        this.file_size = ( from_array[2] instanceof Integer ) ? ( ( Integer )from_array[2] ).longValue() : ( ( Long )from_array[2] ).longValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        epoch = buf.getLong();
        last_object_id = buf.getLong();
        file_size = buf.getLong();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "epoch", new Long( epoch ) );
        to_hash_map.put( "last_object_id", new Long( last_object_id ) );
        to_hash_map.put( "file_size", new Long( file_size ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        writer.putLong( epoch );
        writer.putLong( last_object_id );
        writer.putLong( file_size );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }


    private long epoch;
    private long last_object_id;
    private long file_size;    

}

