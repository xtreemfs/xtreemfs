package org.xtreemfs.interfaces.OSDInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.PrettyPrinter;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_cleanup_startRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082950;
    
    public xtreemfs_cleanup_startRequest() {  }
    public xtreemfs_cleanup_startRequest( boolean remove_zombies, boolean remove_unavail_volume, boolean lost_and_found ) { this.remove_zombies = remove_zombies; this.remove_unavail_volume = remove_unavail_volume; this.lost_and_found = lost_and_found; }

    public boolean getRemove_zombies() { return remove_zombies; }
    public void setRemove_zombies( boolean remove_zombies ) { this.remove_zombies = remove_zombies; }
    public boolean getRemove_unavail_volume() { return remove_unavail_volume; }
    public void setRemove_unavail_volume( boolean remove_unavail_volume ) { this.remove_unavail_volume = remove_unavail_volume; }
    public boolean getLost_and_found() { return lost_and_found; }
    public void setLost_and_found( boolean lost_and_found ) { this.lost_and_found = lost_and_found; }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_cleanup_startResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082950;    

    // yidl.Object
    public int getTag() { return 2009082950; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_cleanup_startRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8; // remove_zombies
        my_size += Integer.SIZE / 8; // remove_unavail_volume
        my_size += Integer.SIZE / 8; // lost_and_found
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeBoolean( "remove_zombies", remove_zombies );
        marshaller.writeBoolean( "remove_unavail_volume", remove_unavail_volume );
        marshaller.writeBoolean( "lost_and_found", lost_and_found );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        remove_zombies = unmarshaller.readBoolean( "remove_zombies" );
        remove_unavail_volume = unmarshaller.readBoolean( "remove_unavail_volume" );
        lost_and_found = unmarshaller.readBoolean( "lost_and_found" );    
    }
        
    

    private boolean remove_zombies;
    private boolean remove_unavail_volume;
    private boolean lost_and_found;    

}

