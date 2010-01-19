package org.xtreemfs.interfaces;

import java.io.StringWriter;
import java.util.Iterator;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Sequence;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class OSDtoMRCDataSet extends Sequence<OSDtoMRCData>
{
    public OSDtoMRCDataSet() { }

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
    public int getTag() { return 2010011925; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDtoMRCDataSet"; }

    public int getXDRSize() 
    {
        int my_size = 4; // Length of the sequence
        
        for ( Iterator<OSDtoMRCData> i = iterator(); i.hasNext(); ) 
        {
            OSDtoMRCData value = i.next();
            my_size += value.getXDRSize(); // Size of value
        }
        
        return my_size;
    }
    
    public void marshal( Marshaller marshaller )
    {
        for ( Iterator<OSDtoMRCData> i = iterator(); i.hasNext(); )
            marshaller.writeStruct( "value", i.next() );;
    }
    
    public void unmarshal( Unmarshaller unmarshaller )
    {
        OSDtoMRCData value; 
        value = new OSDtoMRCData(); unmarshaller.readStruct( "value", value );
        this.add( value );    
    }
        

}

