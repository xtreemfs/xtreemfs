package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_broadcast_gmaxRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082938;
    
    public xtreemfs_broadcast_gmaxRequest() {  }
    public xtreemfs_broadcast_gmaxRequest( String file_id, long truncateEpoch, long lastObject, long fileSize ) { this.file_id = file_id; this.truncateEpoch = truncateEpoch; this.lastObject = lastObject; this.fileSize = fileSize; }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public long getTruncateEpoch() { return truncateEpoch; }
    public void setTruncateEpoch( long truncateEpoch ) { this.truncateEpoch = truncateEpoch; }
    public long getLastObject() { return lastObject; }
    public void setLastObject( long lastObject ) { this.lastObject = lastObject; }
    public long getFileSize() { return fileSize; }
    public void setFileSize( long fileSize ) { this.fileSize = fileSize; }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_broadcast_gmaxResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082938;    

    // yidl.Object
    public int getTag() { return 2009082938; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_broadcast_gmaxRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += file_id != null ? ( ( file_id.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( file_id.getBytes().length + Integer.SIZE/8 ) : ( file_id.getBytes().length + Integer.SIZE/8 + 4 - ( file_id.getBytes().length + Integer.SIZE/8 ) % 4 ) : 0;
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "file_id", file_id );
        marshaller.writeUint64( "truncateEpoch", truncateEpoch );
        marshaller.writeUint64( "lastObject", lastObject );
        marshaller.writeUint64( "fileSize", fileSize );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        file_id = unmarshaller.readString( "file_id" );
        truncateEpoch = unmarshaller.readUint64( "truncateEpoch" );
        lastObject = unmarshaller.readUint64( "lastObject" );
        fileSize = unmarshaller.readUint64( "fileSize" );    
    }
        
    

    private String file_id;
    private long truncateEpoch;
    private long lastObject;
    private long fileSize;    

}

