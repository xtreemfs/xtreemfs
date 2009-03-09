package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class DirectoryEntry implements org.xtreemfs.interfaces.utils.Serializable
{
    public DirectoryEntry() { entry_name = ""; stbuf = new stat_(); link_target = ""; }
    public DirectoryEntry( String entry_name, stat_ stbuf, String link_target ) { this.entry_name = entry_name; this.stbuf = stbuf; this.link_target = link_target; }
    public DirectoryEntry( Object from_hash_map ) { entry_name = ""; stbuf = new stat_(); link_target = ""; this.deserialize( from_hash_map ); }
    public DirectoryEntry( Object[] from_array ) { entry_name = ""; stbuf = new stat_(); link_target = "";this.deserialize( from_array ); }

    public String getEntry_name() { return entry_name; }
    public void setEntry_name( String entry_name ) { this.entry_name = entry_name; }
    public stat_ getStbuf() { return stbuf; }
    public void setStbuf( stat_ stbuf ) { this.stbuf = stbuf; }
    public String getLink_target() { return link_target; }
    public void setLink_target( String link_target ) { this.link_target = link_target; }

    // Serializable
    public String getTypeName() { return "org::xtreemfs::interfaces::DirectoryEntry"; }    
    public long getTypeId() { return 0; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.entry_name = ( String )from_hash_map.get( "entry_name" );
        this.stbuf.deserialize( from_hash_map.get( "stbuf" ) );
        this.link_target = ( String )from_hash_map.get( "link_target" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.entry_name = ( String )from_array[0];
        this.stbuf.deserialize( from_array[1] );
        this.link_target = ( String )from_array[2];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        entry_name = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        stbuf = new stat_(); stbuf.deserialize( buf );
        link_target = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "entry_name", entry_name );
        to_hash_map.put( "stbuf", stbuf.serialize() );
        to_hash_map.put( "link_target", link_target );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( entry_name, writer );
        stbuf.serialize( writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( link_target, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(entry_name);
        my_size += stbuf.calculateSize();
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(link_target);
        return my_size;
    }


    private String entry_name;
    private stat_ stbuf;
    private String link_target;

}

