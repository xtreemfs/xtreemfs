package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class InternalReadLocalResponse implements org.xtreemfs.interfaces.utils.Serializable
{
    public InternalReadLocalResponse() { file_size = new org.xtreemfs.interfaces.NewFileSize(); zero_padding = 0; data = new org.xtreemfs.interfaces.ObjectData(); }
    public InternalReadLocalResponse( NewFileSize file_size, int zero_padding, ObjectData data ) { this.file_size = file_size; this.zero_padding = zero_padding; this.data = data; }

    public NewFileSize getFile_size() { return file_size; }
    public void setFile_size( NewFileSize file_size ) { this.file_size = file_size; }
    public int getZero_padding() { return zero_padding; }
    public void setZero_padding( int zero_padding ) { this.zero_padding = zero_padding; }
    public ObjectData getData() { return data; }
    public void setData( ObjectData data ) { this.data = data; }

    // Object
    public String toString()
    {
        return "InternalReadLocalResponse( " + file_size.toString() + ", " + Integer.toString( zero_padding ) + ", " + data.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::InternalReadLocalResponse"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        file_size.serialize( writer );
        writer.putInt( zero_padding );
        data.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        file_size = new org.xtreemfs.interfaces.NewFileSize(); file_size.deserialize( buf );
        zero_padding = buf.getInt();
        data = new org.xtreemfs.interfaces.ObjectData(); data.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += file_size.calculateSize();
        my_size += ( Integer.SIZE / 8 );
        my_size += data.calculateSize();
        return my_size;
    }

    private NewFileSize file_size;
    private int zero_padding;
    private ObjectData data;

}

