package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class InternalGmax implements Struct
{
    public static final int TAG = 2009082668;
    
    public InternalGmax() {  }
    public InternalGmax( long epoch, long last_object_id, long file_size ) { this.epoch = epoch; this.last_object_id = last_object_id; this.file_size = file_size; }

    public long getEpoch() { return epoch; }
    public void setEpoch( long epoch ) { this.epoch = epoch; }
    public long getLast_object_id() { return last_object_id; }
    public void setLast_object_id( long last_object_id ) { this.last_object_id = last_object_id; }
    public long getFile_size() { return file_size; }
    public void setFile_size( long file_size ) { this.file_size = file_size; }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        string_writer.append(this.getClass().getCanonicalName());
        string_writer.append(" ");
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082668;    

    // yidl.runtime.Object
    public int getTag() { return 2009082668; }
    public String getTypeName() { return "org::xtreemfs::interfaces::InternalGmax"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Long.SIZE / 8; // epoch
        my_size += Long.SIZE / 8; // last_object_id
        my_size += Long.SIZE / 8; // file_size
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeUint64( "epoch", epoch );
        marshaller.writeUint64( "last_object_id", last_object_id );
        marshaller.writeUint64( "file_size", file_size );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        epoch = unmarshaller.readUint64( "epoch" );
        last_object_id = unmarshaller.readUint64( "last_object_id" );
        file_size = unmarshaller.readUint64( "file_size" );    
    }
        
    

    private long epoch;
    private long last_object_id;
    private long file_size;    

}

