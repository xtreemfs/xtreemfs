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
    public StripingPolicy( int policy, int stripe_size, int width ) { this.policy = policy; this.stripe_size = stripe_size; this.width = width; }

    public int getPolicy() { return policy; }
    public void setPolicy( int policy ) { this.policy = policy; }
    public int getStripe_size() { return stripe_size; }
    public void setStripe_size( int stripe_size ) { this.stripe_size = stripe_size; }
    public int getWidth() { return width; }
    public void setWidth( int width ) { this.width = width; }

    // Object
    public String toString()
    {
        return "StripingPolicy( " + Integer.toString( policy ) + ", " + Integer.toString( stripe_size ) + ", " + Integer.toString( width ) + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::StripingPolicy"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        writer.putInt( policy );
        writer.putInt( stripe_size );
        writer.putInt( width );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        policy = buf.getInt();
        stripe_size = buf.getInt();
        width = buf.getInt();    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }

    private int policy;
    private int stripe_size;
    private int width;

}

