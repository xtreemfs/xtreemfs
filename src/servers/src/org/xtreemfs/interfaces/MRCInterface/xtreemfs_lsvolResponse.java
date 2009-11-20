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




public class xtreemfs_lsvolResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009112249;
    
    public xtreemfs_lsvolResponse() { volumes = new VolumeSet();  }
    public xtreemfs_lsvolResponse( VolumeSet volumes ) { this.volumes = volumes; }

    public VolumeSet getVolumes() { return volumes; }
    public void setVolumes( VolumeSet volumes ) { this.volumes = volumes; }

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
    public static final long serialVersionUID = 2009112249;    

    // yidl.runtime.Object
    public int getTag() { return 2009112249; }
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

