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




public class utimensRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009121131;
    
    public utimensRequest() {  }
    public utimensRequest( String path, long atime_ns, long mtime_ns, long ctime_ns ) { this.path = path; this.atime_ns = atime_ns; this.mtime_ns = mtime_ns; this.ctime_ns = ctime_ns; }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public long getAtime_ns() { return atime_ns; }
    public void setAtime_ns( long atime_ns ) { this.atime_ns = atime_ns; }
    public long getMtime_ns() { return mtime_ns; }
    public void setMtime_ns( long mtime_ns ) { this.mtime_ns = mtime_ns; }
    public long getCtime_ns() { return ctime_ns; }
    public void setCtime_ns( long ctime_ns ) { this.ctime_ns = ctime_ns; }

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
    public Response createDefaultResponse() { return new utimensResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009121131;    

    // yidl.runtime.Object
    public int getTag() { return 2009121131; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::utimensRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( path != null ? ( ( path.getBytes().length % 4 == 0 ) ? path.getBytes().length : ( path.getBytes().length + 4 - path.getBytes().length % 4 ) ) : 0 ); // path
        my_size += Long.SIZE / 8; // atime_ns
        my_size += Long.SIZE / 8; // mtime_ns
        my_size += Long.SIZE / 8; // ctime_ns
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "path", path );
        marshaller.writeUint64( "atime_ns", atime_ns );
        marshaller.writeUint64( "mtime_ns", mtime_ns );
        marshaller.writeUint64( "ctime_ns", ctime_ns );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        path = unmarshaller.readString( "path" );
        atime_ns = unmarshaller.readUint64( "atime_ns" );
        mtime_ns = unmarshaller.readUint64( "mtime_ns" );
        ctime_ns = unmarshaller.readUint64( "ctime_ns" );    
    }
        
    

    private String path;
    private long atime_ns;
    private long mtime_ns;
    private long ctime_ns;    

}

