package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class InternalReadLocalResponse implements org.xtreemfs.interfaces.utils.Serializable
{
    public InternalReadLocalResponse() { file_size = new NewFileSize(); zero_padding = 0; data = new ObjectData(); }
    public InternalReadLocalResponse( NewFileSize file_size, int zero_padding, ObjectData data ) { this.file_size = file_size; this.zero_padding = zero_padding; this.data = data; }
    public InternalReadLocalResponse( Object from_hash_map ) { file_size = new NewFileSize(); zero_padding = 0; data = new ObjectData(); this.deserialize( from_hash_map ); }
    public InternalReadLocalResponse( Object[] from_array ) { file_size = new NewFileSize(); zero_padding = 0; data = new ObjectData();this.deserialize( from_array ); }

    public NewFileSize getFile_size() { return file_size; }
    public void setFile_size( NewFileSize file_size ) { this.file_size = file_size; }
    public int getZero_padding() { return zero_padding; }
    public void setZero_padding( int zero_padding ) { this.zero_padding = zero_padding; }
    public ObjectData getData() { return data; }
    public void setData( ObjectData data ) { this.data = data; }

    public String getTypeName() { return "org::xtreemfs::interfaces::InternalReadLocalResponse"; }    
    public long getTypeId() { return 0; }

    public String toString()
    {
        return "InternalReadLocalResponse( " + file_size.toString() + ", " + Integer.toString( zero_padding ) + ", " + data.toString() + " )"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.file_size.deserialize( from_hash_map.get( "file_size" ) );
        this.zero_padding = ( ( Integer )from_hash_map.get( "zero_padding" ) ).intValue();
        this.data.deserialize( from_hash_map.get( "data" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.file_size.deserialize( from_array[0] );
        this.zero_padding = ( ( Integer )from_array[1] ).intValue();
        this.data.deserialize( from_array[2] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        file_size = new NewFileSize(); file_size.deserialize( buf );
        zero_padding = buf.getInt();
        data = new ObjectData(); data.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "file_size", file_size.serialize() );
        to_hash_map.put( "zero_padding", new Integer( zero_padding ) );
        to_hash_map.put( "data", data.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        file_size.serialize( writer );
        writer.putInt( zero_padding );
        data.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += file_size.calculateSize();
        my_size += ( Integer.SIZE / 8 );
        my_size += data.calculateSize();
        return my_size;
    }


    private NewFileSize file_size;
    private int zero_padding;
    private ObjectData data;

}

