package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_cleanup_get_resultsResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082948;
    
    public xtreemfs_cleanup_get_resultsResponse() { results = new StringSet();  }
    public xtreemfs_cleanup_get_resultsResponse( StringSet results ) { this.results = results; }

    public StringSet getResults() { return results; }
    public void setResults( StringSet results ) { this.results = results; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082948;    

    // yidl.Object
    public int getTag() { return 2009082948; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_cleanup_get_resultsResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += results.getXDRSize();
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeSequence( "results", results );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        results = new StringSet(); unmarshaller.readSequence( "results", results );    
    }
        
    

    private StringSet results;    

}

