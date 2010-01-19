package org.xtreemfs.interfaces;

import java.io.StringWriter;
import java.util.Iterator;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Sequence;
import yidl.runtime.Unmarshaller;




public class StringSet extends Sequence<String>
{
    public StringSet() { }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        string_writer.append(this.getClass().getCanonicalName());
        string_writer.append(" ");
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeSequence( "", this );
        return string_writer.toString();
    }


    // yidl.runtime.Object
    public int getTag() { return 2010011912; }
    public String getTypeName() { return "org::xtreemfs::interfaces::StringSet"; }

    public int getXDRSize() 
    {
        int my_size = 4; // Length of the sequence
        
        for ( Iterator<String> i = iterator(); i.hasNext(); ) 
        {
            String value = i.next();
            my_size += Integer.SIZE / 8 + ( value != null ? ( ( value.getBytes().length % 4 == 0 ) ? value.getBytes().length : ( value.getBytes().length + 4 - value.getBytes().length % 4 ) ) : 0 ); // Size of value
        }
        
        return my_size;
    }
    
    public void marshal( Marshaller marshaller )
    {
        for ( Iterator<String> i = iterator(); i.hasNext(); )
            marshaller.writeString( "value", i.next() );;
    }
    
    public void unmarshal( Unmarshaller unmarshaller )
    {
        String value; 
        value = unmarshaller.readString( "value" );
        this.add( value );    
    }
        

}

