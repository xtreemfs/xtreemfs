package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class listxattrResponse implements org.xtreemfs.interfaces.utils.Response
{
    public listxattrResponse() { names = new StringSet(); }
    public listxattrResponse( StringSet names ) { this.names = names; }
    public listxattrResponse( Object from_hash_map ) { names = new StringSet(); this.deserialize( from_hash_map ); }
    public listxattrResponse( Object[] from_array ) { names = new StringSet();this.deserialize( from_array ); }

    public StringSet getNames() { return names; }
    public void setNames( StringSet names ) { this.names = names; }

    public long getTag() { return 1208; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::listxattrResponse"; }

    public String toString()
    {
        return "listxattrResponse( " + names.toString() + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.names.deserialize( ( Object[] )from_hash_map.get( "names" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.names.deserialize( ( Object[] )from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        names = new StringSet(); names.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "names", names.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        names.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += names.calculateSize();
        return my_size;
    }

    // Response
    public int getOperationNumber() { return 1208; }


    private StringSet names;    

}

