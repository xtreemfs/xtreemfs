package org.xtreemfs.interfaces.OSDInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_rwr_updateRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010031288;

    public xtreemfs_rwr_updateRequest() { file_credentials = new FileCredentials(); object_data = new ObjectData();  }
    public xtreemfs_rwr_updateRequest( FileCredentials file_credentials, String file_id, long object_number, long object_version, int offset, ObjectData object_data ) { this.file_credentials = file_credentials; this.file_id = file_id; this.object_number = object_number; this.object_version = object_version; this.offset = offset; this.object_data = object_data; }

    public FileCredentials getFile_credentials() { return file_credentials; }
    public String getFile_id() { return file_id; }
    public long getObject_number() { return object_number; }
    public long getObject_version() { return object_version; }
    public int getOffset() { return offset; }
    public ObjectData getObject_data() { return object_data; }
    public void setFile_credentials( FileCredentials file_credentials ) { this.file_credentials = file_credentials; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public void setObject_number( long object_number ) { this.object_number = object_number; }
    public void setObject_version( long object_version ) { this.object_version = object_version; }
    public void setOffset( int offset ) { this.offset = offset; }
    public void setObject_data( ObjectData object_data ) { this.object_data = object_data; }

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
    public Response createDefaultResponse() { return new xtreemfs_rwr_updateResponse(); }

    // java.io.Serializable
    public static final long serialVersionUID = 2010031288;

    // yidl.runtime.Object
    public int getTag() { return 2010031288; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_rwr_updateRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += file_credentials.getXDRSize(); // file_credentials
        my_size += Integer.SIZE / 8 + ( file_id != null ? ( ( file_id.getBytes().length % 4 == 0 ) ? file_id.getBytes().length : ( file_id.getBytes().length + 4 - file_id.getBytes().length % 4 ) ) : 0 ); // file_id
        my_size += Long.SIZE / 8; // object_number
        my_size += Long.SIZE / 8; // object_version
        my_size += Integer.SIZE / 8; // offset
        my_size += object_data.getXDRSize(); // object_data
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "file_credentials", file_credentials );
        marshaller.writeString( "file_id", file_id );
        marshaller.writeUint64( "object_number", object_number );
        marshaller.writeUint64( "object_version", object_version );
        marshaller.writeUint32( "offset", offset );
        marshaller.writeStruct( "object_data", object_data );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        file_credentials = new FileCredentials(); unmarshaller.readStruct( "file_credentials", file_credentials );
        file_id = unmarshaller.readString( "file_id" );
        object_number = unmarshaller.readUint64( "object_number" );
        object_version = unmarshaller.readUint64( "object_version" );
        offset = unmarshaller.readUint32( "offset" );
        object_data = new ObjectData(); unmarshaller.readStruct( "object_data", object_data );
    }

    private FileCredentials file_credentials;
    private String file_id;
    private long object_number;
    private long object_version;
    private int offset;
    private ObjectData object_data;
}
