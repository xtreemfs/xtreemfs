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


         

public class readdirResponse implements Response
{
    public readdirResponse() { directory_entries = new org.xtreemfs.interfaces.DirectoryEntrySet(); }
    public readdirResponse( DirectoryEntrySet directory_entries ) { this.directory_entries = directory_entries; }

    public DirectoryEntrySet getDirectory_entries() { return directory_entries; }
    public void setDirectory_entries( DirectoryEntrySet directory_entries ) { this.directory_entries = directory_entries; }

    // Object
    public String toString()
    {
        return "readdirResponse( " + directory_entries.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::readdirResponse"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        directory_entries.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        directory_entries = new org.xtreemfs.interfaces.DirectoryEntrySet(); directory_entries.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += directory_entries.calculateSize();
        return my_size;
    }

    private DirectoryEntrySet directory_entries;
    

    // Response
    public int getInterfaceVersion() { return 2; }
    public int getOperationNumber() { return 12; }    

}

