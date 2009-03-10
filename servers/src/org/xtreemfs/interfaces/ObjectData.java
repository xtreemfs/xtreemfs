package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class ObjectData implements org.xtreemfs.interfaces.utils.Serializable
{
    public ObjectData() { checksum = ""; zero_padding = 0; invalid_checksum_on_osd = false; data = null; }
    public ObjectData( String checksum, int zero_padding, boolean invalid_checksum_on_osd, ReusableBuffer data ) { this.checksum = checksum; this.zero_padding = zero_padding; this.invalid_checksum_on_osd = invalid_checksum_on_osd; this.data = data; }
    public ObjectData( Object from_hash_map ) { checksum = ""; zero_padding = 0; invalid_checksum_on_osd = false; data = null; this.deserialize( from_hash_map ); }
    public ObjectData( Object[] from_array ) { checksum = ""; zero_padding = 0; invalid_checksum_on_osd = false; data = null;this.deserialize( from_array ); }

    public String getChecksum() { return checksum; }
    public void setChecksum( String checksum ) { this.checksum = checksum; }
    public int getZero_padding() { return zero_padding; }
    public void setZero_padding( int zero_padding ) { this.zero_padding = zero_padding; }
    public boolean getInvalid_checksum_on_osd() { return invalid_checksum_on_osd; }
    public void setInvalid_checksum_on_osd( boolean invalid_checksum_on_osd ) { this.invalid_checksum_on_osd = invalid_checksum_on_osd; }
    public ReusableBuffer getData() { return data; }
    public void setData( ReusableBuffer data ) { this.data = data; }

    public String getTypeName() { return "org::xtreemfs::interfaces::ObjectData"; }    
    public long getTypeId() { return 0; }

    public String toString()
    {
        return "ObjectData( " + "\"" + checksum + "\"" + ", " + Integer.toString( zero_padding ) + ", " + Boolean.toString( invalid_checksum_on_osd ) + ", " + "\"" + data + "\"" + " )"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.checksum = ( String )from_hash_map.get( "checksum" );
        this.zero_padding = ( ( Integer )from_hash_map.get( "zero_padding" ) ).intValue();
        this.invalid_checksum_on_osd = ( ( Boolean )from_hash_map.get( "invalid_checksum_on_osd" ) ).booleanValue();
        this.data = ( ReusableBuffer )from_hash_map.get( "data" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.checksum = ( String )from_array[0];
        this.zero_padding = ( ( Integer )from_array[1] ).intValue();
        this.invalid_checksum_on_osd = ( ( Boolean )from_array[2] ).booleanValue();
        this.data = ( ReusableBuffer )from_array[3];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        checksum = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        zero_padding = buf.getInt();
        invalid_checksum_on_osd = buf.getInt() != 0;
        { data = org.xtreemfs.interfaces.utils.XDRUtils.deserializeSerializableBuffer( buf ); }
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "checksum", checksum );
        to_hash_map.put( "zero_padding", new Integer( zero_padding ) );
        to_hash_map.put( "invalid_checksum_on_osd", new Boolean( invalid_checksum_on_osd ) );
        to_hash_map.put( "data", data );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( checksum, writer );
        writer.putInt( zero_padding );
        writer.putInt( invalid_checksum_on_osd ? 1 : 0 );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeSerializableBuffer( data, writer ); }
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(checksum);
        my_size += ( Integer.SIZE / 8 );
        my_size += 4;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.serializableBufferLength( data );
        return my_size;
    }


    private String checksum;
    private int zero_padding;
    private boolean invalid_checksum_on_osd;
    private ReusableBuffer data;

}

