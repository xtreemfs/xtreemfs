package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class StripingPolicy implements org.xtreemfs.interfaces.utils.Serializable
{
    public StripingPolicy() { type = StripingPolicyType.STRIPING_POLICY_RAID0; stripe_size = 0; width = 0; }
    public StripingPolicy( StripingPolicyType type, int stripe_size, int width ) { this.type = type; this.stripe_size = stripe_size; this.width = width; }
    public StripingPolicy( Object from_hash_map ) { type = StripingPolicyType.STRIPING_POLICY_RAID0; stripe_size = 0; width = 0; this.deserialize( from_hash_map ); }
    public StripingPolicy( Object[] from_array ) { type = StripingPolicyType.STRIPING_POLICY_RAID0; stripe_size = 0; width = 0;this.deserialize( from_array ); }

    public StripingPolicyType getType() { return type; }
    public void setType( StripingPolicyType type ) { this.type = type; }
    public int getStripe_size() { return stripe_size; }
    public void setStripe_size( int stripe_size ) { this.stripe_size = stripe_size; }
    public int getWidth() { return width; }
    public void setWidth( int width ) { this.width = width; }

    public String getTypeName() { return "org::xtreemfs::interfaces::StripingPolicy"; }    
    public long getTypeId() { return 0; }

    public String toString()
    {
        return "StripingPolicy( " + type.toString() + ", " + Integer.toString( stripe_size ) + ", " + Integer.toString( width ) + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        
        this.stripe_size = ( ( Integer )from_hash_map.get( "stripe_size" ) ).intValue();
        this.width = ( ( Integer )from_hash_map.get( "width" ) ).intValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        
        this.stripe_size = ( ( Integer )from_array[1] ).intValue();
        this.width = ( ( Integer )from_array[2] ).intValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        type = StripingPolicyType.parseInt( buf.getInt() );
        stripe_size = buf.getInt();
        width = buf.getInt();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "type", type );
        to_hash_map.put( "stripe_size", new Integer( stripe_size ) );
        to_hash_map.put( "width", new Integer( width ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        writer.putInt( type.intValue() );
        writer.putInt( stripe_size );
        writer.putInt( width );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4;
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }


    private StripingPolicyType type;
    private int stripe_size;
    private int width;

}

