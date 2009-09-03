package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_internal_read_localRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082961;
    
    public xtreemfs_internal_read_localRequest() { file_credentials = new FileCredentials(); requiredObjects = new ObjectListSet();  }
    public xtreemfs_internal_read_localRequest( FileCredentials file_credentials, String file_id, long object_number, long object_version, long offset, long length, boolean attachObjectList, ObjectListSet requiredObjects ) { this.file_credentials = file_credentials; this.file_id = file_id; this.object_number = object_number; this.object_version = object_version; this.offset = offset; this.length = length; this.attachObjectList = attachObjectList; this.requiredObjects = requiredObjects; }

    public FileCredentials getFile_credentials() { return file_credentials; }
    public void setFile_credentials( FileCredentials file_credentials ) { this.file_credentials = file_credentials; }
    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public long getObject_number() { return object_number; }
    public void setObject_number( long object_number ) { this.object_number = object_number; }
    public long getObject_version() { return object_version; }
    public void setObject_version( long object_version ) { this.object_version = object_version; }
    public long getOffset() { return offset; }
    public void setOffset( long offset ) { this.offset = offset; }
    public long getLength() { return length; }
    public void setLength( long length ) { this.length = length; }
    public boolean getAttachObjectList() { return attachObjectList; }
    public void setAttachObjectList( boolean attachObjectList ) { this.attachObjectList = attachObjectList; }
    public ObjectListSet getRequiredObjects() { return requiredObjects; }
    public void setRequiredObjects( ObjectListSet requiredObjects ) { this.requiredObjects = requiredObjects; }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_internal_read_localResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082961;    

    // yidl.Object
    public int getTag() { return 2009082961; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_read_localRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += file_credentials.getXDRSize();
        my_size += Integer.SIZE/8 + ( file_id != null ? ( ( file_id.getBytes().length % 4 == 0 ) ? file_id.getBytes().length : ( file_id.getBytes().length + 4 - file_id.getBytes().length % 4 ) ) : 0 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += 4;
        my_size += requiredObjects.getXDRSize();
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
        marshaller.writeBoolean( "attachObjectList", attachObjectList );
        marshaller.writeSequence( "requiredObjects", requiredObjects );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        file_credentials = new FileCredentials(); unmarshaller.readStruct( "file_credentials", file_credentials );
        file_id = unmarshaller.readString( "file_id" );
        object_number = unmarshaller.readUint64( "object_number" );
        object_version = unmarshaller.readUint64( "object_version" );
        offset = unmarshaller.readUint64( "offset" );
        length = unmarshaller.readUint64( "length" );
        attachObjectList = unmarshaller.readBoolean( "attachObjectList" );
        requiredObjects = new ObjectListSet(); unmarshaller.readSequence( "requiredObjects", requiredObjects );    
    }
        
    

    private FileCredentials file_credentials;
    private String file_id;
    private long object_number;
    private long object_version;
    private long offset;
    private long length;
    private boolean attachObjectList;
    private ObjectListSet requiredObjects;    

}

