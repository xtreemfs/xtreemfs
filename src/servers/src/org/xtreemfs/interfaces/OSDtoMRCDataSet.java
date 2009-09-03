package org.xtreemfs.interfaces;

import java.util.Iterator;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Sequence;
import yidl.Struct;
import yidl.Unmarshaller;




public class OSDtoMRCDataSet extends Sequence<OSDtoMRCData>
{
    public OSDtoMRCDataSet() { }

    // yidl.Object
    public int getTag() { return 2009082632; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDtoMRCDataSet"; }

    public int getXDRSize() {
        int my_size = Integer.SIZE/8;
        for ( Iterator<OSDtoMRCData> i = iterator(); i.hasNext(); ) {
            OSDtoMRCData value = i.next();
            my_size += value.getXDRSize();
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

