package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
public class OSDsByStripingPolicySet extends ArrayList<OSDsByStripingPolicy>
{    
    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::OSDsByStripingPolicySet"; }
    
    public void serialize(ONCRPCBufferWriter writer) {
        writer.putInt( size() );
        for ( Iterator<OSDsByStripingPolicy> i = iterator(); i.hasNext(); )
        {
            OSDsByStripingPolicy next_value = i.next();        
            next_value.serialize( writer );;
        }
    }

    public void deserialize( ReusableBuffer buf ) {
        int new_size = buf.getInt();
        for ( int i = 0; i < new_size; i++ )
        {
            OSDsByStripingPolicy new_value; new_value = new org.xtreemfs.interfaces.OSDsByStripingPolicy(); new_value.deserialize( buf );;
            this.add( new_value );
        }
    }
    
    public int calculateSize() {
        int my_size = Integer.SIZE/8;
        for ( Iterator<OSDsByStripingPolicy> i = iterator(); i.hasNext(); ) {
            OSDsByStripingPolicy next_value = i.next();
            my_size += next_value.calculateSize();
        }
        return my_size;
    }
}

