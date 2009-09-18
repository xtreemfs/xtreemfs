package org.xtreemfs.interfaces.OSDInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.PrettyPrinter;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_cleanup_statusResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082951;
    
    public xtreemfs_cleanup_statusResponse() {  }
    public xtreemfs_cleanup_statusResponse( String status ) { this.status = status; }

    public String getStatus() { return status; }
    public void setStatus( String status ) { this.status = status; }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082951;    

    // yidl.Object
    public int getTag() { return 2009082951; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_cleanup_statusResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( status != null ? ( ( status.getBytes().length % 4 == 0 ) ? status.getBytes().length : ( status.getBytes().length + 4 - status.getBytes().length % 4 ) ) : 0 ); // status
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "status", status );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        status = unmarshaller.readString( "status" );    
    }
        
    

    private String status;    

}

