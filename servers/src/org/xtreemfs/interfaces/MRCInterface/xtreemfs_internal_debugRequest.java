package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_internal_debugRequest implements org.xtreemfs.interfaces.utils.Request
{
    public xtreemfs_internal_debugRequest() { cmd = ""; }
    public xtreemfs_internal_debugRequest( String cmd ) { this.cmd = cmd; }
    public xtreemfs_internal_debugRequest( Object from_hash_map ) { cmd = ""; this.deserialize( from_hash_map ); }
    public xtreemfs_internal_debugRequest( Object[] from_array ) { cmd = "";this.deserialize( from_array ); }

    public String getCmd() { return cmd; }
    public void setCmd( String cmd ) { this.cmd = cmd; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_internal_debugRequest"; }    
    public long getTypeId() { return 100; }

    public String toString()
    {
        return "xtreemfs_internal_debugRequest( " + "\"" + cmd + "\"" + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.cmd = ( String )from_hash_map.get( "cmd" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.cmd = ( String )from_array[0];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        cmd = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "cmd", cmd );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( cmd, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(cmd);
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 100; }
    public Response createDefaultResponse() { return new xtreemfs_internal_debugResponse(); }


    private String cmd;

}

