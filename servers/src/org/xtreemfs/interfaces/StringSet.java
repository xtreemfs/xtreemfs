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
        writer.putInt( size() );
        for ( Iterator<String> i = iterator(); i.hasNext(); )
        {
            String next_value = i.next();        
            { final byte[] bytes = next_value.getBytes(); writer.putInt( bytes.length ); writer.put( bytes );  if (bytes.length % 4 > 0) {for (int k = 0; k < (4 - (bytes.length % 4)); k++) { writer.put((byte)0); } }};
        }
    }

    public void deserialize( ReusableBuffer buf ) {
        int new_size = buf.getInt();
        for ( int i = 0; i < new_size; i++ )
        {
            String new_value; { int new_value_new_length = buf.getInt(); byte[] new_value_new_bytes = new byte[new_value_new_length]; buf.get( new_value_new_bytes ); new_value = new String( new_value_new_bytes ); if (new_value_new_length % 4 > 0) {for (int k = 0; k < (4 - (new_value_new_length % 4)); k++) { buf.get(); } } };
            this.add( new_value );
        }
    }
    
    public int calculateSize() {
        int my_size = Integer.SIZE/8;
        for ( Iterator<String> i = iterator(); i.hasNext(); ) {
            String next_value = i.next();
            my_size += 4 + ( next_value.length() + 4 - ( next_value.length() % 4 ) );
        }
        return my_size;
    }
}

