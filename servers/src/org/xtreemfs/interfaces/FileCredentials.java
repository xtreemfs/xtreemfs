package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class FileCredentials implements org.xtreemfs.interfaces.utils.Serializable
{
    public FileCredentials() { xlocs = new XLocSet(); xcap = new XCap(); }
    public FileCredentials( XLocSet xlocs, XCap xcap ) { this.xlocs = xlocs; this.xcap = xcap; }
    public FileCredentials( Object from_hash_map ) { xlocs = new XLocSet(); xcap = new XCap(); this.deserialize( from_hash_map ); }
    public FileCredentials( Object[] from_array ) { xlocs = new XLocSet(); xcap = new XCap();this.deserialize( from_array ); }

    public XLocSet getXlocs() { return xlocs; }
    public void setXlocs( XLocSet xlocs ) { this.xlocs = xlocs; }
    public XCap getXcap() { return xcap; }
    public void setXcap( XCap xcap ) { this.xcap = xcap; }

    // Serializable
    public String getTypeName() { return "org::xtreemfs::interfaces::FileCredentials"; }    
    public long getTypeId() { return 0; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.xlocs.deserialize( from_hash_map.get( "xlocs" ) );
        this.xcap.deserialize( from_hash_map.get( "xcap" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.xlocs.deserialize( from_array[0] );
        this.xcap.deserialize( from_array[1] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        xlocs = new XLocSet(); xlocs.deserialize( buf );
        xcap = new XCap(); xcap.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "xlocs", xlocs.serialize() );
        to_hash_map.put( "xcap", xcap.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        xlocs.serialize( writer );
        xcap.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += xlocs.calculateSize();
        my_size += xcap.calculateSize();
        return my_size;
    }


    private XLocSet xlocs;
    private XCap xcap;

}

