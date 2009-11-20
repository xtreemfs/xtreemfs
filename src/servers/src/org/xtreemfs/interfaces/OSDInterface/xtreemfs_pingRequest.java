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




public class xtreemfs_pingRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009112374;
    
    public xtreemfs_pingRequest() { coordinates = new VivaldiCoordinates();  }
    public xtreemfs_pingRequest( VivaldiCoordinates coordinates ) { this.coordinates = coordinates; }

    public VivaldiCoordinates getCoordinates() { return coordinates; }
    public void setCoordinates( VivaldiCoordinates coordinates ) { this.coordinates = coordinates; }

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
    public Response createDefaultResponse() { return new xtreemfs_pingResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009112374;    

    // yidl.runtime.Object
    public int getTag() { return 2009112374; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_pingRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += coordinates.getXDRSize(); // coordinates
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "coordinates", coordinates );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        coordinates = new VivaldiCoordinates(); unmarshaller.readStruct( "coordinates", coordinates );    
    }
        
    

    private VivaldiCoordinates coordinates;    

}

