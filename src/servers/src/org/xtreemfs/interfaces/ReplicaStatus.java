package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class ReplicaStatus implements Struct
{
    public static final int TAG = 2010030975;

    public ReplicaStatus() { objectVersions = new ObjectVersionList();  }
    public ReplicaStatus( long truncate_epoch, long file_size, long max_obj_version, ObjectVersionList objectVersions ) { this.truncate_epoch = truncate_epoch; this.file_size = file_size; this.max_obj_version = max_obj_version; this.objectVersions = objectVersions; }

    public long getTruncate_epoch() { return truncate_epoch; }
    public void setTruncate_epoch( long truncate_epoch ) { this.truncate_epoch = truncate_epoch; }
    public long getFile_size() { return file_size; }
    public void setFile_size( long file_size ) { this.file_size = file_size; }
    public long getMax_obj_version() { return max_obj_version; }
    public void setMax_obj_version( long max_obj_version ) { this.max_obj_version = max_obj_version; }
    public ObjectVersionList getObjectVersions() { return objectVersions; }
    public void setObjectVersions( ObjectVersionList objectVersions ) { this.objectVersions = objectVersions; }

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
    public static final long serialVersionUID = 2010030975;

    // yidl.runtime.Object
    public int getTag() { return 2010030975; }
    public String getTypeName() { return "org::xtreemfs::interfaces::ReplicaStatus"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Long.SIZE / 8; // truncate_epoch
        my_size += Long.SIZE / 8; // file_size
        my_size += Long.SIZE / 8; // max_obj_version
        my_size += objectVersions.getXDRSize(); // objectVersions
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeUint64( "truncate_epoch", truncate_epoch );
        marshaller.writeUint64( "file_size", file_size );
        marshaller.writeUint64( "max_obj_version", max_obj_version );
        marshaller.writeSequence( "objectVersions", objectVersions );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        truncate_epoch = unmarshaller.readUint64( "truncate_epoch" );
        file_size = unmarshaller.readUint64( "file_size" );
        max_obj_version = unmarshaller.readUint64( "max_obj_version" );
        objectVersions = new ObjectVersionList(); unmarshaller.readSequence( "objectVersions", objectVersions );
    }

    

    private long truncate_epoch;
    private long file_size;
    private long max_obj_version;
    private ObjectVersionList objectVersions;

}

