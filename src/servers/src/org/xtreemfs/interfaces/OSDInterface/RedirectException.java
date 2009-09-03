package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class RedirectException extends org.xtreemfs.interfaces.utils.ONCRPCException
{
    public static final int TAG = 2009082924;
    
    public RedirectException() {  }
    public RedirectException( String to_uuid ) { this.to_uuid = to_uuid; }

    public String getTo_uuid() { return to_uuid; }
    public void setTo_uuid( String to_uuid ) { this.to_uuid = to_uuid; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082924;    

    // yidl.Object
    public int getTag() { return 2009082924; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::RedirectException"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += to_uuid != null ? ( ( to_uuid.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( to_uuid.getBytes().length + Integer.SIZE/8 ) : ( to_uuid.getBytes().length + Integer.SIZE/8 + 4 - ( to_uuid.getBytes().length + Integer.SIZE/8 ) % 4 ) : 0;
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "to_uuid", to_uuid );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        to_uuid = unmarshaller.readString( "to_uuid" );    
    }
        
    

    private String to_uuid;    

}

