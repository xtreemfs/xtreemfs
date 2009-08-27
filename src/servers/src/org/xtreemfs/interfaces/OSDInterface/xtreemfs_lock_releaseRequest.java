package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_lock_releaseRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082970;

    
    public xtreemfs_lock_releaseRequest() { file_credentials = new FileCredentials(); file_id = ""; lock = new Lock(); }
    public xtreemfs_lock_releaseRequest( FileCredentials file_credentials, String file_id, Lock lock ) { this.file_credentials = file_credentials; this.file_id = file_id; this.lock = lock; }
    public xtreemfs_lock_releaseRequest( Object from_hash_map ) { file_credentials = new FileCredentials(); file_id = ""; lock = new Lock(); this.deserialize( from_hash_map ); }
    public xtreemfs_lock_releaseRequest( Object[] from_array ) { file_credentials = new FileCredentials(); file_id = ""; lock = new Lock();this.deserialize( from_array ); }

    public FileCredentials getFile_credentials() { return file_credentials; }
    public void setFile_credentials( FileCredentials file_credentials ) { this.file_credentials = file_credentials; }
    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public Lock getLock() { return lock; }
    public void setLock( Lock lock ) { this.lock = lock; }

    // Object
    public String toString()
    {
        return "xtreemfs_lock_releaseRequest( " + file_credentials.toString() + ", " + "\"" + file_id + "\"" + ", " + lock.toString() + " )";
    }

    // Serializable
    public int getTag() { return 2009082970; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_lock_releaseRequest"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.file_credentials.deserialize( from_hash_map.get( "file_credentials" ) );
        this.file_id = ( String )from_hash_map.get( "file_id" );
        this.lock.deserialize( from_hash_map.get( "lock" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.file_credentials.deserialize( from_array[0] );
        this.file_id = ( String )from_array[1];
        this.lock.deserialize( from_array[2] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        file_credentials = new FileCredentials(); file_credentials.deserialize( buf );
        file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        lock = new Lock(); lock.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "file_credentials", file_credentials.serialize() );
        to_hash_map.put( "file_id", file_id );
        to_hash_map.put( "lock", lock.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        file_credentials.serialize( writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( file_id, writer );
        lock.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += file_credentials.calculateSize();
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        my_size += lock.calculateSize();
        return my_size;
    }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_lock_releaseResponse(); }


    private FileCredentials file_credentials;
    private String file_id;
    private Lock lock;    

}

