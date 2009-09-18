package org.xtreemfs.interfaces;

import java.io.StringWriter;
import java.util.Iterator;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.PrettyPrinter;
import yidl.Sequence;
import yidl.Struct;
import yidl.Unmarshaller;




public class VolumeSet extends Sequence<Volume>
{
    public VolumeSet() { }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeSequence( "", this );
        return string_writer.toString();
    }


    // yidl.Object
    public int getTag() { return 2009090254; }
    public String getTypeName() { return "org::xtreemfs::interfaces::VolumeSet"; }

    public int getXDRSize() 
    {
        int my_size = 4; // Length of the sequence
        
        for ( Iterator<Volume> i = iterator(); i.hasNext(); ) 
        {
            Volume value = i.next();
            my_size += value.getXDRSize(); // Size of value
        }
        
        return my_size;
    }
    
    public void marshal( Marshaller marshaller )
    {
        for ( Iterator<Volume> i = iterator(); i.hasNext(); )
            marshaller.writeStruct( "value", i.next() );;
    }
    
    public void unmarshal( Unmarshaller unmarshaller )
    {
        Volume value; 
        value = new Volume(); unmarshaller.readStruct( "value", value );
        this.add( value );    
    }
        

}

