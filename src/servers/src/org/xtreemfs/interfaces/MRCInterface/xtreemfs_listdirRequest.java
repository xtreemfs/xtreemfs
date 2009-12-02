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




public class xtreemfs_listdirRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009120450;
    
    public xtreemfs_listdirRequest() {  }
    public xtreemfs_listdirRequest( String path ) { this.path = path; }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }

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

    // Request
    public Response createDefaultResponse() { return new xtreemfs_listdirResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009120450;    

    // yidl.runtime.Object
    public int getTag() { return 2009120450; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_listdirRequest"; }
    
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

