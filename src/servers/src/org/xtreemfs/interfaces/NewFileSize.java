package org.xtreemfs.interfaces;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class NewFileSize extends Struct
{
    public static final int TAG = 2009082629;
    
    public NewFileSize() {  }
    public NewFileSize( long size_in_bytes, int truncate_epoch ) { this.size_in_bytes = size_in_bytes; this.truncate_epoch = truncate_epoch; }

    public long getSize_in_bytes() { return size_in_bytes; }
    public void setSize_in_bytes( long size_in_bytes ) { this.size_in_bytes = size_in_bytes; }
    public int getTruncate_epoch() { return truncate_epoch; }
    public void setTruncate_epoch( int truncate_epoch ) { this.truncate_epoch = truncate_epoch; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082629;    

    // yidl.Object
    public int getTag() { return 2009082629; }
    public String getTypeName() { return "org::xtreemfs::interfaces::NewFileSize"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Long.SIZE / 8; // size_in_bytes
        my_size += Integer.SIZE / 8; // truncate_epoch
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeUint64( "size_in_bytes", size_in_bytes );
        marshaller.writeUint32( "truncate_epoch", truncate_epoch );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        size_in_bytes = unmarshaller.readUint64( "size_in_bytes" );
        truncate_epoch = unmarshaller.readUint32( "truncate_epoch" );    
    }
        
    

    private long size_in_bytes;
    private int truncate_epoch;    

}

