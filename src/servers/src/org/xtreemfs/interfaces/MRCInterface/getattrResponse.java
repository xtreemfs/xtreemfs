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




public class getattrResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009090415;
    
    public getattrResponse() { stbuf = new Stat();  }
    public getattrResponse( Stat stbuf ) { this.stbuf = stbuf; }

    public Stat getStbuf() { return stbuf; }
    public void setStbuf( Stat stbuf ) { this.stbuf = stbuf; }

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
    public static final long serialVersionUID = 2009090415;    

    // yidl.runtime.Object
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

