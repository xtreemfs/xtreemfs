package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class accessResponse implements org.xtreemfs.interfaces.utils.Response
{
    public accessResponse() { returnValue = false; }
    public accessResponse( boolean returnValue ) { this.returnValue = returnValue; }
    public accessResponse( Object from_hash_map ) { returnValue = false; this.deserialize( from_hash_map ); }
    public accessResponse( Object[] from_array ) { returnValue = false;this.deserialize( from_array ); }

    public boolean getReturnValue() { return returnValue; }
    public void setReturnValue( boolean returnValue ) { this.returnValue = returnValue; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::accessResponse"; }    
    public long getTypeId() { return 1; }

    public String toString()
    {
        return "accessResponse( " + Boolean.toString( returnValue ) + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.returnValue = ( ( Boolean )from_hash_map.get( "returnValue" ) ).booleanValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.returnValue = ( ( Boolean )from_array[0] ).booleanValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        returnValue = buf.getInt() != 0;
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "returnValue", new Boolean( returnValue ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        writer.putInt( returnValue ? 1 : 0 );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4;
        return my_size;
    }

    // Response
    public int getOperationNumber() { return 1; }


    private boolean returnValue;

}

