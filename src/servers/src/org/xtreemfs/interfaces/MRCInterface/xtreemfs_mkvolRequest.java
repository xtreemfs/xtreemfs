package org.xtreemfs.interfaces.MRCInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_mkvolRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010012350;
    
    public xtreemfs_mkvolRequest() { volume = new Volume();  }
    public xtreemfs_mkvolRequest( Volume volume ) { this.volume = volume; }

    public Volume getVolume() { return volume; }
    public void setVolume( Volume volume ) { this.volume = volume; }

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

    // Request
    public Response createDefaultResponse() { return new xtreemfs_mkvolResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2010012350;    

    // yidl.runtime.Object
    public int getTag() { return 2010012350; }
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

