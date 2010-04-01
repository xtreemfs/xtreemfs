package org.xtreemfs.interfaces.OSDInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.*;
import org.xtreemfs.interfaces.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_internal_read_localRequest extends org.xtreemfs.foundation.oncrpc.utils.Request
{
    public static final int TAG = 2010031259;

    public xtreemfs_internal_read_localRequest() { file_credentials = new FileCredentials(); required_objects = new ObjectListSet();  }
    public xtreemfs_internal_read_localRequest( FileCredentials file_credentials, String file_id, long object_number, long object_version, long offset, long length, boolean attach_object_list, ObjectListSet required_objects ) { this.file_credentials = file_credentials; this.file_id = file_id; this.object_number = object_number; this.object_version = object_version; this.offset = offset; this.length = length; this.attach_object_list = attach_object_list; this.required_objects = required_objects; }

    public FileCredentials getFile_credentials() { return file_credentials; }
    public String getFile_id() { return file_id; }
    public long getObject_number() { return object_number; }
    public long getObject_version() { return object_version; }
    public long getOffset() { return offset; }
    public long getLength() { return length; }
    public boolean getAttach_object_list() { return attach_object_list; }
    public ObjectListSet getRequired_objects() { return required_objects; }
    public void setFile_credentials( FileCredentials file_credentials ) { this.file_credentials = file_credentials; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public void setObject_number( long object_number ) { this.object_number = object_number; }
    public void setObject_version( long object_version ) { this.object_version = object_version; }
    public void setOffset( long offset ) { this.offset = offset; }
    public void setLength( long length ) { this.length = length; }
    public void setAttach_object_list( boolean attach_object_list ) { this.attach_object_list = attach_object_list; }
    public void setRequired_objects( ObjectListSet required_objects ) { this.required_objects = required_objects; }

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
    public Response createDefaultResponse() { return new xtreemfs_internal_read_localResponse(); }

    // java.io.Serializable
    public static final long serialVersionUID = 2010031259;

    // yidl.runtime.Object
    public int getTag() { return 2010031259; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_read_localRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += file_credentials.getXDRSize(); // file_credentials
        my_size += Integer.SIZE / 8 + ( file_id != null ? ( ( file_id.getBytes().length % 4 == 0 ) ? file_id.getBytes().length : ( file_id.getBytes().length + 4 - file_id.getBytes().length % 4 ) ) : 0 ); // file_id
        my_size += Long.SIZE / 8; // object_number
        my_size += Long.SIZE / 8; // object_version
        my_size += Long.SIZE / 8; // offset
        my_size += Long.SIZE / 8; // length
        my_size += Integer.SIZE / 8; // attach_object_list
        my_size += required_objects.getXDRSize(); // required_objects
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "file_credentials", file_credentials );
        marshaller.writeString( "file_id", file_id );
        marshaller.writeUint64( "object_number", object_number );
        marshaller.writeUint64( "object_version", object_version );
        marshaller.writeUint64( "offset", offset );
        marshaller.writeUint64( "length", length );
        marshaller.writeBoolean( "attach_object_list", attach_object_list );
        marshaller.writeSequence( "required_objects", required_objects );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        file_credentials = new FileCredentials(); unmarshaller.readStruct( "file_credentials", file_credentials );
        file_id = unmarshaller.readString( "file_id" );
        object_number = unmarshaller.readUint64( "object_number" );
        object_version = unmarshaller.readUint64( "object_version" );
        offset = unmarshaller.readUint64( "offset" );
        length = unmarshaller.readUint64( "length" );
        attach_object_list = unmarshaller.readBoolean( "attach_object_list" );
        required_objects = new ObjectListSet(); unmarshaller.readSequence( "required_objects", required_objects );
    }

    private FileCredentials file_credentials;
    private String file_id;
    private long object_number;
    private long object_version;
    private long offset;
    private long length;
    private boolean attach_object_list;
    private ObjectListSet required_objects;
}
