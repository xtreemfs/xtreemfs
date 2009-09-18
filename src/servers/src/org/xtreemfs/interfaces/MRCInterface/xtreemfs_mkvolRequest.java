package org.xtreemfs.interfaces.MRCInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.PrettyPrinter;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_mkvolRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009090446;
    
    public xtreemfs_mkvolRequest() { volume = new Volume();  }
    public xtreemfs_mkvolRequest( Volume volume ) { this.volume = volume; }

    public Volume getVolume() { return volume; }
    public void setVolume( Volume volume ) { this.volume = volume; }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_mkvolResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009090446;    

    // yidl.Object
    public int getTag() { return 2009090446; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_mkvolRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += volume.getXDRSize(); // volume
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "volume", volume );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        volume = new Volume(); unmarshaller.readStruct( "volume", volume );    
    }
        
    

    private Volume volume;    

}

