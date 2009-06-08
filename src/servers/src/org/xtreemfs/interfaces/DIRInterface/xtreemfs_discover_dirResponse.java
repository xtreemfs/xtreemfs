package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_discover_dirResponse implements org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 1152;

    
    public xtreemfs_discover_dirResponse() { dir_service = new DirService(); }
    public xtreemfs_discover_dirResponse( DirService dir_service ) { this.dir_service = dir_service; }
    public xtreemfs_discover_dirResponse( Object from_hash_map ) { dir_service = new DirService(); this.deserialize( from_hash_map ); }
    public xtreemfs_discover_dirResponse( Object[] from_array ) { dir_service = new DirService();this.deserialize( from_array ); }

    public DirService getDir_service() { return dir_service; }
    public void setDir_service( DirService dir_service ) { this.dir_service = dir_service; }

    // Object
    public String toString()
    {
        return "xtreemfs_discover_dirResponse( " + dir_service.toString() + " )";
    }

    // Serializable
    public int getTag() { return 1152; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_discover_dirResponse"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.dir_service.deserialize( from_hash_map.get( "dir_service" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.dir_service.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        dir_service = new DirService(); dir_service.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "dir_service", dir_service.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        dir_service.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += dir_service.calculateSize();
        return my_size;
    }


    private DirService dir_service;    

}

