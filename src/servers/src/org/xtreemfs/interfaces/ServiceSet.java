package org.xtreemfs.interfaces;

import java.util.Iterator;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Sequence;
import yidl.Struct;
import yidl.Unmarshaller;




public class ServiceSet extends Sequence<Service>
{
    public ServiceSet() { }

    // yidl.Object
    public int getTag() { return 2009082653; }
    public String getTypeName() { return "org::xtreemfs::interfaces::ServiceSet"; }

    public int getXDRSize() 
    {
        int my_size = 4; // Length of the sequence
        
        for ( Iterator<Service> i = iterator(); i.hasNext(); ) 
        {
            Service value = i.next();
            my_size += value.getXDRSize(); // Size of value
        }
        
        return my_size;
    }
    
    public void marshal( Marshaller marshaller )
    {
        for ( Iterator<Service> i = iterator(); i.hasNext(); )
            marshaller.writeStruct( "value", i.next() );;
    }
    
    public void unmarshal( Unmarshaller unmarshaller )
    {
        Service value; 
        value = new Service(); unmarshaller.readStruct( "value", value );
        this.add( value );    
    }
        

}

