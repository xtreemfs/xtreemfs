package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class getxattrResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009090416;
    
    public getxattrResponse() {  }
    public getxattrResponse( String value ) { this.value = value; }

    public String getValue() { return value; }
    public void setValue( String value ) { this.value = value; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009090416;    

    // yidl.Object
    public int getTag() { return 2009090416; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::getxattrResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( value != null ? ( ( value.getBytes().length % 4 == 0 ) ? value.getBytes().length : ( value.getBytes().length + 4 - value.getBytes().length % 4 ) ) : 0 ); // value
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "value", value );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        value = unmarshaller.readString( "value" );    
    }
        
    

    private String value;    

}

