package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class statfs_ implements Serializable
{
    public statfs_() { bsize = 0; bfree = 0; fsid = ""; namelen = 0; }
    public statfs_( long bsize, long bfree, String fsid, long namelen ) { this.bsize = bsize; this.bfree = bfree; this.fsid = fsid; this.namelen = namelen; }


    // Object
    public String toString()
    {
        return "statfs_( " + Long.toString( bsize ) + ", " + Long.toString( bfree ) + ", " + "\"" + fsid + "\"" + ", " + Long.toString( namelen ) + " )";
    }    

    // Serializable
    public void serialize(ONCRPCBufferWriter writer) {
        writer.putLong( bsize );
        writer.putLong( bfree );
        { final byte[] bytes = fsid.getBytes(); writer.putInt( bytes.length ); writer.put( bytes );  if (bytes.length % 4 > 0) {for (int k = 0; k < (4 - (bytes.length % 4)); k++) { writer.put((byte)0); } }}
        writer.putLong( namelen );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        bsize = buf.getLong();
        bfree = buf.getLong();
        { int fsid_new_length = buf.getInt(); byte[] fsid_new_bytes = new byte[fsid_new_length]; buf.get( fsid_new_bytes ); fsid = new String( fsid_new_bytes ); if (fsid_new_length % 4 > 0) {for (int k = 0; k < (4 - (fsid_new_length % 4)); k++) { buf.get(); } } }
        namelen = buf.getLong();    
    }
    
    public int getSize()
    {
        int my_size = 0;
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += 4 + ( fsid.length() + 4 - ( fsid.length() % 4 ) );
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }

    public long bsize;
    public long bfree;
    public String fsid;
    public long namelen;

}

