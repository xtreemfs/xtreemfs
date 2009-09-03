package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_lock_releaseRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082970;
    
    public xtreemfs_lock_releaseRequest() { file_credentials = new FileCredentials(); lock = new Lock();  }
    public xtreemfs_lock_releaseRequest( FileCredentials file_credentials, String file_id, Lock lock ) { this.file_credentials = file_credentials; this.file_id = file_id; this.lock = lock; }

    public FileCredentials getFile_credentials() { return file_credentials; }
    public void setFile_credentials( FileCredentials file_credentials ) { this.file_credentials = file_credentials; }
    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public Lock getLock() { return lock; }
    public void setLock( Lock lock ) { this.lock = lock; }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_lock_releaseResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082970;    

    // yidl.Object
    public int getTag() { return 2009082970; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_lock_releaseRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += file_credentials.getXDRSize();
        my_size += file_id != null ? ( ( file_id.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( file_id.getBytes().length + Integer.SIZE/8 ) : ( file_id.getBytes().length + Integer.SIZE/8 + 4 - ( file_id.getBytes().length + Integer.SIZE/8 ) % 4 ) : 0;
        my_size += lock.getXDRSize();
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "file_credentials", file_credentials );
        marshaller.writeString( "file_id", file_id );
        marshaller.writeStruct( "lock", lock );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        file_credentials = new FileCredentials(); unmarshaller.readStruct( "file_credentials", file_credentials );
        file_id = unmarshaller.readString( "file_id" );
        lock = new Lock(); unmarshaller.readStruct( "lock", lock );    
    }
        
    

    private FileCredentials file_credentials;
    private String file_id;
    private Lock lock;    

}

