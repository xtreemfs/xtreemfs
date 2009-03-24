package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class getxattrResponse implements org.xtreemfs.interfaces.utils.Response
{
    public getxattrResponse() { returnValue = ""; }
    public getxattrResponse( String returnValue ) { this.returnValue = returnValue; }
    public getxattrResponse( Object from_hash_map ) { returnValue = ""; this.deserialize( from_hash_map ); }
    public getxattrResponse( Object[] from_array ) { returnValue = "";this.deserialize( from_array ); }

    public String getReturnValue() { return returnValue; }
    public void setReturnValue( String returnValue ) { this.returnValue = returnValue; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::getxattrResponse"; }    
    public long getTypeId() { return 6; }

    public String toString()
    {
        return "getxattrResponse( " + "\"" + returnValue + "\"" + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.returnValue = ( String )from_hash_map.get( "returnValue" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.returnValue = ( String )from_array[0];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        returnValue = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "returnValue", returnValue );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( returnValue, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(returnValue);
        return my_size;
    }

    // Response
    public int getOperationNumber() { return 6; }


    private String returnValue;

}

