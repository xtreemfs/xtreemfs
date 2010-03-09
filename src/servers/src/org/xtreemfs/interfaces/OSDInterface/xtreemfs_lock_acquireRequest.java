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




public class xtreemfs_lock_acquireRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010031266;

    public xtreemfs_lock_acquireRequest() { file_credentials = new FileCredentials();  }
    public xtreemfs_lock_acquireRequest( FileCredentials file_credentials, String client_uuid, int client_pid, String file_id, long offset, long length, boolean exclusive ) { this.file_credentials = file_credentials; this.client_uuid = client_uuid; this.client_pid = client_pid; this.file_id = file_id; this.offset = offset; this.length = length; this.exclusive = exclusive; }

    public FileCredentials getFile_credentials() { return file_credentials; }
    public void setFile_credentials( FileCredentials file_credentials ) { this.file_credentials = file_credentials; }
    public String getClient_uuid() { return client_uuid; }
    public void setClient_uuid( String client_uuid ) { this.client_uuid = client_uuid; }
    public int getClient_pid() { return client_pid; }
    public void setClient_pid( int client_pid ) { this.client_pid = client_pid; }
    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public long getOffset() { return offset; }
    public void setOffset( long offset ) { this.offset = offset; }
    public long getLength() { return length; }
    public void setLength( long length ) { this.length = length; }
    public boolean getExclusive() { return exclusive; }
    public void setExclusive( boolean exclusive ) { this.exclusive = exclusive; }

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
    public Response createDefaultResponse() { return new xtreemfs_lock_acquireResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2010031266;

    // yidl.runtime.Object
    public int getTag() { return 2010031266; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_lock_acquireRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += file_credentials.getXDRSize(); // file_credentials
        my_size += Integer.SIZE / 8 + ( client_uuid != null ? ( ( client_uuid.getBytes().length % 4 == 0 ) ? client_uuid.getBytes().length : ( client_uuid.getBytes().length + 4 - client_uuid.getBytes().length % 4 ) ) : 0 ); // client_uuid
        my_size += Integer.SIZE / 8; // client_pid
        my_size += Integer.SIZE / 8 + ( file_id != null ? ( ( file_id.getBytes().length % 4 == 0 ) ? file_id.getBytes().length : ( file_id.getBytes().length + 4 - file_id.getBytes().length % 4 ) ) : 0 ); // file_id
        my_size += Long.SIZE / 8; // offset
        my_size += Long.SIZE / 8; // length
        my_size += Integer.SIZE / 8; // exclusive
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "file_credentials", file_credentials );
        marshaller.writeString( "client_uuid", client_uuid );
        marshaller.writeInt32( "client_pid", client_pid );
        marshaller.writeString( "file_id", file_id );
        marshaller.writeUint64( "offset", offset );
        marshaller.writeUint64( "length", length );
        marshaller.writeBoolean( "exclusive", exclusive );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        file_credentials = new FileCredentials(); unmarshaller.readStruct( "file_credentials", file_credentials );
        client_uuid = unmarshaller.readString( "client_uuid" );
        client_pid = unmarshaller.readInt32( "client_pid" );
        file_id = unmarshaller.readString( "file_id" );
        offset = unmarshaller.readUint64( "offset" );
        length = unmarshaller.readUint64( "length" );
        exclusive = unmarshaller.readBoolean( "exclusive" );
    }

    

    private FileCredentials file_credentials;
    private String client_uuid;
    private int client_pid;
    private String file_id;
    private long offset;
    private long length;
    private boolean exclusive;

}

