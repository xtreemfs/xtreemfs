package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class Stat implements Struct
{
    public static final int TAG = 2010030956;

    public Stat() {  }
    public Stat( long dev, long ino, int mode, int nlink, String user_id, String group_id, long size, long atime_ns, long mtime_ns, long ctime_ns, int blksize, long etag, int truncate_epoch, int attributes ) { this.dev = dev; this.ino = ino; this.mode = mode; this.nlink = nlink; this.user_id = user_id; this.group_id = group_id; this.size = size; this.atime_ns = atime_ns; this.mtime_ns = mtime_ns; this.ctime_ns = ctime_ns; this.blksize = blksize; this.etag = etag; this.truncate_epoch = truncate_epoch; this.attributes = attributes; }

    public long getDev() { return dev; }
    public void setDev( long dev ) { this.dev = dev; }
    public long getIno() { return ino; }
    public void setIno( long ino ) { this.ino = ino; }
    public int getMode() { return mode; }
    public void setMode( int mode ) { this.mode = mode; }
    public int getNlink() { return nlink; }
    public void setNlink( int nlink ) { this.nlink = nlink; }
    public String getUser_id() { return user_id; }
    public void setUser_id( String user_id ) { this.user_id = user_id; }
    public String getGroup_id() { return group_id; }
    public void setGroup_id( String group_id ) { this.group_id = group_id; }
    public long getSize() { return size; }
    public void setSize( long size ) { this.size = size; }
    public long getAtime_ns() { return atime_ns; }
    public void setAtime_ns( long atime_ns ) { this.atime_ns = atime_ns; }
    public long getMtime_ns() { return mtime_ns; }
    public void setMtime_ns( long mtime_ns ) { this.mtime_ns = mtime_ns; }
    public long getCtime_ns() { return ctime_ns; }
    public void setCtime_ns( long ctime_ns ) { this.ctime_ns = ctime_ns; }
    public int getBlksize() { return blksize; }
    public void setBlksize( int blksize ) { this.blksize = blksize; }
    public long getEtag() { return etag; }
    public void setEtag( long etag ) { this.etag = etag; }
    public int getTruncate_epoch() { return truncate_epoch; }
    public void setTruncate_epoch( int truncate_epoch ) { this.truncate_epoch = truncate_epoch; }
    public int getAttributes() { return attributes; }
    public void setAttributes( int attributes ) { this.attributes = attributes; }

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
    public static final long serialVersionUID = 2010030956;

    // yidl.runtime.Object
    public int getTag() { return 2010030956; }
    public String getTypeName() { return "org::xtreemfs::interfaces::Stat"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Long.SIZE / 8; // dev
        my_size += Long.SIZE / 8; // ino
        my_size += Integer.SIZE / 8; // mode
        my_size += Integer.SIZE / 8; // nlink
        my_size += Integer.SIZE / 8 + ( user_id != null ? ( ( user_id.getBytes().length % 4 == 0 ) ? user_id.getBytes().length : ( user_id.getBytes().length + 4 - user_id.getBytes().length % 4 ) ) : 0 ); // user_id
        my_size += Integer.SIZE / 8 + ( group_id != null ? ( ( group_id.getBytes().length % 4 == 0 ) ? group_id.getBytes().length : ( group_id.getBytes().length + 4 - group_id.getBytes().length % 4 ) ) : 0 ); // group_id
        my_size += Long.SIZE / 8; // size
        my_size += Long.SIZE / 8; // atime_ns
        my_size += Long.SIZE / 8; // mtime_ns
        my_size += Long.SIZE / 8; // ctime_ns
        my_size += Integer.SIZE / 8; // blksize
        my_size += Long.SIZE / 8; // etag
        my_size += Integer.SIZE / 8; // truncate_epoch
        my_size += Integer.SIZE / 8; // attributes
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeUint64( "dev", dev );
        marshaller.writeUint64( "ino", ino );
        marshaller.writeUint32( "mode", mode );
        marshaller.writeUint32( "nlink", nlink );
        marshaller.writeString( "user_id", user_id );
        marshaller.writeString( "group_id", group_id );
        marshaller.writeUint64( "size", size );
        marshaller.writeUint64( "atime_ns", atime_ns );
        marshaller.writeUint64( "mtime_ns", mtime_ns );
        marshaller.writeUint64( "ctime_ns", ctime_ns );
        marshaller.writeUint32( "blksize", blksize );
        marshaller.writeUint64( "etag", etag );
        marshaller.writeUint32( "truncate_epoch", truncate_epoch );
        marshaller.writeUint32( "attributes", attributes );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        dev = unmarshaller.readUint64( "dev" );
        ino = unmarshaller.readUint64( "ino" );
        mode = unmarshaller.readUint32( "mode" );
        nlink = unmarshaller.readUint32( "nlink" );
        user_id = unmarshaller.readString( "user_id" );
        group_id = unmarshaller.readString( "group_id" );
        size = unmarshaller.readUint64( "size" );
        atime_ns = unmarshaller.readUint64( "atime_ns" );
        mtime_ns = unmarshaller.readUint64( "mtime_ns" );
        ctime_ns = unmarshaller.readUint64( "ctime_ns" );
        blksize = unmarshaller.readUint32( "blksize" );
        etag = unmarshaller.readUint64( "etag" );
        truncate_epoch = unmarshaller.readUint32( "truncate_epoch" );
        attributes = unmarshaller.readUint32( "attributes" );
    }

    

    private long dev;
    private long ino;
    private int mode;
    private int nlink;
    private String user_id;
    private String group_id;
    private long size;
    private long atime_ns;
    private long mtime_ns;
    private long ctime_ns;
    private int blksize;
    private long etag;
    private int truncate_epoch;
    private int attributes;

}

