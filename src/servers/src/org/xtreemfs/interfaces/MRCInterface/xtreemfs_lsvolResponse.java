package org.xtreemfs.interfaces.MRCInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_lsvolResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2010031151;

    public xtreemfs_lsvolResponse() { volumes = new StatVFSSet();  }
    public xtreemfs_lsvolResponse( StatVFSSet volumes ) { this.volumes = volumes; }

    public StatVFSSet getVolumes() { return volumes; }
    public void setVolumes( StatVFSSet volumes ) { this.volumes = volumes; }

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
    public static final long serialVersionUID = 2010031151;

    // yidl.runtime.Object
    public int getTag() { return 2010031151; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_lsvolResponse"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += volumes.getXDRSize(); // volumes
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeSequence( "volumes", volumes );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        volumes = new StatVFSSet(); unmarshaller.readSequence( "volumes", volumes );
    }

    private StatVFSSet volumes;
}
