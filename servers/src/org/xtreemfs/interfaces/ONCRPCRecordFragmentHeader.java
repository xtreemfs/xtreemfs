package org.xtreemfs.interfaces;

import java.nio.ByteBuffer;


public class ONCRPCRecordFragmentHeader implements Serializable
{
    public ONCRPCRecordFragmentHeader()
    {
        length = 0;
        is_last = true;
    }

    public int getRecordFragmentLength() { return length; }
    public boolean isLastRecordFragment() { return is_last; }
    
    // Serializable
    public void serialize( ByteBuffer buf )
    {
        int record_fragment_marker = length = this.getSize();
        if ( is_last ) record_fragment_marker |= ( 1 << 31 );
        buf.putInt( record_fragment_marker );
    }
    
    public void deserialize( ByteBuffer buf )
    {
        int record_fragment_marker = buf.getInt(); 
        is_last = ( record_fragment_marker << 31 ) != 0;
        length = record_fragment_marker ^ ( 1 << 31 );
        System.out.println( "Length " + Integer.toString( length ) );
        if ( is_last )
            System.out.println( "Last" );
    }

    public int getSize()
    {
        return 4;
    }

    private int length;
    private boolean is_last;
};
