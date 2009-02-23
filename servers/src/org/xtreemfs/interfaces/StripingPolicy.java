package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class StripingPolicy implements Serializable
{
    public StripingPolicy() { policy = 0; stripe_size = 0; width = 0; }
    public StripingPolicy( long policy, long stripe_size, long width ) { this.policy = policy; this.stripe_size = stripe_size; this.width = width; }


    // Object
    public String toString()
    {
        return "StripingPolicy( " + Long.toString( policy ) + ", " + Long.toString( stripe_size ) + ", " + Long.toString( width ) + " )";
    }    

    // Serializable
    public void serialize(ONCRPCBufferWriter writer) {
        writer.putLong( policy );
        writer.putLong( stripe_size );
        writer.putLong( width );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        policy = buf.getLong();
        stripe_size = buf.getLong();
        width = buf.getLong();    
    }
    
    public int getSize()
    {
        int my_size = 0;
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }

    public long policy;
    public long stripe_size;
    public long width;

}

