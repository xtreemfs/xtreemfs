package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class utimensRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082839;

    
    public utimensRequest() { path = ""; atime_ns = 0; mtime_ns = 0; ctime_ns = 0; }
    public utimensRequest( String path, long atime_ns, long mtime_ns, long ctime_ns ) { this.path = path; this.atime_ns = atime_ns; this.mtime_ns = mtime_ns; this.ctime_ns = ctime_ns; }
    public utimensRequest( Object from_hash_map ) { path = ""; atime_ns = 0; mtime_ns = 0; ctime_ns = 0; this.deserialize( from_hash_map ); }
    public utimensRequest( Object[] from_array ) { path = ""; atime_ns = 0; mtime_ns = 0; ctime_ns = 0;this.deserialize( from_array ); }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public long getAtime_ns() { return atime_ns; }
    public void setAtime_ns( long atime_ns ) { this.atime_ns = atime_ns; }
    public long getMtime_ns() { return mtime_ns; }
    public void setMtime_ns( long mtime_ns ) { this.mtime_ns = mtime_ns; }
    public long getCtime_ns() { return ctime_ns; }
    public void setCtime_ns( long ctime_ns ) { this.ctime_ns = ctime_ns; }

    // Object
    public String toString()
    {
        return "utimensRequest( " + "\"" + path + "\"" + ", " + Long.toString( atime_ns ) + ", " + Long.toString( mtime_ns ) + ", " + Long.toString( ctime_ns ) + " )";
    }

    // Serializable
    public int getTag() { return 2009082839; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::utimensRequest"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.path = ( String )from_hash_map.get( "path" );
        this.atime_ns = ( ( Long )from_hash_map.get( "atime_ns" ) ).longValue();
        this.mtime_ns = ( ( Long )from_hash_map.get( "mtime_ns" ) ).longValue();
        this.ctime_ns = ( ( Long )from_hash_map.get( "ctime_ns" ) ).longValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.path = ( String )from_array[0];
        this.atime_ns = ( ( Long )from_array[1] ).longValue();
        this.mtime_ns = ( ( Long )from_array[2] ).longValue();
        this.ctime_ns = ( ( Long )from_array[3] ).longValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        atime_ns = buf.getLong();
        mtime_ns = buf.getLong();
        ctime_ns = buf.getLong();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "path", path );
        to_hash_map.put( "atime_ns", new Long( atime_ns ) );
        to_hash_map.put( "mtime_ns", new Long( mtime_ns ) );
        to_hash_map.put( "ctime_ns", new Long( ctime_ns ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( path, writer );
        writer.putLong( atime_ns );
        writer.putLong( mtime_ns );
        writer.putLong( ctime_ns );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(path);
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }

    // Request
    public Response createDefaultResponse() { return new utimensResponse(); }


    private String path;
    private long atime_ns;
    private long mtime_ns;
    private long ctime_ns;    

}

