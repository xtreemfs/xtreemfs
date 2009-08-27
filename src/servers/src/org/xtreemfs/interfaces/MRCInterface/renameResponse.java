package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class renameResponse implements org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082832;

    
    public renameResponse() { file_credentials = new FileCredentialsSet(); }
    public renameResponse( FileCredentialsSet file_credentials ) { this.file_credentials = file_credentials; }
    public renameResponse( Object from_hash_map ) { file_credentials = new FileCredentialsSet(); this.deserialize( from_hash_map ); }
    public renameResponse( Object[] from_array ) { file_credentials = new FileCredentialsSet();this.deserialize( from_array ); }

    public FileCredentialsSet getFile_credentials() { return file_credentials; }
    public void setFile_credentials( FileCredentialsSet file_credentials ) { this.file_credentials = file_credentials; }

    // Object
    public String toString()
    {
        return "renameResponse( " + file_credentials.toString() + " )";
    }

    // Serializable
    public int getTag() { return 2009082832; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::renameResponse"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.file_credentials.deserialize( ( Object[] )from_hash_map.get( "file_credentials" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.file_credentials.deserialize( ( Object[] )from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        file_credentials = new FileCredentialsSet(); file_credentials.deserialize( buf );
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


    private FileCredentialsSet file_credentials;    

}

