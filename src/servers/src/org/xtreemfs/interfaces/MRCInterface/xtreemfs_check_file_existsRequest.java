package org.xtreemfs.interfaces.MRCInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_check_file_existsRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010031147;

    public xtreemfs_check_file_existsRequest() { file_ids = new StringSet();  }
    public xtreemfs_check_file_existsRequest( String volume_id, StringSet file_ids, String osd_uuid ) { this.volume_id = volume_id; this.file_ids = file_ids; this.osd_uuid = osd_uuid; }

    public String getVolume_id() { return volume_id; }
    public StringSet getFile_ids() { return file_ids; }
    public String getOsd_uuid() { return osd_uuid; }
    public void setVolume_id( String volume_id ) { this.volume_id = volume_id; }
    public void setFile_ids( StringSet file_ids ) { this.file_ids = file_ids; }
    public void setOsd_uuid( String osd_uuid ) { this.osd_uuid = osd_uuid; }

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
    public Response createDefaultResponse() { return new xtreemfs_check_file_existsResponse(); }

    // java.io.Serializable
    public static final long serialVersionUID = 2010031147;

    // yidl.runtime.Object
    public int getTag() { return 2010031147; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_check_file_existsRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( volume_id != null ? ( ( volume_id.getBytes().length % 4 == 0 ) ? volume_id.getBytes().length : ( volume_id.getBytes().length + 4 - volume_id.getBytes().length % 4 ) ) : 0 ); // volume_id
        my_size += file_ids.getXDRSize(); // file_ids
        my_size += Integer.SIZE / 8 + ( osd_uuid != null ? ( ( osd_uuid.getBytes().length % 4 == 0 ) ? osd_uuid.getBytes().length : ( osd_uuid.getBytes().length + 4 - osd_uuid.getBytes().length % 4 ) ) : 0 ); // osd_uuid
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "volume_id", volume_id );
        marshaller.writeSequence( "file_ids", file_ids );
        marshaller.writeString( "osd_uuid", osd_uuid );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        volume_id = unmarshaller.readString( "volume_id" );
        file_ids = new StringSet(); unmarshaller.readSequence( "file_ids", file_ids );
        osd_uuid = unmarshaller.readString( "osd_uuid" );
    }

    private String volume_id;
    private StringSet file_ids;
    private String osd_uuid;
}
