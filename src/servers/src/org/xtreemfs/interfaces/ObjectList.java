package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class ObjectList implements Struct
{
    public static final int TAG = 2009082673;
    
    public ObjectList() {  }
    public ObjectList( ReusableBuffer set, int stripeWidth, int firstObjectNo ) { this.set = set; this.stripeWidth = stripeWidth; this.firstObjectNo = firstObjectNo; }

    public ReusableBuffer getSet() { return set; }
    public void setSet( ReusableBuffer set ) { this.set = set; }
    public int getStripeWidth() { return stripeWidth; }
    public void setStripeWidth( int stripeWidth ) { this.stripeWidth = stripeWidth; }
    public int getFirstObjectNo() { return firstObjectNo; }
    public void setFirstObjectNo( int firstObjectNo ) { this.firstObjectNo = firstObjectNo; }

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
    public static final long serialVersionUID = 2009082673;    

    // yidl.runtime.Object
    public int getTag() { return 2009082673; }
    public String getTypeName() { return "org::xtreemfs::interfaces::ObjectList"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( set != null ? ( ( set.remaining() % 4 == 0 ) ? set.remaining() : ( set.remaining() + 4 - set.remaining() % 4 ) ) : 0 ); // set
        my_size += Integer.SIZE / 8; // stripeWidth
        my_size += Integer.SIZE / 8; // firstObjectNo
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeBuffer( "set", set );
        marshaller.writeUint32( "stripeWidth", stripeWidth );
        marshaller.writeUint32( "firstObjectNo", firstObjectNo );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        set = ( ReusableBuffer )unmarshaller.readBuffer( "set" );
        stripeWidth = unmarshaller.readUint32( "stripeWidth" );
        firstObjectNo = unmarshaller.readUint32( "firstObjectNo" );    
    }
        
    

    private ReusableBuffer set;
    private int stripeWidth;
    private int firstObjectNo;    

}

