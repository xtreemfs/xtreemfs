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




public class DirectoryEntrySet extends Sequence<DirectoryEntry>
{
    public DirectoryEntrySet() { }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeSequence( "", this );
        return string_writer.toString();
    }


    // yidl.Object
    public int getTag() { return 2009090251; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DirectoryEntrySet"; }

    public int getXDRSize() 
    {
        int my_size = 4; // Length of the sequence
        
        for ( Iterator<DirectoryEntry> i = iterator(); i.hasNext(); ) 
        {
            DirectoryEntry value = i.next();
            my_size += value.getXDRSize(); // Size of value
        }
        
        return my_size;
    }
    
    public void marshal( Marshaller marshaller )
    {
        for ( Iterator<DirectoryEntry> i = iterator(); i.hasNext(); )
            marshaller.writeStruct( "value", i.next() );;
    }
    
    public void unmarshal( Unmarshaller unmarshaller )
    {
        DirectoryEntry value; 
        value = new DirectoryEntry(); unmarshaller.readStruct( "value", value );
        this.add( value );    
    }
        

}

