package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class ObjectData implements org.xtreemfs.interfaces.utils.Serializable
{
    public static final int TAG = 1051;

    
    public ObjectData() { checksum = 0; invalid_checksum_on_osd = false; zero_padding = 0; data = null; }
    public ObjectData( int checksum, boolean invalid_checksum_on_osd, int zero_padding, ReusableBuffer data ) { this.checksum = checksum; this.invalid_checksum_on_osd = invalid_checksum_on_osd; this.zero_padding = zero_padding; this.data = data; }
    public ObjectData( Object from_hash_map ) { checksum = 0; invalid_checksum_on_osd = false; zero_padding = 0; data = null; this.deserialize( from_hash_map ); }
    public ObjectData( Object[] from_array ) { checksum = 0; invalid_checksum_on_osd = false; zero_padding = 0; data = null;this.deserialize( from_array ); }

    public int getChecksum() { return checksum; }
    public void setChecksum( int checksum ) { this.checksum = checksum; }
    public boolean getInvalid_checksum_on_osd() { return invalid_checksum_on_osd; }
    public void setInvalid_checksum_on_osd( boolean invalid_checksum_on_osd ) { this.invalid_checksum_on_osd = invalid_checksum_on_osd; }
    public int getZero_padding() { return zero_padding; }
    public void setZero_padding( int zero_padding ) { this.zero_padding = zero_padding; }
    public ReusableBuffer getData() { return data; }
    public void setData( ReusableBuffer data ) { this.data = data; }

    // Object
    public String toString()
    {
        return "ObjectData( " + Integer.toString( checksum ) + ", " + Boolean.toString( invalid_checksum_on_osd ) + ", " + Integer.toString( zero_padding ) + ", " + "\"" + data + "\"" + " )";
    }

    // Serializable
    public int getTag() { return 1051; }
    public String getTypeName() { return "org::xtreemfs::interfaces::ObjectData"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.checksum = ( ( Integer )from_hash_map.get( "checksum" ) ).intValue();
        this.invalid_checksum_on_osd = ( ( Boolean )from_hash_map.get( "invalid_checksum_on_osd" ) ).booleanValue();
        this.zero_padding = ( ( Integer )from_hash_map.get( "zero_padding" ) ).intValue();
        this.data = ( ReusableBuffer )from_hash_map.get( "data" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.checksum = ( ( Integer )from_array[0] ).intValue();
        this.invalid_checksum_on_osd = ( ( Boolean )from_array[1] ).booleanValue();
        this.zero_padding = ( ( Integer )from_array[2] ).intValue();
        this.data = ( ReusableBuffer )from_array[3];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        checksum = buf.getInt();
        invalid_checksum_on_osd = buf.getInt() != 0;
        zero_padding = buf.getInt();
        { data = org.xtreemfs.interfaces.utils.XDRUtils.deserializeSerializableBuffer( buf ); }
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "checksum", new Integer( checksum ) );
        to_hash_map.put( "invalid_checksum_on_osd", new Boolean( invalid_checksum_on_osd ) );
        to_hash_map.put( "zero_padding", new Integer( zero_padding ) );
        to_hash_map.put( "data", data );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        writer.putInt( checksum );
        writer.putInt( invalid_checksum_on_osd ? 1 : 0 );
        writer.putInt( zero_padding );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeSerializableBuffer( data, writer ); }
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += ( Integer.SIZE / 8 );
        my_size += 4;
        my_size += ( Integer.SIZE / 8 );
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.serializableBufferLength( data );
        return my_size;
    }


    private int checksum;
    private boolean invalid_checksum_on_osd;
    private int zero_padding;
    private ReusableBuffer data;    

}

