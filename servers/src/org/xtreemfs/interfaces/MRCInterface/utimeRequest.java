package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class utimeRequest implements org.xtreemfs.interfaces.utils.Request
{
    public utimeRequest() { context = new Context(); path = ""; ctime = 0; atime = 0; mtime = 0; }
    public utimeRequest( Context context, String path, long ctime, long atime, long mtime ) { this.context = context; this.path = path; this.ctime = ctime; this.atime = atime; this.mtime = mtime; }
    public utimeRequest( Object from_hash_map ) { context = new Context(); path = ""; ctime = 0; atime = 0; mtime = 0; this.deserialize( from_hash_map ); }
    public utimeRequest( Object[] from_array ) { context = new Context(); path = ""; ctime = 0; atime = 0; mtime = 0;this.deserialize( from_array ); }

    public Context getContext() { return context; }
    public void setContext( Context context ) { this.context = context; }
    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public long getCtime() { return ctime; }
    public void setCtime( long ctime ) { this.ctime = ctime; }
    public long getAtime() { return atime; }
    public void setAtime( long atime ) { this.atime = atime; }
    public long getMtime() { return mtime; }
    public void setMtime( long mtime ) { this.mtime = mtime; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::utimeRequest"; }    
    public long getTypeId() { return 22; }

    public String toString()
    {
        return "utimeRequest( " + context.toString() + ", " + "\"" + path + "\"" + ", " + Long.toString( ctime ) + ", " + Long.toString( atime ) + ", " + Long.toString( mtime ) + " )"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.context.deserialize( from_hash_map.get( "context" ) );
        this.path = ( String )from_hash_map.get( "path" );
        this.ctime = ( ( Long )from_hash_map.get( "ctime" ) ).longValue();
        this.atime = ( ( Long )from_hash_map.get( "atime" ) ).longValue();
        this.mtime = ( ( Long )from_hash_map.get( "mtime" ) ).longValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.context.deserialize( from_array[0] );
        this.path = ( String )from_array[1];
        this.ctime = ( ( Long )from_array[2] ).longValue();
        this.atime = ( ( Long )from_array[3] ).longValue();
        this.mtime = ( ( Long )from_array[4] ).longValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        context = new Context(); context.deserialize( buf );
        path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        ctime = buf.getLong();
        atime = buf.getLong();
        mtime = buf.getLong();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "context", context.serialize() );
        to_hash_map.put( "path", path );
        to_hash_map.put( "ctime", new Long( ctime ) );
        to_hash_map.put( "atime", new Long( atime ) );
        to_hash_map.put( "mtime", new Long( mtime ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        context.serialize( writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( path, writer );
        writer.putLong( ctime );
        writer.putLong( atime );
        writer.putLong( mtime );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += context.calculateSize();
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(path);
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 22; }
    public Response createDefaultResponse() { return new utimeResponse(); }


    private Context context;
    private String path;
    private long ctime;
    private long atime;
    private long mtime;

}

