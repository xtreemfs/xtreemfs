package org.xtreemfs.interfaces;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class OSDtoMRCData extends Struct
{
    public static final int TAG = 2009090222;
    
    public OSDtoMRCData() {  }
    public OSDtoMRCData( int caching_policy, String data ) { this.caching_policy = caching_policy; this.data = data; }

    public int getCaching_policy() { return caching_policy; }
    public void setCaching_policy( int caching_policy ) { this.caching_policy = caching_policy; }
    public String getData() { return data; }
    public void setData( String data ) { this.data = data; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009090222;    

    // yidl.Object
    public int getTag() { return 2009090222; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDtoMRCData"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += ( Integer.SIZE / 8 );
        my_size += Integer.SIZE/8 + ( data != null ? ( ( data.getBytes().length % 4 == 0 ) ? data.getBytes().length : ( data.getBytes().length + 4 - data.getBytes().length % 4 ) ) : 0 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeUint8( "caching_policy", caching_policy );
        marshaller.writeString( "data", data );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        caching_policy = unmarshaller.readUint8( "caching_policy" );
        data = unmarshaller.readString( "data" );    
    }
        
    

    private int caching_policy;
    private String data;    

}

