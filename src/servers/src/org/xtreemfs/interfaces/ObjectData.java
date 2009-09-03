package org.xtreemfs.interfaces;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class ObjectData extends Struct
{
    public static final int TAG = 2009082669;
    
    public ObjectData() {  }
    public ObjectData( int checksum, boolean invalid_checksum_on_osd, int zero_padding, ReusableBuffer data ) { this.checksum = checksum; this.invalid_checksum_on_osd = invalid_checksum_on_osd; this.zero_padding = zero_padding; this.data = data; }

    public int getChecksum() { return checksum; }
    public void setChecksum( int checksum ) { this.checksum = checksum; }
    public boolean getInvalid_checksum_on_osd() { return invalid_checksum_on_osd; }
    public void setInvalid_checksum_on_osd( boolean invalid_checksum_on_osd ) { this.invalid_checksum_on_osd = invalid_checksum_on_osd; }
    public int getZero_padding() { return zero_padding; }
    public void setZero_padding( int zero_padding ) { this.zero_padding = zero_padding; }
    public ReusableBuffer getData() { return data; }
    public void setData( ReusableBuffer data ) { this.data = data; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082669;    

    // yidl.Object
    public int getTag() { return 2009082669; }
    public String getTypeName() { return "org::xtreemfs::interfaces::ObjectData"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += ( Integer.SIZE / 8 );
        my_size += 4;
        my_size += ( Integer.SIZE / 8 );
        my_size += ( ( data.remaining() + Integer.SIZE/8 ) % 4 == 0 ) ? ( data.remaining() + Integer.SIZE/8 ) : ( data.remaining() + Integer.SIZE/8 + 4 - ( data.remaining() + Integer.SIZE/8 ) % 4 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeUint32( "checksum", checksum );
        marshaller.writeBoolean( "invalid_checksum_on_osd", invalid_checksum_on_osd );
        marshaller.writeUint32( "zero_padding", zero_padding );
        marshaller.writeBuffer( "data", data );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        checksum = unmarshaller.readUint32( "checksum" );
        invalid_checksum_on_osd = unmarshaller.readBoolean( "invalid_checksum_on_osd" );
        zero_padding = unmarshaller.readUint32( "zero_padding" );
        data = ( ReusableBuffer )unmarshaller.readBuffer( "data" );    
    }
        
    

    private int checksum;
    private boolean invalid_checksum_on_osd;
    private int zero_padding;
    private ReusableBuffer data;    

}

