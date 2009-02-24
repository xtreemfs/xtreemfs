package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class StripingPolicy implements org.xtreemfs.interfaces.utils.Serializable
{
    public StripingPolicy() { policy = 0; stripe_size = 0; width = 0; }
    public StripingPolicy( int policy, long stripe_size, long width ) { this.policy = policy; this.stripe_size = stripe_size; this.width = width; }

    public int getPolicy() { return policy; }
    public void setPolicy( int policy ) { this.policy = policy; }
    public long getStripe_size() { return stripe_size; }
    public void setStripe_size( long stripe_size ) { this.stripe_size = stripe_size; }
    public long getWidth() { return width; }
    public void setWidth( long width ) { this.width = width; }

    // Object
    public String toString()
    {
        return "StripingPolicy( " + Integer.toString( policy ) + ", " + Long.toString( stripe_size ) + ", " + Long.toString( width ) + " )";
    }    

    // Serializable
    public void serialize(ONCRPCBufferWriter writer) {
        writer.putInt( policy );
        writer.putLong( stripe_size );
        writer.putLong( width );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        policy = buf.getInt();
        stripe_size = buf.getLong();
        width = buf.getLong();    
    }
    
    public int getSize()
    {
        int my_size = 0;
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }

    private int policy;
    private long stripe_size;
    private long width;

}

