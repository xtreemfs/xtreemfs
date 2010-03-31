package org.xtreemfs.interfaces.OSDInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_rwr_statusResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2010031292;

    public xtreemfs_rwr_statusResponse() { local_state = new ReplicaStatus();  }
    public xtreemfs_rwr_statusResponse( ReplicaStatus local_state ) { this.local_state = local_state; }

    public ReplicaStatus getLocal_state() { return local_state; }
    public void setLocal_state( ReplicaStatus local_state ) { this.local_state = local_state; }

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
    public static final long serialVersionUID = 2010031292;

    // yidl.runtime.Object
    public int getTag() { return 2010031292; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_rwr_statusResponse"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += local_state.getXDRSize(); // local_state
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "local_state", local_state );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        local_state = new ReplicaStatus(); unmarshaller.readStruct( "local_state", local_state );
    }

    private ReplicaStatus local_state;
}
