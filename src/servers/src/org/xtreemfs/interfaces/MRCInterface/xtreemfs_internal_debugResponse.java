package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_internal_debugResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009090443;
    
    public xtreemfs_internal_debugResponse() {  }
    public xtreemfs_internal_debugResponse( String result ) { this.result = result; }

    public String getResult() { return result; }
    public void setResult( String result ) { this.result = result; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009090443;    

    // yidl.Object
    public int getTag() { return 2009090443; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_internal_debugResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += ( ( result.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( result.getBytes().length + Integer.SIZE/8 ) : ( result.getBytes().length + Integer.SIZE/8 + 4 - ( result.getBytes().length + Integer.SIZE/8 ) % 4 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "result", result );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        result = unmarshaller.readString( "result" );    
    }
        
    

    private String result;    

}

