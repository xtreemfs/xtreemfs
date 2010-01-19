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




public class closeRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010012115;
    
    public closeRequest() { client_vivaldi_coordinates = new VivaldiCoordinates(); write_xcap = new XCap();  }
    public closeRequest( VivaldiCoordinates client_vivaldi_coordinates, XCap write_xcap ) { this.client_vivaldi_coordinates = client_vivaldi_coordinates; this.write_xcap = write_xcap; }

    public VivaldiCoordinates getClient_vivaldi_coordinates() { return client_vivaldi_coordinates; }
    public void setClient_vivaldi_coordinates( VivaldiCoordinates client_vivaldi_coordinates ) { this.client_vivaldi_coordinates = client_vivaldi_coordinates; }
    public XCap getWrite_xcap() { return write_xcap; }
    public void setWrite_xcap( XCap write_xcap ) { this.write_xcap = write_xcap; }

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
    public Response createDefaultResponse() { return new closeResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2010012115;    

    // yidl.runtime.Object
    public int getTag() { return 2010012115; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::closeRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += client_vivaldi_coordinates.getXDRSize(); // client_vivaldi_coordinates
        my_size += write_xcap.getXDRSize(); // write_xcap
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "client_vivaldi_coordinates", client_vivaldi_coordinates );
        marshaller.writeStruct( "write_xcap", write_xcap );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        client_vivaldi_coordinates = new VivaldiCoordinates(); unmarshaller.readStruct( "client_vivaldi_coordinates", client_vivaldi_coordinates );
        write_xcap = new XCap(); unmarshaller.readStruct( "write_xcap", write_xcap );    
    }
        
    

    private VivaldiCoordinates client_vivaldi_coordinates;
    private XCap write_xcap;    

}

