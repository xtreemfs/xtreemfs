package org.xtreemfs.interfaces;

import java.nio.ByteBuffer;


public class ONCRPCResponseHeader extends ONCRPCRecordFragmentHeader
{    
    public ONCRPCResponseHeader()
    {
        xid = 0;        
    }
    
    public ONCRPCResponseHeader( int xid )
    {
        this.xid = xid;
    }
    
    public int getXID() { return xid; }
        
    // Serializable    
    public void serialize( ByteBuffer buf )
    {
        super.serialize( buf );
        buf.putInt( xid );
        buf.putInt( 0 ); // reply_stat MSG_ACCEPTED
        buf.putInt( 0 ); // accept_stat SUCCESS        
    }
    
    public void deserialize( ByteBuffer buf )
    {        
        super.deserialize( buf );
    }

    public int getSize()
    {
        return super.getSize() + 4;
    }

    
    private int xid;
}
