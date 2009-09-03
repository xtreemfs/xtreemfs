package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_pingResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082978;
    
    public xtreemfs_pingResponse() { remote_coordinates = new VivaldiCoordinates();  }
    public xtreemfs_pingResponse( VivaldiCoordinates remote_coordinates ) { this.remote_coordinates = remote_coordinates; }

    public VivaldiCoordinates getRemote_coordinates() { return remote_coordinates; }
    public void setRemote_coordinates( VivaldiCoordinates remote_coordinates ) { this.remote_coordinates = remote_coordinates; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082978;    

    // yidl.Object
    public int getTag() { return 2009082978; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_pingResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += remote_coordinates.getXDRSize();
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "remote_coordinates", remote_coordinates );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        remote_coordinates = new VivaldiCoordinates(); unmarshaller.readStruct( "remote_coordinates", remote_coordinates );    
    }
        
    

    private VivaldiCoordinates remote_coordinates;    

}

