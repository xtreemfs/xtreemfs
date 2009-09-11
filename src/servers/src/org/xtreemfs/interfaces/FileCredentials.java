package org.xtreemfs.interfaces;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class FileCredentials extends Struct
{
    public static final int TAG = 2009090234;
    
    public FileCredentials() { xlocs = new XLocSet(); xcap = new XCap();  }
    public FileCredentials( XLocSet xlocs, XCap xcap ) { this.xlocs = xlocs; this.xcap = xcap; }

    public XLocSet getXlocs() { return xlocs; }
    public void setXlocs( XLocSet xlocs ) { this.xlocs = xlocs; }
    public XCap getXcap() { return xcap; }
    public void setXcap( XCap xcap ) { this.xcap = xcap; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009090234;    

    // yidl.Object
    public int getTag() { return 2009090234; }
    public String getTypeName() { return "org::xtreemfs::interfaces::FileCredentials"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += xlocs.getXDRSize(); // xlocs
        my_size += xcap.getXDRSize(); // xcap
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "xlocs", xlocs );
        marshaller.writeStruct( "xcap", xcap );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        xlocs = new XLocSet(); unmarshaller.readStruct( "xlocs", xlocs );
        xcap = new XCap(); unmarshaller.readStruct( "xcap", xcap );    
    }
        
    

    private XLocSet xlocs;
    private XCap xcap;    

}

