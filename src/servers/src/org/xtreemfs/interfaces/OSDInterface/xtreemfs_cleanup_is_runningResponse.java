package org.xtreemfs.interfaces.OSDInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_cleanup_is_runningResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009121241;
    
    public xtreemfs_cleanup_is_runningResponse() {  }
    public xtreemfs_cleanup_is_runningResponse( boolean is_running ) { this.is_running = is_running; }

    public boolean getIs_running() { return is_running; }
    public void setIs_running( boolean is_running ) { this.is_running = is_running; }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        string_writer.append(this.getClass().getCanonicalName());
        string_writer.append(" ");
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }


    // java.io.Serializable
    public static final long serialVersionUID = 2009121241;    

    // yidl.runtime.Object
    public int getTag() { return 2009121241; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_cleanup_is_runningResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8; // is_running
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

