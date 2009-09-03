package org.xtreemfs.interfaces;

import java.util.Iterator;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Sequence;
import yidl.Struct;
import yidl.Unmarshaller;




public class DirectoryEntrySet extends Sequence<DirectoryEntry>
{
    public DirectoryEntrySet() { }

    // yidl.Object
    public int getTag() { return 2009082660; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DirectoryEntrySet"; }

    public int getXDRSize() {
        int my_size = Integer.SIZE/8;
        for ( Iterator<DirectoryEntry> i = iterator(); i.hasNext(); ) {
            DirectoryEntry value = i.next();
            my_size += value.getXDRSize();
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

