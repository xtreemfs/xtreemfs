package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class OSDWriteResponse implements org.xtreemfs.interfaces.utils.Serializable
{
    public OSDWriteResponse() { new_file_size = new NewFileSizeSet(); opaque_data = new OSDtoMRCDataSet(); }
    public OSDWriteResponse( NewFileSizeSet new_file_size, OSDtoMRCDataSet opaque_data ) { this.new_file_size = new_file_size; this.opaque_data = opaque_data; }
    public OSDWriteResponse( Object from_hash_map ) { new_file_size = new NewFileSizeSet(); opaque_data = new OSDtoMRCDataSet(); this.deserialize( from_hash_map ); }
    public OSDWriteResponse( Object[] from_array ) { new_file_size = new NewFileSizeSet(); opaque_data = new OSDtoMRCDataSet();this.deserialize( from_array ); }

    public NewFileSizeSet getNew_file_size() { return new_file_size; }
    public void setNew_file_size( NewFileSizeSet new_file_size ) { this.new_file_size = new_file_size; }
    public OSDtoMRCDataSet getOpaque_data() { return opaque_data; }
    public void setOpaque_data( OSDtoMRCDataSet opaque_data ) { this.opaque_data = opaque_data; }

    public String getTypeName() { return "org::xtreemfs::interfaces::OSDWriteResponse"; }    
    public long getTypeId() { return 0; }

    public String toString()
    {
        return "OSDWriteResponse( " + new_file_size.toString() + ", " + opaque_data.toString() + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.new_file_size.deserialize( ( Object[] )from_hash_map.get( "new_file_size" ) );
        this.opaque_data.deserialize( ( Object[] )from_hash_map.get( "opaque_data" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.new_file_size.deserialize( ( Object[] )from_array[0] );
        this.opaque_data.deserialize( ( Object[] )from_array[1] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        new_file_size = new NewFileSizeSet(); new_file_size.deserialize( buf );
        opaque_data = new OSDtoMRCDataSet(); opaque_data.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "new_file_size", new_file_size.serialize() );
        to_hash_map.put( "opaque_data", opaque_data.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        new_file_size.serialize( writer );
        opaque_data.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += new_file_size.calculateSize();
        my_size += opaque_data.calculateSize();
        return my_size;
    }


    private NewFileSizeSet new_file_size;
    private OSDtoMRCDataSet opaque_data;

}

