package org.xtreemfs.interfaces;

import java.util.Iterator;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Sequence;
import yidl.Struct;
import yidl.Unmarshaller;




public class AddressMappingSet extends Sequence<AddressMapping>
{
    public AddressMappingSet() { }

    // yidl.Object
    public int getTag() { return 2009082649; }
    public String getTypeName() { return "org::xtreemfs::interfaces::AddressMappingSet"; }

    public int getXDRSize() {
        int my_size = Integer.SIZE/8;
        for ( Iterator<AddressMapping> i = iterator(); i.hasNext(); ) {
            AddressMapping value = i.next();
            my_size += value.getXDRSize();
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

