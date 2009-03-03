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


         

public class listxattrResponse implements Response
{
    public listxattrResponse() { names = new org.xtreemfs.interfaces.StringSet(); }
    public listxattrResponse( StringSet names ) { this.names = names; }

    public StringSet getNames() { return names; }
    public void setNames( StringSet names ) { this.names = names; }

    // Object
    public String toString()
    {
        return "listxattrResponse( " + names.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::listxattrResponse"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        names.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        names = new org.xtreemfs.interfaces.StringSet(); names.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += names.calculateSize();
        return my_size;
    }

    private StringSet names;
    

    // Response
    public int getInterfaceVersion() { return 2; }
    public int getOperationNumber() { return 8; }    

}

