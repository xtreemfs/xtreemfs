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
    public static final int TAG = 2010012168;
    
    public ObjectList() {  }
    public ObjectList( ReusableBuffer set, int stripe_width, int first_ ) { this.set = set; this.stripe_width = stripe_width; this.first_ = first_; }

    public ReusableBuffer getSet() { return set; }
    public void setSet( ReusableBuffer set ) { this.set = set; }
    public int getStripe_width() { return stripe_width; }
    public void setStripe_width( int stripe_width ) { this.stripe_width = stripe_width; }
    public int getFirst_() { return first_; }
    public void setFirst_( int first_ ) { this.first_ = first_; }

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
    public static final long serialVersionUID = 2010012168;    

    // yidl.runtime.Object
    public int getTag() { return 2010012168; }
    public String getTypeName() { return "org::xtreemfs::interfaces::ObjectList"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( set != null ? ( ( set.remaining() % 4 == 0 ) ? set.remaining() : ( set.remaining() + 4 - set.remaining() % 4 ) ) : 0 ); // set
        my_size += Integer.SIZE / 8; // stripe_width
        my_size += Integer.SIZE / 8; // first_
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeBuffer( "set", set );
        marshaller.writeUint32( "stripe_width", stripe_width );
        marshaller.writeUint32( "first_", first_ );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        set = ( ReusableBuffer )unmarshaller.readBuffer( "set" );
        stripe_width = unmarshaller.readUint32( "stripe_width" );
        first_ = unmarshaller.readUint32( "first_" );    
    }
        
    

    private ReusableBuffer set;
    private int stripe_width;
    private int first_;    

}

