package org.xtreemfs.interfaces.MRCInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.*;
import org.xtreemfs.interfaces.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_restore_fileRequest extends org.xtreemfs.foundation.oncrpc.utils.Request
{
    public static final int TAG = 2010031158;

    public xtreemfs_restore_fileRequest() {  }
    public xtreemfs_restore_fileRequest( String file_path, String file_id, long file_size, String osd_uuid, int stripe_size ) { this.file_path = file_path; this.file_id = file_id; this.file_size = file_size; this.osd_uuid = osd_uuid; this.stripe_size = stripe_size; }

    public String getFile_path() { return file_path; }
    public String getFile_id() { return file_id; }
    public long getFile_size() { return file_size; }
    public String getOsd_uuid() { return osd_uuid; }
    public int getStripe_size() { return stripe_size; }
    public void setFile_path( String file_path ) { this.file_path = file_path; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public void setFile_size( long file_size ) { this.file_size = file_size; }
    public void setOsd_uuid( String osd_uuid ) { this.osd_uuid = osd_uuid; }
    public void setStripe_size( int stripe_size ) { this.stripe_size = stripe_size; }

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
    public Response createDefaultResponse() { return new xtreemfs_restore_fileResponse(); }

    // java.io.Serializable
    public static final long serialVersionUID = 2010031158;

    // yidl.runtime.Object
    public int getTag() { return 2010031158; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_restore_fileRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( file_path != null ? ( ( file_path.getBytes().length % 4 == 0 ) ? file_path.getBytes().length : ( file_path.getBytes().length + 4 - file_path.getBytes().length % 4 ) ) : 0 ); // file_path
        my_size += Integer.SIZE / 8 + ( file_id != null ? ( ( file_id.getBytes().length % 4 == 0 ) ? file_id.getBytes().length : ( file_id.getBytes().length + 4 - file_id.getBytes().length % 4 ) ) : 0 ); // file_id
        my_size += Long.SIZE / 8; // file_size
        my_size += Integer.SIZE / 8 + ( osd_uuid != null ? ( ( osd_uuid.getBytes().length % 4 == 0 ) ? osd_uuid.getBytes().length : ( osd_uuid.getBytes().length + 4 - osd_uuid.getBytes().length % 4 ) ) : 0 ); // osd_uuid
        my_size += Integer.SIZE / 8; // stripe_size
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "file_path", file_path );
        marshaller.writeString( "file_id", file_id );
        marshaller.writeUint64( "file_size", file_size );
        marshaller.writeString( "osd_uuid", osd_uuid );
        marshaller.writeInt32( "stripe_size", stripe_size );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        file_path = unmarshaller.readString( "file_path" );
        file_id = unmarshaller.readString( "file_id" );
        file_size = unmarshaller.readUint64( "file_size" );
        osd_uuid = unmarshaller.readString( "osd_uuid" );
        stripe_size = unmarshaller.readInt32( "stripe_size" );
    }

    private String file_path;
    private String file_id;
    private long file_size;
    private String osd_uuid;
    private int stripe_size;
}
