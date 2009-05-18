package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class DirectoryEntry implements org.xtreemfs.interfaces.utils.Serializable
{
    public DirectoryEntry() { name = ""; stbuf = new Stat(); }
    public DirectoryEntry( String name, Stat stbuf ) { this.name = name; this.stbuf = stbuf; }
    public DirectoryEntry( Object from_hash_map ) { name = ""; stbuf = new Stat(); this.deserialize( from_hash_map ); }
    public DirectoryEntry( Object[] from_array ) { name = ""; stbuf = new Stat();this.deserialize( from_array ); }

    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }
    public Stat getStbuf() { return stbuf; }
    public void setStbuf( Stat stbuf ) { this.stbuf = stbuf; }

    public long getTag() { return 1041; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DirectoryEntry"; }

    public String toString()
    {
        return "DirectoryEntry( " + "\"" + name + "\"" + ", " + stbuf.toString() + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.name = ( String )from_hash_map.get( "name" );
        this.stbuf.deserialize( from_hash_map.get( "stbuf" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.name = ( String )from_array[0];
        this.stbuf.deserialize( from_array[1] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        name = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        stbuf = new Stat(); stbuf.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "name", name );
        to_hash_map.put( "stbuf", stbuf.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( name, writer );
        stbuf.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(name);
        my_size += stbuf.calculateSize();
        return my_size;
    }


    private String name;
    private Stat stbuf;    

}

