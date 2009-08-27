package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class NewFileSize implements org.xtreemfs.interfaces.utils.Serializable
{
    public static final int TAG = 2009082629;

    
    public NewFileSize() { size_in_bytes = 0; truncate_epoch = 0; }
    public NewFileSize( long size_in_bytes, int truncate_epoch ) { this.size_in_bytes = size_in_bytes; this.truncate_epoch = truncate_epoch; }
    public NewFileSize( Object from_hash_map ) { size_in_bytes = 0; truncate_epoch = 0; this.deserialize( from_hash_map ); }
    public NewFileSize( Object[] from_array ) { size_in_bytes = 0; truncate_epoch = 0;this.deserialize( from_array ); }

    public long getSize_in_bytes() { return size_in_bytes; }
    public void setSize_in_bytes( long size_in_bytes ) { this.size_in_bytes = size_in_bytes; }
    public int getTruncate_epoch() { return truncate_epoch; }
    public void setTruncate_epoch( int truncate_epoch ) { this.truncate_epoch = truncate_epoch; }

    // Object
    public String toString()
    {
        return "NewFileSize( " + Long.toString( size_in_bytes ) + ", " + Integer.toString( truncate_epoch ) + " )";
    }

    // Serializable
    public int getTag() { return 2009082629; }
    public String getTypeName() { return "org::xtreemfs::interfaces::NewFileSize"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.size_in_bytes = ( ( Long )from_hash_map.get( "size_in_bytes" ) ).longValue();
        this.truncate_epoch = ( ( Integer )from_hash_map.get( "truncate_epoch" ) ).intValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.size_in_bytes = ( ( Long )from_array[0] ).longValue();
        this.truncate_epoch = ( ( Integer )from_array[1] ).intValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        size_in_bytes = buf.getLong();
        truncate_epoch = buf.getInt();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "size_in_bytes", new Long( size_in_bytes ) );
        to_hash_map.put( "truncate_epoch", new Integer( truncate_epoch ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        writer.putLong( size_in_bytes );
        writer.putInt( truncate_epoch );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += ( Long.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }


    private long size_in_bytes;
    private int truncate_epoch;    

}

