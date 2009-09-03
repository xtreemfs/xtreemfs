package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class truncateResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082929;
    
    public truncateResponse() { osd_write_response = new OSDWriteResponse();  }
    public truncateResponse( OSDWriteResponse osd_write_response ) { this.osd_write_response = osd_write_response; }

    public OSDWriteResponse getOsd_write_response() { return osd_write_response; }
    public void setOsd_write_response( OSDWriteResponse osd_write_response ) { this.osd_write_response = osd_write_response; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082929;    

    // yidl.Object
    public int getTag() { return 2009082929; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::truncateResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += osd_write_response.getXDRSize();
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "osd_write_response", osd_write_response );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        osd_write_response = new OSDWriteResponse(); unmarshaller.readStruct( "osd_write_response", osd_write_response );    
    }
        
    

    private OSDWriteResponse osd_write_response;    

}

