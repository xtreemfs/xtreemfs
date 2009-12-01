package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class OSDtoMRCData implements Struct
{
    public static final int TAG = 2009120124;
    
    public OSDtoMRCData() {  }
    public OSDtoMRCData( int caching_policy, String data ) { this.caching_policy = caching_policy; this.data = data; }

    public int getCaching_policy() { return caching_policy; }
    public void setCaching_policy( int caching_policy ) { this.caching_policy = caching_policy; }
    public String getData() { return data; }
    public void setData( String data ) { this.data = data; }

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
    public static final long serialVersionUID = 2009120124;    

    // yidl.runtime.Object
    public int getTag() { return 2009120124; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDtoMRCData"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8; // caching_policy
        my_size += Integer.SIZE / 8 + ( data != null ? ( ( data.getBytes().length % 4 == 0 ) ? data.getBytes().length : ( data.getBytes().length + 4 - data.getBytes().length % 4 ) ) : 0 ); // data
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

