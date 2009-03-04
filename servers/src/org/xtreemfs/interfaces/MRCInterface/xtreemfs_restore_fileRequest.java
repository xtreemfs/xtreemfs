package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.MRCInterface.*;
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         

public class xtreemfs_restore_fileRequest implements Request
{
    public xtreemfs_restore_fileRequest() { file_path = ""; file_id = ""; file_size = 0; osd_uuid = ""; stripe_size = 0; }
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

    // Object
    public String toString()
    {
        return "xtreemfs_restore_fileRequest( " + "\"" + file_path + "\"" + ", " + "\"" + file_id + "\"" + ", " + Long.toString( file_size ) + ", " + "\"" + osd_uuid + "\"" + ", " + Integer.toString( stripe_size ) + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::xtreemfs_restore_fileRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(file_path,writer); }
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(file_id,writer); }
        writer.putLong( file_size );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(osd_uuid,writer); }
        writer.putInt( stripe_size );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { file_path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        { file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        file_size = buf.getLong();
        { osd_uuid = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        stripe_size = buf.getInt();    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_path);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        my_size += ( Long.SIZE / 8 );
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(osd_uuid);
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }

    private String file_path;
    private String file_id;
    private long file_size;
    private String osd_uuid;
    private int stripe_size;
    

    // Request
    public int getInterfaceVersion() { return 2; }    
    public int getOperationNumber() { return 28; }
    public Response createDefaultResponse() { return new xtreemfs_restore_fileResponse(); }

}

