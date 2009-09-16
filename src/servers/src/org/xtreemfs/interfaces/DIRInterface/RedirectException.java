package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class RedirectException extends org.xtreemfs.interfaces.utils.ONCRPCException
{
    public static final int TAG = 2009082741;
    
    public RedirectException() {  }
    public RedirectException( String address, int port ) { this.address = address; this.port = port; }

    public String getAddress() { return address; }
    public void setAddress( String address ) { this.address = address; }
    public int getPort() { return port; }
    public void setPort( int port ) { this.port = port; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082741;    

    // yidl.Object
    public int getTag() { return 2009082741; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::RedirectException"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( address != null ? ( ( address.getBytes().length % 4 == 0 ) ? address.getBytes().length : ( address.getBytes().length + 4 - address.getBytes().length % 4 ) ) : 0 ); // address
        my_size += Integer.SIZE / 8; // port
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "address", address );
        marshaller.writeUint16( "port", port );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        address = unmarshaller.readString( "address" );
        port = unmarshaller.readUint16( "port" );    
    }
        
    

    private String address;
    private int port;    

}

