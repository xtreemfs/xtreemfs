package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_pingRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082978;
    
    public xtreemfs_pingRequest() { coordinates = new VivaldiCoordinates();  }
    public xtreemfs_pingRequest( VivaldiCoordinates coordinates ) { this.coordinates = coordinates; }

    public VivaldiCoordinates getCoordinates() { return coordinates; }
    public void setCoordinates( VivaldiCoordinates coordinates ) { this.coordinates = coordinates; }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_pingResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082978;    

    // yidl.Object
    public int getTag() { return 2009082978; }
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

