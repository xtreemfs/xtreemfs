package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_check_file_existsRequest implements org.xtreemfs.interfaces.utils.Request
{
    public xtreemfs_check_file_existsRequest() { volume_id = ""; file_ids = new StringSet(); }
    public xtreemfs_check_file_existsRequest( String volume_id, StringSet file_ids ) { this.volume_id = volume_id; this.file_ids = file_ids; }
    public xtreemfs_check_file_existsRequest( Object from_hash_map ) { volume_id = ""; file_ids = new StringSet(); this.deserialize( from_hash_map ); }
    public xtreemfs_check_file_existsRequest( Object[] from_array ) { volume_id = ""; file_ids = new StringSet();this.deserialize( from_array ); }

    public String getVolume_id() { return volume_id; }
    public void setVolume_id( String volume_id ) { this.volume_id = volume_id; }
    public StringSet getFile_ids() { return file_ids; }
    public void setFile_ids( StringSet file_ids ) { this.file_ids = file_ids; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_check_file_existsRequest"; }    
    public long getTypeId() { return 23; }

    public String toString()
    {
        return "xtreemfs_check_file_existsRequest( " + "\"" + volume_id + "\"" + ", " + file_ids.toString() + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.volume_id = ( String )from_hash_map.get( "volume_id" );
        this.file_ids.deserialize( ( Object[] )from_hash_map.get( "file_ids" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.volume_id = ( String )from_array[0];
        this.file_ids.deserialize( ( Object[] )from_array[1] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        volume_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        file_ids = new StringSet(); file_ids.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "volume_id", volume_id );
        to_hash_map.put( "file_ids", file_ids.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( volume_id, writer );
        file_ids.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(volume_id);
        my_size += file_ids.calculateSize();
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 23; }
    public Response createDefaultResponse() { return new xtreemfs_check_file_existsResponse(); }


    private String volume_id;
    private StringSet file_ids;

}

