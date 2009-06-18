package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_restore_fileRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 1243;

    
    public xtreemfs_restore_fileRequest() { file_path = ""; file_id = ""; file_size = 0; osd_uuid = ""; stripe_size = 0; }
    public xtreemfs_restore_fileRequest( String file_path, String file_id, long file_size, String osd_uuid, int stripe_size ) { this.file_path = file_path; this.file_id = file_id; this.file_size = file_size; this.osd_uuid = osd_uuid; this.stripe_size = stripe_size; }
    public xtreemfs_restore_fileRequest( Object from_hash_map ) { file_path = ""; file_id = ""; file_size = 0; osd_uuid = ""; stripe_size = 0; this.deserialize( from_hash_map ); }
    public xtreemfs_restore_fileRequest( Object[] from_array ) { file_path = ""; file_id = ""; file_size = 0; osd_uuid = ""; stripe_size = 0;this.deserialize( from_array ); }

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
    public int getTag() { return 1243; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_restore_fileRequest"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.file_path = ( String )from_hash_map.get( "file_path" );
        this.file_id = ( String )from_hash_map.get( "file_id" );
        this.file_size = ( ( Long )from_hash_map.get( "file_size" ) ).longValue();
        this.osd_uuid = ( String )from_hash_map.get( "osd_uuid" );
        this.stripe_size = ( ( Integer )from_hash_map.get( "stripe_size" ) ).intValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.file_path = ( String )from_array[0];
        this.file_id = ( String )from_array[1];
        this.file_size = ( ( Long )from_array[2] ).longValue();
        this.osd_uuid = ( String )from_array[3];
        this.stripe_size = ( ( Integer )from_array[4] ).intValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        file_path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        file_size = buf.getLong();
        osd_uuid = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        stripe_size = buf.getInt();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "file_path", file_path );
        to_hash_map.put( "file_id", file_id );
        to_hash_map.put( "file_size", new Long( file_size ) );
        to_hash_map.put( "osd_uuid", osd_uuid );
        to_hash_map.put( "stripe_size", new Integer( stripe_size ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( file_path, writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( file_id, writer );
        writer.putLong( file_size );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( osd_uuid, writer );
        writer.putInt( stripe_size );
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

    // Request
    public Response createDefaultResponse() { return new xtreemfs_restore_fileResponse(); }


    private String file_path;
    private String file_id;
    private long file_size;
    private String osd_uuid;
    private int stripe_size;    

}

