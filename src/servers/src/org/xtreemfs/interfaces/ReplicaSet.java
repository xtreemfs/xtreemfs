package org.xtreemfs.interfaces;

import java.util.Iterator;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Sequence;
import yidl.Struct;
import yidl.Unmarshaller;




public class ReplicaSet extends Sequence<Replica>
{
    public ReplicaSet() { }

    // yidl.Object
    public int getTag() { return 2009090230; }
    public String getTypeName() { return "org::xtreemfs::interfaces::ReplicaSet"; }

    public int getXDRSize() {
        int my_size = Integer.SIZE/8;
        for ( Iterator<Replica> i = iterator(); i.hasNext(); ) {
            Replica value = i.next();
            my_size += value.getXDRSize();
        }
        return my_size;
    }
    
    public void marshal( Marshaller marshaller )
    {
        for ( Iterator<Replica> i = iterator(); i.hasNext(); )
            marshaller.writeStruct( "value", i.next() );;
    }
    
    public void unmarshal( Unmarshaller unmarshaller )
    {
        Replica value; 
        value = new Replica(); unmarshaller.readStruct( "value", value );
        this.add( value );    
    }
        

}

