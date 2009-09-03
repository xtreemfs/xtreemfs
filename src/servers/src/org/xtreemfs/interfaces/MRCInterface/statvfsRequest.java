package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class statvfsRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009090427;
    
    public statvfsRequest() {  }
    public statvfsRequest( String volume_name ) { this.volume_name = volume_name; }

    public String getVolume_name() { return volume_name; }
    public void setVolume_name( String volume_name ) { this.volume_name = volume_name; }

    // Request
    public Response createDefaultResponse() { return new statvfsResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009090427;    

    // yidl.Object
    public int getTag() { return 2009090427; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::statvfsRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += ( ( volume_name.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( volume_name.getBytes().length + Integer.SIZE/8 ) : ( volume_name.getBytes().length + Integer.SIZE/8 + 4 - ( volume_name.getBytes().length + Integer.SIZE/8 ) % 4 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "volume_name", volume_name );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        volume_name = unmarshaller.readString( "volume_name" );    
    }
        
    

    private String volume_name;    

}

