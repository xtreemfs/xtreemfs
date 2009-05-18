package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class openResponse implements org.xtreemfs.interfaces.utils.Response
{
    public openResponse() { file_credentials = new FileCredentials(); }
    public openResponse( FileCredentials file_credentials ) { this.file_credentials = file_credentials; }
    public openResponse( Object from_hash_map ) { file_credentials = new FileCredentials(); this.deserialize( from_hash_map ); }
    public openResponse( Object[] from_array ) { file_credentials = new FileCredentials();this.deserialize( from_array ); }

    public FileCredentials getFile_credentials() { return file_credentials; }
    public void setFile_credentials( FileCredentials file_credentials ) { this.file_credentials = file_credentials; }

    public long getTag() { return 1211; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::openResponse"; }

    public String toString()
    {
        return "openResponse( " + file_credentials.toString() + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.file_credentials.deserialize( from_hash_map.get( "file_credentials" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.file_credentials.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        file_credentials = new FileCredentials(); file_credentials.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "file_credentials", file_credentials.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        file_credentials.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += file_credentials.calculateSize();
        return my_size;
    }

    // Response
    public int getOperationNumber() { return 1211; }


    private FileCredentials file_credentials;    

}

