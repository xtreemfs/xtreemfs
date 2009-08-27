package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_cleanup_get_resultsResponse implements org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082948;

    
    public xtreemfs_cleanup_get_resultsResponse() { results = new StringSet(); }
    public xtreemfs_cleanup_get_resultsResponse( StringSet results ) { this.results = results; }
    public xtreemfs_cleanup_get_resultsResponse( Object from_hash_map ) { results = new StringSet(); this.deserialize( from_hash_map ); }
    public xtreemfs_cleanup_get_resultsResponse( Object[] from_array ) { results = new StringSet();this.deserialize( from_array ); }

    public StringSet getResults() { return results; }
    public void setResults( StringSet results ) { this.results = results; }

    // Object
    public String toString()
    {
        return "xtreemfs_cleanup_get_resultsResponse( " + results.toString() + " )";
    }

    // Serializable
    public int getTag() { return 2009082948; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_cleanup_get_resultsResponse"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.results.deserialize( ( Object[] )from_hash_map.get( "results" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.results.deserialize( ( Object[] )from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        results = new StringSet(); results.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "results", results.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        results.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += results.calculateSize();
        return my_size;
    }


    private StringSet results;    

}

