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




public class statvfsResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2010012131;
    
    public statvfsResponse() { stbuf = new StatVFS();  }
    public statvfsResponse( StatVFS stbuf ) { this.stbuf = stbuf; }

    public StatVFS getStbuf() { return stbuf; }
    public void setStbuf( StatVFS stbuf ) { this.stbuf = stbuf; }

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
    public static final long serialVersionUID = 2010012131;    

    // yidl.runtime.Object
    public int getTag() { return 2010012131; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::statvfsResponse"; }
    
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
        stbuf = new StatVFS(); unmarshaller.readStruct( "stbuf", stbuf );    
    }
        
    

    private StatVFS stbuf;    

}

