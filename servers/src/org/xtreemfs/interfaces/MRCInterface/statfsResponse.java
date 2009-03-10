package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class statfsResponse implements org.xtreemfs.interfaces.utils.Response
{
    public statfsResponse() { statfsbuf = new statfs_(); }
    public statfsResponse( statfs_ statfsbuf ) { this.statfsbuf = statfsbuf; }
    public statfsResponse( Object from_hash_map ) { statfsbuf = new statfs_(); this.deserialize( from_hash_map ); }
    public statfsResponse( Object[] from_array ) { statfsbuf = new statfs_();this.deserialize( from_array ); }

    public statfs_ getStatfsbuf() { return statfsbuf; }
    public void setStatfsbuf( statfs_ statfsbuf ) { this.statfsbuf = statfsbuf; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::statfsResponse"; }    
    public long getTypeId() { return 19; }

    public String toString()
    {
        return "statfsResponse( " + statfsbuf.toString() + " )"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.statfsbuf.deserialize( from_hash_map.get( "statfsbuf" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.statfsbuf.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        statfsbuf = new statfs_(); statfsbuf.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "statfsbuf", statfsbuf.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        statfsbuf.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += statfsbuf.calculateSize();
        return my_size;
    }

    // Response
    public int getOperationNumber() { return 19; }


    private statfs_ statfsbuf;

}

