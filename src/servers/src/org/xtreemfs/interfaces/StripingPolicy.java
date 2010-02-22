package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class StripingPolicy implements Struct
{
    public static final int TAG = 2010022234;

    public StripingPolicy() { type = StripingPolicyType.STRIPING_POLICY_RAID0;  }
    public StripingPolicy( StripingPolicyType type, int stripe_size, int width ) { this.type = type; this.stripe_size = stripe_size; this.width = width; }

    public StripingPolicyType getType() { return type; }
    public void setType( StripingPolicyType type ) { this.type = type; }
    public int getStripe_size() { return stripe_size; }
    public void setStripe_size( int stripe_size ) { this.stripe_size = stripe_size; }
    public int getWidth() { return width; }
    public void setWidth( int width ) { this.width = width; }

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
    public static final long serialVersionUID = 2010022234;

    // yidl.runtime.Object
    public int getTag() { return 2010022234; }
    public String getTypeName() { return "org::xtreemfs::interfaces::StripingPolicy"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8; // type
        my_size += Integer.SIZE / 8; // stripe_size
        my_size += Integer.SIZE / 8; // width
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeInt32( type, type.intValue() );
        marshaller.writeUint32( "stripe_size", stripe_size );
        marshaller.writeUint32( "width", width );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        type = StripingPolicyType.parseInt( unmarshaller.readInt32( "type" ) );
        stripe_size = unmarshaller.readUint32( "stripe_size" );
        width = unmarshaller.readUint32( "width" );
    }

    

    private StripingPolicyType type;
    private int stripe_size;
    private int width;

}

