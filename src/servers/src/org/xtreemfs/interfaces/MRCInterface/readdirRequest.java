package org.xtreemfs.interfaces.MRCInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.PrettyPrinter;
import yidl.Struct;
import yidl.Unmarshaller;




public class readdirRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009090421;
    
    public readdirRequest() {  }
    public readdirRequest( String path ) { this.path = path; }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }

    // Request
    public Response createDefaultResponse() { return new readdirResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009090421;    

    // yidl.Object
    public int getTag() { return 2009090421; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::readdirRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( path != null ? ( ( path.getBytes().length % 4 == 0 ) ? path.getBytes().length : ( path.getBytes().length + 4 - path.getBytes().length % 4 ) ) : 0 ); // path
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "path", path );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        path = unmarshaller.readString( "path" );    
    }
        
    

    private String path;    

}

