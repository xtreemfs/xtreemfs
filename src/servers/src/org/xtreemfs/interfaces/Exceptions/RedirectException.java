package org.xtreemfs.interfaces.Exceptions;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class RedirectException extends org.xtreemfs.interfaces.utils.ONCRPCException
{
    public RedirectException() { to_uuid = ""; }
    public RedirectException( String to_uuid ) { this.to_uuid = to_uuid; }
    public RedirectException( Object from_hash_map ) { to_uuid = ""; this.deserialize( from_hash_map ); }
    public RedirectException( Object[] from_array ) { to_uuid = "";this.deserialize( from_array ); }

    public String getTo_uuid() { return to_uuid; }
    public void setTo_uuid( String to_uuid ) { this.to_uuid = to_uuid; }

    public String getTypeName() { return "org::xtreemfs::interfaces::Exceptions::RedirectException"; }    
    public long getTypeId() { return 0; }

    public String toString()
    {
        return "RedirectException( " + "\"" + to_uuid + "\"" + " )"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.to_uuid = ( String )from_hash_map.get( "to_uuid" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.to_uuid = ( String )from_array[0];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        to_uuid = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "to_uuid", to_uuid );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( to_uuid, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(to_uuid);
        return my_size;
    }


    private String to_uuid;

}

