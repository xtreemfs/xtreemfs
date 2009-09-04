package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class readRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082928;
    
    public readRequest() { file_credentials = new FileCredentials();  }
    public readRequest( FileCredentials file_credentials, String file_id, long object_number, long object_version, int offset, int length ) { this.file_credentials = file_credentials; this.file_id = file_id; this.object_number = object_number; this.object_version = object_version; this.offset = offset; this.length = length; }

    public FileCredentials getFile_credentials() { return file_credentials; }
    public void setFile_credentials( FileCredentials file_credentials ) { this.file_credentials = file_credentials; }
    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public long getObject_number() { return object_number; }
    public void setObject_number( long object_number ) { this.object_number = object_number; }
    public long getObject_version() { return object_version; }
    public void setObject_version( long object_version ) { this.object_version = object_version; }
    public int getOffset() { return offset; }
    public void setOffset( int offset ) { this.offset = offset; }
    public int getLength() { return length; }
    public void setLength( int length ) { this.length = length; }

    // Request
    public Response createDefaultResponse() { return new readResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082928;    

    // yidl.Object
    public int getTag() { return 2009082928; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::readRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += file_credentials.getXDRSize(); // file_credentials
        my_size += Integer.SIZE / 8 + ( file_id != null ? ( ( file_id.getBytes().length % 4 == 0 ) ? file_id.getBytes().length : ( file_id.getBytes().length + 4 - file_id.getBytes().length % 4 ) ) : 0 ); // file_id
        my_size += Long.SIZE / 8; // object_number
        my_size += Long.SIZE / 8; // object_version
        my_size += Integer.SIZE / 8; // offset
        my_size += Integer.SIZE / 8; // length
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "file_credentials", file_credentials );
        marshaller.writeString( "file_id", file_id );
        marshaller.writeUint64( "object_number", object_number );
        marshaller.writeUint64( "object_version", object_version );
        marshaller.writeUint32( "offset", offset );
        marshaller.writeUint32( "length", length );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        file_credentials = new FileCredentials(); unmarshaller.readStruct( "file_credentials", file_credentials );
        file_id = unmarshaller.readString( "file_id" );
        object_number = unmarshaller.readUint64( "object_number" );
        object_version = unmarshaller.readUint64( "object_version" );
        offset = unmarshaller.readUint32( "offset" );
        length = unmarshaller.readUint32( "length" );    
    }
        
    

    private FileCredentials file_credentials;
    private String file_id;
    private long object_number;
    private long object_version;
    private int offset;
    private int length;    

}

