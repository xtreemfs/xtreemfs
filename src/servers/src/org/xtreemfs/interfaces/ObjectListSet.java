package org.xtreemfs.interfaces;

import java.util.Iterator;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Sequence;
import yidl.Struct;
import yidl.Unmarshaller;




public class ObjectListSet extends Sequence<ObjectList>
{
    public ObjectListSet() { }

    // yidl.Object
    public int getTag() { return 2009082674; }
    public String getTypeName() { return "org::xtreemfs::interfaces::ObjectListSet"; }

    public int getXDRSize() {
        int my_size = Integer.SIZE/8;
        for ( Iterator<ObjectList> i = iterator(); i.hasNext(); ) {
            ObjectList value = i.next();
            my_size += value.getXDRSize();
        }
        return my_size;
    }
    
    public void marshal( Marshaller marshaller )
    {
        for ( Iterator<ObjectList> i = iterator(); i.hasNext(); )
            marshaller.writeStruct( "value", i.next() );;
    }
    
    public void unmarshal( Unmarshaller unmarshaller )
    {
        ObjectList value; 
        value = new ObjectList(); unmarshaller.readStruct( "value", value );
        this.add( value );    
    }
        

}

