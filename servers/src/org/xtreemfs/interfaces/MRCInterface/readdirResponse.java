package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class readdirResponse implements org.xtreemfs.interfaces.utils.Response
{
    public readdirResponse() { directory_entries = new DirectoryEntrySet(); }
    public readdirResponse( DirectoryEntrySet directory_entries ) { this.directory_entries = directory_entries; }
    public readdirResponse( Object from_hash_map ) { directory_entries = new DirectoryEntrySet(); this.deserialize( from_hash_map ); }
    public readdirResponse( Object[] from_array ) { directory_entries = new DirectoryEntrySet();this.deserialize( from_array ); }

    public DirectoryEntrySet getDirectory_entries() { return directory_entries; }
    public void setDirectory_entries( DirectoryEntrySet directory_entries ) { this.directory_entries = directory_entries; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::readdirResponse"; }    
    public long getTypeId() { return 12; }

    public String toString()
    {
        return "readdirResponse( " + directory_entries.toString() + " )"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.directory_entries.deserialize( from_hash_map.get( "directory_entries" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.directory_entries.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        directory_entries = new DirectoryEntrySet(); directory_entries.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "directory_entries", directory_entries.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        directory_entries.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += directory_entries.calculateSize();
        return my_size;
    }

    // Response
    public int getOperationNumber() { return 12; }


    private DirectoryEntrySet directory_entries;

}

