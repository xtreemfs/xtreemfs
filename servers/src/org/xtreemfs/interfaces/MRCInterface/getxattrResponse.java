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


         

public class getxattrResponse implements Response
{
    public getxattrResponse() { returnValue = ""; }
    public getxattrResponse( String returnValue ) { this.returnValue = returnValue; }

    public String getReturnValue() { return returnValue; }
    public void setReturnValue( String returnValue ) { this.returnValue = returnValue; }

    // Object
    public String toString()
    {
        return "getxattrResponse( " + "\"" + returnValue + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::getxattrResponse"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(returnValue,writer); }        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { returnValue = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4 + ( returnValue.length() + 4 - ( returnValue.length() % 4 ) );
        return my_size;
    }

    private String returnValue;
    

    // Response
    public int getInterfaceVersion() { return 2; }
    public int getOperationNumber() { return 6; }    

}

