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




public class xtreemfs_rwr_statusRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010031292;

    public xtreemfs_rwr_statusRequest() { file_credentials = new FileCredentials();  }
    public xtreemfs_rwr_statusRequest( FileCredentials file_credentials, String file_id, long max_local_obj_version ) { this.file_credentials = file_credentials; this.file_id = file_id; this.max_local_obj_version = max_local_obj_version; }

    public FileCredentials getFile_credentials() { return file_credentials; }
    public String getFile_id() { return file_id; }
    public long getMax_local_obj_version() { return max_local_obj_version; }
    public void setFile_credentials( FileCredentials file_credentials ) { this.file_credentials = file_credentials; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public void setMax_local_obj_version( long max_local_obj_version ) { this.max_local_obj_version = max_local_obj_version; }

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
    public Response createDefaultResponse() { return new xtreemfs_rwr_statusResponse(); }

    // java.io.Serializable
    public static final long serialVersionUID = 2010031292;

    // yidl.runtime.Object
    public int getTag() { return 2010031292; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_rwr_statusRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += file_credentials.getXDRSize(); // file_credentials
        my_size += Integer.SIZE / 8 + ( file_id != null ? ( ( file_id.getBytes().length % 4 == 0 ) ? file_id.getBytes().length : ( file_id.getBytes().length + 4 - file_id.getBytes().length % 4 ) ) : 0 ); // file_id
        my_size += Long.SIZE / 8; // max_local_obj_version
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "file_credentials", file_credentials );
        marshaller.writeString( "file_id", file_id );
        marshaller.writeInt64( "max_local_obj_version", max_local_obj_version );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        file_credentials = new FileCredentials(); unmarshaller.readStruct( "file_credentials", file_credentials );
        file_id = unmarshaller.readString( "file_id" );
        max_local_obj_version = unmarshaller.readInt64( "max_local_obj_version" );
    }

    private FileCredentials file_credentials;
    private String file_id;
    private long max_local_obj_version;
}
