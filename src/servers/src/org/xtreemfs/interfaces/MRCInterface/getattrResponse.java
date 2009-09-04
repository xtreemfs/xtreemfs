package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class getattrResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009090415;
    
    public getattrResponse() { stbuf = new Stat();  }
    public getattrResponse( Stat stbuf ) { this.stbuf = stbuf; }

    public Stat getStbuf() { return stbuf; }
    public void setStbuf( Stat stbuf ) { this.stbuf = stbuf; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009090415;    

    // yidl.Object
    public int getTag() { return 2009090415; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::getattrResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += stbuf.getXDRSize(); // stbuf
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "stbuf", stbuf );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        stbuf = new Stat(); unmarshaller.readStruct( "stbuf", stbuf );    
    }
        
    

    private Stat stbuf;    

}

