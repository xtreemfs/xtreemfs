package org.xtreemfs.interfaces;

import java.util.Iterator;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Sequence;
import yidl.Struct;
import yidl.Unmarshaller;




public class NewFileSizeSet extends Sequence<NewFileSize>
{
    public NewFileSizeSet() { }

    // yidl.Object
    public int getTag() { return 2009082630; }
    public String getTypeName() { return "org::xtreemfs::interfaces::NewFileSizeSet"; }

    public int getXDRSize() {
        int my_size = Integer.SIZE/8;
        for ( Iterator<NewFileSize> i = iterator(); i.hasNext(); ) {
            NewFileSize value = i.next();
            my_size += value.getXDRSize();
        }
        return my_size;
    }
    
    public void marshal( Marshaller marshaller )
    {
        for ( Iterator<NewFileSize> i = iterator(); i.hasNext(); )
            marshaller.writeStruct( "value", i.next() );;
    }
    
    public void unmarshal( Unmarshaller unmarshaller )
    {
        NewFileSize value; 
        value = new NewFileSize(); unmarshaller.readStruct( "value", value );
        this.add( value );    
    }
        

}

