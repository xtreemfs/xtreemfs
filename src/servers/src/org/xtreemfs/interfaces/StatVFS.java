package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class StatVFS implements Struct
{
    public static final int TAG = 2010030960;

    public StatVFS() { access_control_policy = AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL; default_striping_policy = new StripingPolicy();  }
    public StatVFS( int bsize, long bavail, long blocks, String fsid, int namemax, AccessControlPolicyType access_control_policy, StripingPolicy default_striping_policy, long etag, int mode, String name, String owner_group_id, String owner_user_id ) { this.bsize = bsize; this.bavail = bavail; this.blocks = blocks; this.fsid = fsid; this.namemax = namemax; this.access_control_policy = access_control_policy; this.default_striping_policy = default_striping_policy; this.etag = etag; this.mode = mode; this.name = name; this.owner_group_id = owner_group_id; this.owner_user_id = owner_user_id; }

    public int getBsize() { return bsize; }
    public long getBavail() { return bavail; }
    public long getBlocks() { return blocks; }
    public String getFsid() { return fsid; }
    public int getNamemax() { return namemax; }
    public AccessControlPolicyType getAccess_control_policy() { return access_control_policy; }
    public StripingPolicy getDefault_striping_policy() { return default_striping_policy; }
    public long getEtag() { return etag; }
    public int getMode() { return mode; }
    public String getName() { return name; }
    public String getOwner_group_id() { return owner_group_id; }
    public String getOwner_user_id() { return owner_user_id; }
    public void setBsize( int bsize ) { this.bsize = bsize; }
    public void setBavail( long bavail ) { this.bavail = bavail; }
    public void setBlocks( long blocks ) { this.blocks = blocks; }
    public void setFsid( String fsid ) { this.fsid = fsid; }
    public void setNamemax( int namemax ) { this.namemax = namemax; }
    public void setAccess_control_policy( AccessControlPolicyType access_control_policy ) { this.access_control_policy = access_control_policy; }
    public void setDefault_striping_policy( StripingPolicy default_striping_policy ) { this.default_striping_policy = default_striping_policy; }
    public void setEtag( long etag ) { this.etag = etag; }
    public void setMode( int mode ) { this.mode = mode; }
    public void setName( String name ) { this.name = name; }
    public void setOwner_group_id( String owner_group_id ) { this.owner_group_id = owner_group_id; }
    public void setOwner_user_id( String owner_user_id ) { this.owner_user_id = owner_user_id; }

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
    public static final long serialVersionUID = 2010030960;

    // yidl.runtime.Object
    public int getTag() { return 2010030960; }
    public String getTypeName() { return "org::xtreemfs::interfaces::StatVFS"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8; // bsize
        my_size += Long.SIZE / 8; // bavail
        my_size += Long.SIZE / 8; // blocks
        my_size += Integer.SIZE / 8 + ( fsid != null ? ( ( fsid.getBytes().length % 4 == 0 ) ? fsid.getBytes().length : ( fsid.getBytes().length + 4 - fsid.getBytes().length % 4 ) ) : 0 ); // fsid
        my_size += Integer.SIZE / 8; // namemax
        my_size += Integer.SIZE / 8; // access_control_policy
        my_size += default_striping_policy.getXDRSize(); // default_striping_policy
        my_size += Long.SIZE / 8; // etag
        my_size += Integer.SIZE / 8; // mode
        my_size += Integer.SIZE / 8 + ( name != null ? ( ( name.getBytes().length % 4 == 0 ) ? name.getBytes().length : ( name.getBytes().length + 4 - name.getBytes().length % 4 ) ) : 0 ); // name
        my_size += Integer.SIZE / 8 + ( owner_group_id != null ? ( ( owner_group_id.getBytes().length % 4 == 0 ) ? owner_group_id.getBytes().length : ( owner_group_id.getBytes().length + 4 - owner_group_id.getBytes().length % 4 ) ) : 0 ); // owner_group_id
        my_size += Integer.SIZE / 8 + ( owner_user_id != null ? ( ( owner_user_id.getBytes().length % 4 == 0 ) ? owner_user_id.getBytes().length : ( owner_user_id.getBytes().length + 4 - owner_user_id.getBytes().length % 4 ) ) : 0 ); // owner_user_id
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeUint32( "bsize", bsize );
        marshaller.writeUint64( "bavail", bavail );
        marshaller.writeUint64( "blocks", blocks );
        marshaller.writeString( "fsid", fsid );
        marshaller.writeUint32( "namemax", namemax );
        marshaller.writeInt32( access_control_policy, access_control_policy.intValue() );
        marshaller.writeStruct( "default_striping_policy", default_striping_policy );
        marshaller.writeUint64( "etag", etag );
        marshaller.writeUint32( "mode", mode );
        marshaller.writeString( "name", name );
        marshaller.writeString( "owner_group_id", owner_group_id );
        marshaller.writeString( "owner_user_id", owner_user_id );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        bsize = unmarshaller.readUint32( "bsize" );
        bavail = unmarshaller.readUint64( "bavail" );
        blocks = unmarshaller.readUint64( "blocks" );
        fsid = unmarshaller.readString( "fsid" );
        namemax = unmarshaller.readUint32( "namemax" );
        access_control_policy = AccessControlPolicyType.parseInt( unmarshaller.readInt32( "access_control_policy" ) );
        default_striping_policy = new StripingPolicy(); unmarshaller.readStruct( "default_striping_policy", default_striping_policy );
        etag = unmarshaller.readUint64( "etag" );
        mode = unmarshaller.readUint32( "mode" );
        name = unmarshaller.readString( "name" );
        owner_group_id = unmarshaller.readString( "owner_group_id" );
        owner_user_id = unmarshaller.readString( "owner_user_id" );
    }

    private int bsize;
    private long bavail;
    private long blocks;
    private String fsid;
    private int namemax;
    private AccessControlPolicyType access_control_policy;
    private StripingPolicy default_striping_policy;
    private long etag;
    private int mode;
    private String name;
    private String owner_group_id;
    private String owner_user_id;
}
