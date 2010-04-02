package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class InternalGmax implements Struct
{
    public static final int TAG = 2010030966;

    public InternalGmax() {  }
    public InternalGmax( long epoch, long file_size, long last_object_id ) { this.epoch = epoch; this.file_size = file_size; this.last_object_id = last_object_id; }

    public long getEpoch() { return epoch; }
    public long getFile_size() { return file_size; }
    public long getLast_object_id() { return last_object_id; }
    public void setEpoch( long epoch ) { this.epoch = epoch; }
    public void setFile_size( long file_size ) { this.file_size = file_size; }
    public void setLast_object_id( long last_object_id ) { this.last_object_id = last_object_id; }

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
    public static final long serialVersionUID = 2010030966;

    // yidl.runtime.Object
    public int getTag() { return 2010030966; }
    public String getTypeName() { return "org::xtreemfs::interfaces::InternalGmax"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Long.SIZE / 8; // epoch
        my_size += Long.SIZE / 8; // file_size
        my_size += Long.SIZE / 8; // last_object_id
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeUint64( "epoch", epoch );
        marshaller.writeUint64( "file_size", file_size );
        marshaller.writeUint64( "last_object_id", last_object_id );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        epoch = unmarshaller.readUint64( "epoch" );
        file_size = unmarshaller.readUint64( "file_size" );
        last_object_id = unmarshaller.readUint64( "last_object_id" );
    }

    private long epoch;
    private long file_size;
    private long last_object_id;
}
