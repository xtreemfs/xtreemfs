package org.xtreemfs.interfaces.OSDInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_broadcast_gmaxRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010030634;

    public xtreemfs_broadcast_gmaxRequest() {  }
    public xtreemfs_broadcast_gmaxRequest( String file_id, long truncate_epoch, long last_object, long file_size ) { this.file_id = file_id; this.truncate_epoch = truncate_epoch; this.last_object = last_object; this.file_size = file_size; }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public long getTruncate_epoch() { return truncate_epoch; }
    public void setTruncate_epoch( long truncate_epoch ) { this.truncate_epoch = truncate_epoch; }
    public long getLast_object() { return last_object; }
    public void setLast_object( long last_object ) { this.last_object = last_object; }
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

    // Request
    public Response createDefaultResponse() { return new xtreemfs_broadcast_gmaxResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2010030634;

    // yidl.runtime.Object
    public int getTag() { return 2010030634; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_broadcast_gmaxRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( file_id != null ? ( ( file_id.getBytes().length % 4 == 0 ) ? file_id.getBytes().length : ( file_id.getBytes().length + 4 - file_id.getBytes().length % 4 ) ) : 0 ); // file_id
        my_size += Long.SIZE / 8; // truncate_epoch
        my_size += Long.SIZE / 8; // last_object
        my_size += Long.SIZE / 8; // file_size
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "file_id", file_id );
        marshaller.writeUint64( "truncate_epoch", truncate_epoch );
        marshaller.writeUint64( "last_object", last_object );
        marshaller.writeUint64( "file_size", file_size );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        file_id = unmarshaller.readString( "file_id" );
        truncate_epoch = unmarshaller.readUint64( "truncate_epoch" );
        last_object = unmarshaller.readUint64( "last_object" );
        file_size = unmarshaller.readUint64( "file_size" );
    }

    

    private String file_id;
    private long truncate_epoch;
    private long last_object;
    private long file_size;

}

