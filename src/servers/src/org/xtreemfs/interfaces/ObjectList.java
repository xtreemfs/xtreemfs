package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class ObjectList implements org.xtreemfs.interfaces.utils.Serializable
{
    public static final int TAG = 1055;

    
    public ObjectList() { set = null; stripeWidth = 0; firstObjectNo = 0; }
    public ObjectList( ReusableBuffer set, int stripeWidth, int firstObjectNo ) { this.set = set; this.stripeWidth = stripeWidth; this.firstObjectNo = firstObjectNo; }
    public ObjectList( Object from_hash_map ) { set = null; stripeWidth = 0; firstObjectNo = 0; this.deserialize( from_hash_map ); }
    public ObjectList( Object[] from_array ) { set = null; stripeWidth = 0; firstObjectNo = 0;this.deserialize( from_array ); }

    public ReusableBuffer getSet() { return set; }
    public void setSet( ReusableBuffer set ) { this.set = set; }
    public int getStripeWidth() { return stripeWidth; }
    public void setStripeWidth( int stripeWidth ) { this.stripeWidth = stripeWidth; }
    public int getFirstObjectNo() { return firstObjectNo; }
    public void setFirstObjectNo( int firstObjectNo ) { this.firstObjectNo = firstObjectNo; }

    // Object
    public String toString()
    {
        return "ObjectList( " + "\"" + set + "\"" + ", " + Integer.toString( stripeWidth ) + ", " + Integer.toString( firstObjectNo ) + " )";
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
        this.set = ( ReusableBuffer )from_hash_map.get( "set" );
        this.stripeWidth = ( ( Integer )from_hash_map.get( "stripeWidth" ) ).intValue();
        this.firstObjectNo = ( ( Integer )from_hash_map.get( "firstObjectNo" ) ).intValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.set = ( ReusableBuffer )from_array[0];
        this.stripeWidth = ( ( Integer )from_array[1] ).intValue();
        this.firstObjectNo = ( ( Integer )from_array[2] ).intValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        { set = org.xtreemfs.interfaces.utils.XDRUtils.deserializeSerializableBuffer( buf ); }
        stripeWidth = buf.getInt();
        firstObjectNo = buf.getInt();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "set", set );
        to_hash_map.put( "stripeWidth", new Integer( stripeWidth ) );
        to_hash_map.put( "firstObjectNo", new Integer( firstObjectNo ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeSerializableBuffer( set, writer ); }
        writer.putInt( stripeWidth );
        writer.putInt( firstObjectNo );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.serializableBufferLength( set );
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }


    private ReusableBuffer set;
    private int stripeWidth;
    private int firstObjectNo;    

}

