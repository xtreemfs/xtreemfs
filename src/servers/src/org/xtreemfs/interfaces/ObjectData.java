package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class ObjectData implements Struct
{
    public static final int TAG = 2010011962;
    
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

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        string_writer.append(this.getClass().getCanonicalName());
        string_writer.append(" ");
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }


    // java.io.Serializable
    public static final long serialVersionUID = 2010011962;    

    // yidl.runtime.Object
    public int getTag() { return 2010011962; }
    public String getTypeName() { return "org::xtreemfs::interfaces::ObjectData"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8; // checksum
        my_size += Integer.SIZE / 8; // invalid_checksum_on_osd
        my_size += Integer.SIZE / 8; // zero_padding
        my_size += Integer.SIZE / 8 + ( data != null ? ( ( data.remaining() % 4 == 0 ) ? data.remaining() : ( data.remaining() + 4 - data.remaining() % 4 ) ) : 0 ); // data
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

