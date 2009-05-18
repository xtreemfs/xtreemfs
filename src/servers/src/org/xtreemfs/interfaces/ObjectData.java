package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class ObjectData implements org.xtreemfs.interfaces.utils.Serializable
{
    public ObjectData() { data = null; checksum = 0; zero_padding = 0; invalid_checksum_on_osd = false; }
    public ObjectData( ReusableBuffer data, int checksum, int zero_padding, boolean invalid_checksum_on_osd ) { this.data = data; this.checksum = checksum; this.zero_padding = zero_padding; this.invalid_checksum_on_osd = invalid_checksum_on_osd; }
    public ObjectData( Object from_hash_map ) { data = null; checksum = 0; zero_padding = 0; invalid_checksum_on_osd = false; this.deserialize( from_hash_map ); }
    public ObjectData( Object[] from_array ) { data = null; checksum = 0; zero_padding = 0; invalid_checksum_on_osd = false;this.deserialize( from_array ); }

    public ReusableBuffer getData() { return data; }
    public void setData( ReusableBuffer data ) { this.data = data; }
    public int getChecksum() { return checksum; }
    public void setChecksum( int checksum ) { this.checksum = checksum; }
    public int getZero_padding() { return zero_padding; }
    public void setZero_padding( int zero_padding ) { this.zero_padding = zero_padding; }
    public boolean getInvalid_checksum_on_osd() { return invalid_checksum_on_osd; }
    public void setInvalid_checksum_on_osd( boolean invalid_checksum_on_osd ) { this.invalid_checksum_on_osd = invalid_checksum_on_osd; }

    public long getTag() { return 1051; }
    public String getTypeName() { return "org::xtreemfs::interfaces::ObjectData"; }

    public String toString()
    {
        return "ObjectData( " + "\"" + data + "\"" + ", " + Integer.toString( checksum ) + ", " + Integer.toString( zero_padding ) + ", " + Boolean.toString( invalid_checksum_on_osd ) + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.data = ( ReusableBuffer )from_hash_map.get( "data" );
        this.checksum = ( ( Integer )from_hash_map.get( "checksum" ) ).intValue();
        this.zero_padding = ( ( Integer )from_hash_map.get( "zero_padding" ) ).intValue();
        this.invalid_checksum_on_osd = ( ( Boolean )from_hash_map.get( "invalid_checksum_on_osd" ) ).booleanValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.data = ( ReusableBuffer )from_array[0];
        this.checksum = ( ( Integer )from_array[1] ).intValue();
        this.zero_padding = ( ( Integer )from_array[2] ).intValue();
        this.invalid_checksum_on_osd = ( ( Boolean )from_array[3] ).booleanValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        { data = org.xtreemfs.interfaces.utils.XDRUtils.deserializeSerializableBuffer( buf ); }
        checksum = buf.getInt();
        zero_padding = buf.getInt();
        invalid_checksum_on_osd = buf.getInt() != 0;
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "data", data );
        to_hash_map.put( "checksum", new Integer( checksum ) );
        to_hash_map.put( "zero_padding", new Integer( zero_padding ) );
        to_hash_map.put( "invalid_checksum_on_osd", new Boolean( invalid_checksum_on_osd ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeSerializableBuffer( data, writer ); }
        writer.putInt( checksum );
        writer.putInt( zero_padding );
        writer.putInt( invalid_checksum_on_osd ? 1 : 0 );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.serializableBufferLength( data );
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        my_size += 4;
        return my_size;
    }


    private ReusableBuffer data;
    private int checksum;
    private int zero_padding;
    private boolean invalid_checksum_on_osd;    

}

