package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
public class StringSet extends ArrayList<String>
{    
    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::StringSet"; }
    
    public void serialize(ONCRPCBufferWriter writer) {
        if (this.size() > org.xtreemfs.interfaces.utils.XDRUtils.MAX_ARRAY_ELEMS)
	    throw new IllegalArgumentException("array is too large ("+this.size()+")");
        writer.putInt( size() );
        for ( Iterator<String> i = iterator(); i.hasNext(); )
        {
            String next_value = i.next();        
            { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(next_value,writer); };
        }
    }

    public void deserialize( ReusableBuffer buf ) {
        int new_size = buf.getInt();
	if (new_size > org.xtreemfs.interfaces.utils.XDRUtils.MAX_ARRAY_ELEMS)
	    throw new IllegalArgumentException("array is too large ("+this.size()+")");
        for ( int i = 0; i < new_size; i++ )
        {
            String new_value; { new_value = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); };
            this.add( new_value );
        }
    }
    
    public int calculateSize() {
        int my_size = Integer.SIZE/8;
        for ( Iterator<String> i = iterator(); i.hasNext(); ) {
            String next_value = i.next();
            my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(next_value);
        }
        return my_size;
    }
}

