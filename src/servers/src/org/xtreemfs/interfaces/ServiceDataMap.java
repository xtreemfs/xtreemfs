package org.xtreemfs.interfaces;

import java.io.StringWriter;
import java.util.Iterator;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Map;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Unmarshaller;




public class ServiceDataMap extends Map<String, String>
{
    public ServiceDataMap() { }
    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        string_writer.append(this.getClass().getCanonicalName());
        string_writer.append(" ");
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeMap( "", this );
        return string_writer.toString();
    }


    // yidl.runtime.Object
    public int getTag() { return 2010030346; }
    public String getTypeName() { return "org::xtreemfs::interfaces::ServiceDataMap"; }

    public int getXDRSize()
    {
        int my_size = 4; // The number of keys

        for ( Iterator<String> key_i = keySet().iterator(); key_i.hasNext(); )
        {
            String key = key_i.next();
            my_size += Integer.SIZE / 8 + ( key != null ? ( ( key.getBytes().length % 4 == 0 ) ? key.getBytes().length : ( key.getBytes().length + 4 - key.getBytes().length % 4 ) ) : 0 ); // Size of the key

            String value = get( key );
            my_size += Integer.SIZE / 8 + ( value != null ? ( ( value.getBytes().length % 4 == 0 ) ? value.getBytes().length : ( value.getBytes().length + 4 - value.getBytes().length % 4 ) ) : 0 ); // Size of the value
        }

        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        for ( Iterator<String> key_i = keySet().iterator(); key_i.hasNext(); )
        {
             String key = key_i.next();
             String value = get( key );
             marshaller.writeString( key, value );
        }
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        String key = unmarshaller.readString( null );
        if ( key != null )
        {
            String value;
            value = unmarshaller.readString( key );
            put( key, value );
        }
    }

}

