package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_restore_fileRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009090452;
    
    public xtreemfs_restore_fileRequest() {  }
    public xtreemfs_restore_fileRequest( String file_path, String file_id, long file_size, String osd_uuid, int stripe_size ) { this.file_path = file_path; this.file_id = file_id; this.file_size = file_size; this.osd_uuid = osd_uuid; this.stripe_size = stripe_size; }

    public String getFile_path() { return file_path; }
    public void setFile_path( String file_path ) { this.file_path = file_path; }
    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public long getFile_size() { return file_size; }
    public void setFile_size( long file_size ) { this.file_size = file_size; }
    public String getOsd_uuid() { return osd_uuid; }
    public void setOsd_uuid( String osd_uuid ) { this.osd_uuid = osd_uuid; }
    public int getStripe_size() { return stripe_size; }
    public void setStripe_size( int stripe_size ) { this.stripe_size = stripe_size; }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_restore_fileResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009090452;    

    // yidl.Object
    public int getTag() { return 2009090452; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_restore_fileRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += file_path != null ? ( ( file_path.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( file_path.getBytes().length + Integer.SIZE/8 ) : ( file_path.getBytes().length + Integer.SIZE/8 + 4 - ( file_path.getBytes().length + Integer.SIZE/8 ) % 4 ) : 0;
        my_size += file_id != null ? ( ( file_id.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( file_id.getBytes().length + Integer.SIZE/8 ) : ( file_id.getBytes().length + Integer.SIZE/8 + 4 - ( file_id.getBytes().length + Integer.SIZE/8 ) % 4 ) : 0;
        my_size += ( Long.SIZE / 8 );
        my_size += osd_uuid != null ? ( ( osd_uuid.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( osd_uuid.getBytes().length + Integer.SIZE/8 ) : ( osd_uuid.getBytes().length + Integer.SIZE/8 + 4 - ( osd_uuid.getBytes().length + Integer.SIZE/8 ) % 4 ) : 0;
        my_size += ( Integer.SIZE / 8 );
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

