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


         

public class admin_checkpointRequest implements Request
{
    public admin_checkpointRequest() { password = ""; }
    public admin_checkpointRequest( String password ) { this.password = password; }

    public String getPassword() { return password; }
    public void setPassword( String password ) { this.password = password; }

    // Object
    public String toString()
    {
        return "admin_checkpointRequest( " + "\"" + password + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::admin_checkpointRequest"; }    
    
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
    public int getInterfaceVersion() { return 2; }    
    public int getOperationNumber() { return 51; }
    public Response createDefaultResponse() { return new admin_checkpointResponse(); }

}

