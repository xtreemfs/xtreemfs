package org.xtreemfs.interfaces.OSDInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_pingResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2010030674;

    public xtreemfs_pingResponse() { remote_coordinates = new VivaldiCoordinates();  }
    public xtreemfs_pingResponse( VivaldiCoordinates remote_coordinates ) { this.remote_coordinates = remote_coordinates; }

    public VivaldiCoordinates getRemote_coordinates() { return remote_coordinates; }
    public void setRemote_coordinates( VivaldiCoordinates remote_coordinates ) { this.remote_coordinates = remote_coordinates; }

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
    public static final long serialVersionUID = 2010030674;

    // yidl.runtime.Object
    public int getTag() { return 2010030674; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_pingResponse"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += remote_coordinates.getXDRSize(); // remote_coordinates
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "remote_coordinates", remote_coordinates );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        remote_coordinates = new VivaldiCoordinates(); unmarshaller.readStruct( "remote_coordinates", remote_coordinates );
    }

    

    private VivaldiCoordinates remote_coordinates;

}

