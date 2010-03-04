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




public class xtreemfs_replica_removeResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2010030555;

    public xtreemfs_replica_removeResponse() { delete_xcap = new XCap();  }
    public xtreemfs_replica_removeResponse( XCap delete_xcap ) { this.delete_xcap = delete_xcap; }

    public XCap getDelete_xcap() { return delete_xcap; }
    public void setDelete_xcap( XCap delete_xcap ) { this.delete_xcap = delete_xcap; }

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
    public static final long serialVersionUID = 2010030555;

    // yidl.runtime.Object
    public int getTag() { return 2010030555; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_removeResponse"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += delete_xcap.getXDRSize(); // delete_xcap
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "delete_xcap", delete_xcap );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        delete_xcap = new XCap(); unmarshaller.readStruct( "delete_xcap", delete_xcap );
    }

    

    private XCap delete_xcap;

}

