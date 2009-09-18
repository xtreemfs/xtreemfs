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




public class AddressMappingSet extends Sequence<AddressMapping>
{
    public AddressMappingSet() { }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeSequence( "", this );
        return string_writer.toString();
    }


    // yidl.Object
    public int getTag() { return 2009082649; }
    public String getTypeName() { return "org::xtreemfs::interfaces::AddressMappingSet"; }

    public int getXDRSize() 
    {
        int my_size = 4; // Length of the sequence
        
        for ( Iterator<AddressMapping> i = iterator(); i.hasNext(); ) 
        {
            AddressMapping value = i.next();
            my_size += value.getXDRSize(); // Size of value
        }
        
        return my_size;
    }
    
    public void marshal( Marshaller marshaller )
    {
        for ( Iterator<AddressMapping> i = iterator(); i.hasNext(); )
            marshaller.writeStruct( "value", i.next() );;
    }
    
    public void unmarshal( Unmarshaller unmarshaller )
    {
        AddressMapping value; 
        value = new AddressMapping(); unmarshaller.readStruct( "value", value );
        this.add( value );    
    }
        

}

