package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class openResponse implements org.xtreemfs.interfaces.utils.Response
{
    public openResponse() { credentials = new FileCredentials(); }
    public openResponse( FileCredentials credentials ) { this.credentials = credentials; }
    public openResponse( Object from_hash_map ) { credentials = new FileCredentials(); this.deserialize( from_hash_map ); }
    public openResponse( Object[] from_array ) { credentials = new FileCredentials();this.deserialize( from_array ); }

    public FileCredentials getCredentials() { return credentials; }
    public void setCredentials( FileCredentials credentials ) { this.credentials = credentials; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::openResponse"; }    
    public long getTypeId() { return 11; }

    public String toString()
    {
        return "openResponse( " + credentials.toString() + " )"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.credentials.deserialize( from_hash_map.get( "credentials" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.credentials.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        credentials = new FileCredentials(); credentials.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "credentials", credentials.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        credentials.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += credentials.calculateSize();
        return my_size;
    }

    // Response
    public int getOperationNumber() { return 11; }


    private FileCredentials credentials;

}

