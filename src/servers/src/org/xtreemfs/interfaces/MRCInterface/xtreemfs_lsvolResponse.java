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




public class xtreemfs_lsvolResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009090444;
    
    public xtreemfs_lsvolResponse() { volumes = new VolumeSet();  }
    public xtreemfs_lsvolResponse( VolumeSet volumes ) { this.volumes = volumes; }

    public VolumeSet getVolumes() { return volumes; }
    public void setVolumes( VolumeSet volumes ) { this.volumes = volumes; }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }


    // java.io.Serializable
    public static final long serialVersionUID = 2009090444;    

    // yidl.Object
    public int getTag() { return 2009090444; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_lsvolResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += volumes.getXDRSize(); // volumes
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeSequence( "volumes", volumes );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        volumes = new VolumeSet(); unmarshaller.readSequence( "volumes", volumes );    
    }
        
    

    private VolumeSet volumes;    

}

