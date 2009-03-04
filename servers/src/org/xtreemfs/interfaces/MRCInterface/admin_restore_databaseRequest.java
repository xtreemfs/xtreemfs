package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.MRCInterface.*;
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         

public class admin_restore_databaseRequest implements Request
{
    public admin_restore_databaseRequest() { password = ""; dump_file = ""; }
    public admin_restore_databaseRequest( String password, String dump_file ) { this.password = password; this.dump_file = dump_file; }

    public String getPassword() { return password; }
    public void setPassword( String password ) { this.password = password; }
    public String getDump_file() { return dump_file; }
    public void setDump_file( String dump_file ) { this.dump_file = dump_file; }

    // Object
    public String toString()
    {
        return "admin_restore_databaseRequest( " + "\"" + password + "\"" + ", " + "\"" + dump_file + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::admin_restore_databaseRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(password,writer); }
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(dump_file,writer); }        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { password = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        { dump_file = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4 + ( password.length() + 4 - ( password.length() % 4 ) );
        my_size += 4 + ( dump_file.length() + 4 - ( dump_file.length() % 4 ) );
        return my_size;
    }

    private String password;
    private String dump_file;
    

    // Request
    public int getInterfaceVersion() { return 2; }    
    public int getOperationNumber() { return 53; }
    public Response createDefaultResponse() { return new admin_restore_databaseResponse(); }

}

