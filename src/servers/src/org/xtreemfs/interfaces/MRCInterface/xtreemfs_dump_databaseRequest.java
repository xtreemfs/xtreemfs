package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_dump_databaseRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082850;

    
    public xtreemfs_dump_databaseRequest() { dump_file = ""; }
    public xtreemfs_dump_databaseRequest( String dump_file ) { this.dump_file = dump_file; }
    public xtreemfs_dump_databaseRequest( Object from_hash_map ) { dump_file = ""; this.deserialize( from_hash_map ); }
    public xtreemfs_dump_databaseRequest( Object[] from_array ) { dump_file = "";this.deserialize( from_array ); }

    public String getDump_file() { return dump_file; }
    public void setDump_file( String dump_file ) { this.dump_file = dump_file; }

    // Object
    public String toString()
    {
        return "xtreemfs_dump_databaseRequest( " + "\"" + dump_file + "\"" + " )";
    }

    // Serializable
    public int getTag() { return 2009082850; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_dump_databaseRequest"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.dump_file = ( String )from_hash_map.get( "dump_file" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.dump_file = ( String )from_array[0];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        dump_file = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "dump_file", dump_file );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( dump_file, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(dump_file);
        return my_size;
    }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_dump_databaseResponse(); }


    private String dump_file;    

}

