package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.MRCInterface.*;
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         

public class setxattrRequest implements Request
{
    public setxattrRequest() { context = new org.xtreemfs.interfaces.Context(); path = ""; name = ""; value = ""; flags = 0; }
    public setxattrRequest( Context context, String path, String name, String value, int flags ) { this.context = context; this.path = path; this.name = name; this.value = value; this.flags = flags; }

    public Context getContext() { return context; }
    public void setContext( Context context ) { this.context = context; }
    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }
    public String getValue() { return value; }
    public void setValue( String value ) { this.value = value; }
    public int getFlags() { return flags; }
    public void setFlags( int flags ) { this.flags = flags; }

    // Object
    public String toString()
    {
        return "setxattrRequest( " + context.toString() + ", " + "\"" + path + "\"" + ", " + "\"" + name + "\"" + ", " + "\"" + value + "\"" + ", " + Integer.toString( flags ) + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::setxattrRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        context.serialize( writer );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(path,writer); }
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(name,writer); }
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(value,writer); }
        writer.putInt( flags );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        context = new org.xtreemfs.interfaces.Context(); context.deserialize( buf );
        { path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        { name = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        { value = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        flags = buf.getInt();    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += context.calculateSize();
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(path);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(name);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(value);
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }

    private Context context;
    private String path;
    private String name;
    private String value;
    private int flags;
    

    // Request
    public int getInterfaceVersion() { return 2; }    
    public int getOperationNumber() { return 18; }
    public Response createDefaultResponse() { return new setxattrResponse(); }

}

