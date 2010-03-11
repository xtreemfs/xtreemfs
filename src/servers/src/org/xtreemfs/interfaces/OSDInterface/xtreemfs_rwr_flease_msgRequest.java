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




public class xtreemfs_rwr_flease_msgRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010031287;

    public xtreemfs_rwr_flease_msgRequest() {  }
    public xtreemfs_rwr_flease_msgRequest( ReusableBuffer fleaseMessage, String senderHostname, int senderPort ) { this.fleaseMessage = fleaseMessage; this.senderHostname = senderHostname; this.senderPort = senderPort; }

    public ReusableBuffer getFleaseMessage() { return fleaseMessage; }
    public String getSenderHostname() { return senderHostname; }
    public int getSenderPort() { return senderPort; }
    public void setFleaseMessage( ReusableBuffer fleaseMessage ) { this.fleaseMessage = fleaseMessage; }
    public void setSenderHostname( String senderHostname ) { this.senderHostname = senderHostname; }
    public void setSenderPort( int senderPort ) { this.senderPort = senderPort; }

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
    public Response createDefaultResponse() { return new xtreemfs_rwr_flease_msgResponse(); }

    // java.io.Serializable
    public static final long serialVersionUID = 2010031287;

    // yidl.runtime.Object
    public int getTag() { return 2010031287; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_rwr_flease_msgRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( fleaseMessage != null ? ( ( fleaseMessage.remaining() % 4 == 0 ) ? fleaseMessage.remaining() : ( fleaseMessage.remaining() + 4 - fleaseMessage.remaining() % 4 ) ) : 0 ); // fleaseMessage
        my_size += Integer.SIZE / 8 + ( senderHostname != null ? ( ( senderHostname.getBytes().length % 4 == 0 ) ? senderHostname.getBytes().length : ( senderHostname.getBytes().length + 4 - senderHostname.getBytes().length % 4 ) ) : 0 ); // senderHostname
        my_size += Integer.SIZE / 8; // senderPort
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeBuffer( "fleaseMessage", fleaseMessage );
        marshaller.writeString( "senderHostname", senderHostname );
        marshaller.writeUint32( "senderPort", senderPort );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        fleaseMessage = ( ReusableBuffer )unmarshaller.readBuffer( "fleaseMessage" );
        senderHostname = unmarshaller.readString( "senderHostname" );
        senderPort = unmarshaller.readUint32( "senderPort" );
    }

    private ReusableBuffer fleaseMessage;
    private String senderHostname;
    private int senderPort;
}
