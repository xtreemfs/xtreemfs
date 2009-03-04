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


         

public class xtreemfs_check_file_existsResponse implements Response
{
    public xtreemfs_check_file_existsResponse() { bitmap = ""; }
    public xtreemfs_check_file_existsResponse( String bitmap ) { this.bitmap = bitmap; }

    public String getBitmap() { return bitmap; }
    public void setBitmap( String bitmap ) { this.bitmap = bitmap; }

    // Object
    public String toString()
    {
        return "xtreemfs_check_file_existsResponse( " + "\"" + bitmap + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::xtreemfs_check_file_existsResponse"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(bitmap,writer); }        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { bitmap = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4 + ( bitmap.length() + 4 - ( bitmap.length() % 4 ) );
        return my_size;
    }

    private String bitmap;
    

    // Response
    public int getInterfaceVersion() { return 2; }
    public int getOperationNumber() { return 23; }    

}

