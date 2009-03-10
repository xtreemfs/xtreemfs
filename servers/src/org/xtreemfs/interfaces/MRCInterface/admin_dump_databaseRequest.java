package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class admin_dump_databaseRequest implements org.xtreemfs.interfaces.utils.Request
{
    public admin_dump_databaseRequest() { password = ""; dump_file = ""; }
    public admin_dump_databaseRequest( String password, String dump_file ) { this.password = password; this.dump_file = dump_file; }
    public admin_dump_databaseRequest( Object from_hash_map ) { password = ""; dump_file = ""; this.deserialize( from_hash_map ); }
    public admin_dump_databaseRequest( Object[] from_array ) { password = ""; dump_file = "";this.deserialize( from_array ); }

    public String getPassword() { return password; }
    public void setPassword( String password ) { this.password = password; }
    public String getDump_file() { return dump_file; }
    public void setDump_file( String dump_file ) { this.dump_file = dump_file; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::admin_dump_databaseRequest"; }    
    public long getTypeId() { return 52; }

    public String toString()
    {
        return "admin_dump_databaseRequest( " + "\"" + password + "\"" + ", " + "\"" + dump_file + "\"" + " )"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.password = ( String )from_hash_map.get( "password" );
        this.dump_file = ( String )from_hash_map.get( "dump_file" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.password = ( String )from_array[0];
        this.dump_file = ( String )from_array[1];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        password = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        dump_file = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "password", password );
        to_hash_map.put( "dump_file", dump_file );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( password, writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( dump_file, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(password);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(dump_file);
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 52; }
    public Response createDefaultResponse() { return new admin_dump_databaseResponse(); }


    private String password;
    private String dump_file;

}

