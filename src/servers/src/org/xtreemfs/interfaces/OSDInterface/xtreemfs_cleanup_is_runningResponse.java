package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_cleanup_is_runningResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082949;
    
    public xtreemfs_cleanup_is_runningResponse() {  }
    public xtreemfs_cleanup_is_runningResponse( boolean is_running ) { this.is_running = is_running; }

    public boolean getIs_running() { return is_running; }
    public void setIs_running( boolean is_running ) { this.is_running = is_running; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082949;    

    // yidl.Object
    public int getTag() { return 2009082949; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_cleanup_is_runningResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += 4;
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeBoolean( "is_running", is_running );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        is_running = unmarshaller.readBoolean( "is_running" );    
    }
        
    

    private boolean is_running;    

}

