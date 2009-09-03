package org.xtreemfs.interfaces;

import java.util.Iterator;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Map;
import yidl.Marshaller;
import yidl.Unmarshaller;




public class ServiceDataMap extends Map<String, String>
{
    public ServiceDataMap() { }

    // yidl.Object
    public int getTag() { return 2009082650; }
    public String getTypeName() { return "org::xtreemfs::interfaces::ServiceDataMap"; }

    public int getXDRSize() 
    {
        int my_size = Integer.SIZE / 8;
        for ( Iterator<String> key_i = keySet().iterator(); key_i.hasNext(); ) 
        {
            String key = key_i.next();
            String value = get( key );
            my_size += ( ( value.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( value.getBytes().length + Integer.SIZE/8 ) : ( value.getBytes().length + Integer.SIZE/8 + 4 - ( value.getBytes().length + Integer.SIZE/8 ) % 4 );
        my_size += ( ( key.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( key.getBytes().length + Integer.SIZE/8 ) : ( key.getBytes().length + Integer.SIZE/8 + 4 - ( key.getBytes().length + Integer.SIZE/8 ) % 4 );
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
    
