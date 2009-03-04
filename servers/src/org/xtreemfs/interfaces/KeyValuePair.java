package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class KeyValuePair implements org.xtreemfs.interfaces.utils.Serializable
{
    public KeyValuePair() { key = ""; value = ""; }
    public KeyValuePair( String key, String value ) { this.key = key; this.value = value; }

    public String getKey() { return key; }
    public void setKey( String key ) { this.key = key; }
    public String getValue() { return value; }
    public void setValue( String value ) { this.value = value; }

    // Object
    public String toString()
    {
        return "KeyValuePair( " + "\"" + key + "\"" + ", " + "\"" + value + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::KeyValuePair"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(key,writer); }
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(value,writer); }        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { key = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        { value = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4 + ( key.length() + 4 - ( key.length() % 4 ) );
        my_size += 4 + ( value.length() + 4 - ( value.length() % 4 ) );
        return my_size;
    }

    private String key;
    private String value;

}

