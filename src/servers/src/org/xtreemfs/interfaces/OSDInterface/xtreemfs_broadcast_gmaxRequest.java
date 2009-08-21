package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_broadcast_gmaxRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 1320;

    
    public xtreemfs_broadcast_gmaxRequest() { file_id = ""; truncateEpoch = 0; lastObject = 0; fileSize = 0; }
    public xtreemfs_broadcast_gmaxRequest( String file_id, long truncateEpoch, long lastObject, long fileSize ) { this.file_id = file_id; this.truncateEpoch = truncateEpoch; this.lastObject = lastObject; this.fileSize = fileSize; }
    public xtreemfs_broadcast_gmaxRequest( Object from_hash_map ) { file_id = ""; truncateEpoch = 0; lastObject = 0; fileSize = 0; this.deserialize( from_hash_map ); }
    public xtreemfs_broadcast_gmaxRequest( Object[] from_array ) { file_id = ""; truncateEpoch = 0; lastObject = 0; fileSize = 0;this.deserialize( from_array ); }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public long getTruncateEpoch() { return truncateEpoch; }
    public void setTruncateEpoch( long truncateEpoch ) { this.truncateEpoch = truncateEpoch; }
    public long getLastObject() { return lastObject; }
    public void setLastObject( long lastObject ) { this.lastObject = lastObject; }
    public long getFileSize() { return fileSize; }
    public void setFileSize( long fileSize ) { this.fileSize = fileSize; }

    // Object
    public String toString()
    {
        return "xtreemfs_broadcast_gmaxRequest( " + "\"" + file_id + "\"" + ", " + Long.toString( truncateEpoch ) + ", " + Long.toString( lastObject ) + ", " + Long.toString( fileSize ) + " )";
    }

    // Serializable
    public int getTag() { return 1320; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_broadcast_gmaxRequest"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.file_id = ( String )from_hash_map.get( "file_id" );
        this.truncateEpoch = ( ( Long )from_hash_map.get( "truncateEpoch" ) ).longValue();
        this.lastObject = ( ( Long )from_hash_map.get( "lastObject" ) ).longValue();
        this.fileSize = ( ( Long )from_hash_map.get( "fileSize" ) ).longValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.file_id = ( String )from_array[0];
        this.truncateEpoch = ( ( Long )from_array[1] ).longValue();
        this.lastObject = ( ( Long )from_array[2] ).longValue();
        this.fileSize = ( ( Long )from_array[3] ).longValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        truncateEpoch = buf.getLong();
        lastObject = buf.getLong();
        fileSize = buf.getLong();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "file_id", file_id );
        to_hash_map.put( "truncateEpoch", new Long( truncateEpoch ) );
        to_hash_map.put( "lastObject", new Long( lastObject ) );
        to_hash_map.put( "fileSize", new Long( fileSize ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( file_id, writer );
        writer.putLong( truncateEpoch );
        writer.putLong( lastObject );
        writer.putLong( fileSize );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_broadcast_gmaxResponse(); }


    private String file_id;
    private long truncateEpoch;
    private long lastObject;
    private long fileSize;    

}

