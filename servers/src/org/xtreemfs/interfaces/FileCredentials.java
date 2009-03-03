package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class FileCredentials implements org.xtreemfs.interfaces.utils.Serializable
{
    public FileCredentials() { xlocs = new org.xtreemfs.interfaces.XLocSet(); xcap = new org.xtreemfs.interfaces.XCap(); }
    public FileCredentials( XLocSet xlocs, XCap xcap ) { this.xlocs = xlocs; this.xcap = xcap; }

    public XLocSet getXlocs() { return xlocs; }
    public void setXlocs( XLocSet xlocs ) { this.xlocs = xlocs; }
    public XCap getXcap() { return xcap; }
    public void setXcap( XCap xcap ) { this.xcap = xcap; }

    // Object
    public String toString()
    {
        return "FileCredentials( " + xlocs.toString() + ", " + xcap.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::FileCredentials"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        xlocs.serialize( writer );
        xcap.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        xlocs = new org.xtreemfs.interfaces.XLocSet(); xlocs.deserialize( buf );
        xcap = new org.xtreemfs.interfaces.XCap(); xcap.deserialize( buf );    
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

