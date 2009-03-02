package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class OSDWriteResponse implements org.xtreemfs.interfaces.utils.Serializable
{
    public OSDWriteResponse() { new_file_size = new org.xtreemfs.interfaces.NewFileSizeSet(); opaque_data = new org.xtreemfs.interfaces.OSDtoMRCDataSet(); }
    public OSDWriteResponse( NewFileSizeSet new_file_size, OSDtoMRCDataSet opaque_data ) { this.new_file_size = new_file_size; this.opaque_data = opaque_data; }

    public NewFileSizeSet getNew_file_size() { return new_file_size; }
    public void setNew_file_size( NewFileSizeSet new_file_size ) { this.new_file_size = new_file_size; }
    public OSDtoMRCDataSet getOpaque_data() { return opaque_data; }
    public void setOpaque_data( OSDtoMRCDataSet opaque_data ) { this.opaque_data = opaque_data; }

    // Object
    public String toString()
    {
        return "OSDWriteResponse( " + new_file_size.toString() + ", " + opaque_data.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::OSDWriteResponse"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        new_file_size.serialize( writer );
        opaque_data.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        new_file_size = new org.xtreemfs.interfaces.NewFileSizeSet(); new_file_size.deserialize( buf );
        opaque_data = new org.xtreemfs.interfaces.OSDtoMRCDataSet(); opaque_data.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += new_file_size.calculateSize();
        my_size += opaque_data.calculateSize();
        return my_size;
    }

    private NewFileSizeSet new_file_size;
    private OSDtoMRCDataSet opaque_data;

}

