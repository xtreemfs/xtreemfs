package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.DIRInterface.*;
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         

public class admin_shutdownRequest implements Request
{
    public admin_shutdownRequest() { password = ""; }
    public admin_shutdownRequest( String password ) { this.password = password; }

    public String getPassword() { return password; }
    public void setPassword( String password ) { this.password = password; }

    // Object
    public String toString()
    {
        return "admin_shutdownRequest( " + "\"" + password + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::DIRInterface::admin_shutdownRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(password,writer); }        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { password = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4 + ( password.length() + 4 - ( password.length() % 4 ) );
        return my_size;
    }

    private String password;
    

    // Request
    public int getInterfaceVersion() { return 1; }    
    public int getOperationNumber() { return 51; }
    public Response createDefaultResponse() { return new admin_shutdownResponse(); }

}

