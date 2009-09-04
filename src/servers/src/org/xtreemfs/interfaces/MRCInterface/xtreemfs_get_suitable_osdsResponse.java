package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_get_suitable_osdsResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082851;
    
    public xtreemfs_get_suitable_osdsResponse() { osd_uuids = new StringSet();  }
    public xtreemfs_get_suitable_osdsResponse( StringSet osd_uuids ) { this.osd_uuids = osd_uuids; }

    public StringSet getOsd_uuids() { return osd_uuids; }
    public void setOsd_uuids( StringSet osd_uuids ) { this.osd_uuids = osd_uuids; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082851;    

    // yidl.Object
    public int getTag() { return 2009082851; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_get_suitable_osdsResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += osd_uuids.getXDRSize(); // osd_uuids
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeSequence( "osd_uuids", osd_uuids );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        osd_uuids = new StringSet(); unmarshaller.readSequence( "osd_uuids", osd_uuids );    
    }
        
    

    private StringSet osd_uuids;    

}

