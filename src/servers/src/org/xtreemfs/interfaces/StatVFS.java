package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class StatVFS implements org.xtreemfs.interfaces.utils.Serializable
{
    public static final int TAG = 1043;

    
    public StatVFS() { bsize = 0; bfree = 0; fsid = ""; namelen = 0; }
    public StatVFS( int bsize, long bfree, String fsid, int namelen ) { this.bsize = bsize; this.bfree = bfree; this.fsid = fsid; this.namelen = namelen; }
    public StatVFS( Object from_hash_map ) { bsize = 0; bfree = 0; fsid = ""; namelen = 0; this.deserialize( from_hash_map ); }
    public StatVFS( Object[] from_array ) { bsize = 0; bfree = 0; fsid = ""; namelen = 0;this.deserialize( from_array ); }

    public int getBsize() { return bsize; }
    public void setBsize( int bsize ) { this.bsize = bsize; }
    public long getBfree() { return bfree; }
    public void setBfree( long bfree ) { this.bfree = bfree; }
    public String getFsid() { return fsid; }
    public void setFsid( String fsid ) { this.fsid = fsid; }
    public int getNamelen() { return namelen; }
    public void setNamelen( int namelen ) { this.namelen = namelen; }

    // Object
    public String toString()
    {
        return "StatVFS( " + Integer.toString( bsize ) + ", " + Long.toString( bfree ) + ", " + "\"" + fsid + "\"" + ", " + Integer.toString( namelen ) + " )";
    }

    // Serializable
    public int getTag() { return 1043; }
    public String getTypeName() { return "org::xtreemfs::interfaces::StatVFS"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.bsize = ( from_hash_map.get( "bsize" ) instanceof Integer ) ? ( ( Integer )from_hash_map.get( "bsize" ) ).intValue() : ( ( Long )from_hash_map.get( "bsize" ) ).intValue();
        this.bfree = ( from_hash_map.get( "bfree" ) instanceof Integer ) ? ( ( Integer )from_hash_map.get( "bfree" ) ).longValue() : ( ( Long )from_hash_map.get( "bfree" ) ).longValue();
        this.fsid = ( String )from_hash_map.get( "fsid" );
        this.namelen = ( from_hash_map.get( "namelen" ) instanceof Integer ) ? ( ( Integer )from_hash_map.get( "namelen" ) ).intValue() : ( ( Long )from_hash_map.get( "namelen" ) ).intValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.bsize = ( from_array[0] instanceof Integer ) ? ( ( Integer )from_array[0] ).intValue() : ( ( Long )from_array[0] ).intValue();
        this.bfree = ( from_array[1] instanceof Integer ) ? ( ( Integer )from_array[1] ).longValue() : ( ( Long )from_array[1] ).longValue();
        this.fsid = ( String )from_array[2];
        this.namelen = ( from_array[3] instanceof Integer ) ? ( ( Integer )from_array[3] ).intValue() : ( ( Long )from_array[3] ).intValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        bsize = buf.getInt();
        bfree = buf.getLong();
        fsid = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        namelen = buf.getInt();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "bsize", new Integer( bsize ) );
        to_hash_map.put( "bfree", new Long( bfree ) );
        to_hash_map.put( "fsid", fsid );
        to_hash_map.put( "namelen", new Integer( namelen ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        writer.putInt( bsize );
        writer.putLong( bfree );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( fsid, writer );
        writer.putInt( namelen );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(fsid);
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }


    private int bsize;
    private long bfree;
    private String fsid;
    private int namelen;    

}

