package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_check_file_existsResponse implements org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 1231;

    
    public xtreemfs_check_file_existsResponse() { bitmap = ""; }
    public xtreemfs_check_file_existsResponse( String bitmap ) { this.bitmap = bitmap; }
    public xtreemfs_check_file_existsResponse( Object from_hash_map ) { bitmap = ""; this.deserialize( from_hash_map ); }
    public xtreemfs_check_file_existsResponse( Object[] from_array ) { bitmap = "";this.deserialize( from_array ); }

    public String getBitmap() { return bitmap; }
    public void setBitmap( String bitmap ) { this.bitmap = bitmap; }

    // Object
    public String toString()
    {
        return "xtreemfs_check_file_existsResponse( " + "\"" + bitmap + "\"" + " )";
    }

    // Serializable
    public int getTag() { return 1231; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_check_file_existsResponse"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.bitmap = ( String )from_hash_map.get( "bitmap" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.bitmap = ( String )from_array[0];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        bitmap = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "bitmap", bitmap );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( bitmap, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(bitmap);
        return my_size;
    }


    private String bitmap;    

}

