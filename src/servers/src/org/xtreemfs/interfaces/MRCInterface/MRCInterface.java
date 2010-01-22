package org.xtreemfs.interfaces.MRCInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.PrettyPrinter;




public class MRCInterface
{
    public static final int HTTP_PORT_DEFAULT = 30636;
    public static final int ONCRPC_PORT_DEFAULT = 32636;
    public static final int ONCRPCG_PORT_DEFAULT = 32636;
    public static final int ONCRPCS_PORT_DEFAULT = 32636;
    public static final int ONCRPCU_PORT_DEFAULT = 32636;
    public static final int SETATTR_MODE = 1;
    public static final int SETATTR_UID = 2;
    public static final int SETATTR_GID = 4;
    public static final int SETATTR_SIZE = 8;
    public static final int SETATTR_ATIME = 16;
    public static final int SETATTR_MTIME = 32;
    public static final int SETATTR_CTIME = 64;
    public static final int SETATTR_ATTRIBUTES = 128;


    public static int getVersion() { return 2010012313; }

    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
            case 2010012363: return new ConcurrentModificationException();
            case 2010012364: return new errnoException();
            case 2010012365: return new InvalidArgumentException();
            case 2010012366: return new MRCException();
            case 2010012367: return new ProtocolException();
            case 2010012368: return new RedirectException();

            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2010012314: return new closeRequest();
            case 2010012315: return new fsetattrRequest();
            case 2010012316: return new ftruncateRequest();
            case 2010012317: return new getattrRequest();
            case 2010012318: return new getxattrRequest();
            case 2010012319: return new linkRequest();
            case 2010012320: return new listxattrRequest();
            case 2010012321: return new mkdirRequest();
            case 2010012322: return new openRequest();
            case 2010012323: return new readdirRequest();
            case 2010012324: return new readlinkRequest();
            case 2010012325: return new removexattrRequest();
            case 2010012326: return new renameRequest();
            case 2010012327: return new rmdirRequest();
            case 2010012328: return new setattrRequest();
            case 2010012329: return new setxattrRequest();
            case 2010012330: return new statvfsRequest();
            case 2010012331: return new symlinkRequest();
            case 2010012332: return new unlinkRequest();
            case 2010012343: return new xtreemfs_checkpointRequest();
            case 2010012344: return new xtreemfs_check_file_existsRequest();
            case 2010012345: return new xtreemfs_dump_databaseRequest();
            case 2010012346: return new xtreemfs_get_suitable_osdsRequest();
            case 2010012347: return new xtreemfs_internal_debugRequest();
            case 2010012349: return new xtreemfs_listdirRequest();
            case 2010012348: return new xtreemfs_lsvolRequest();
            case 2010012350: return new xtreemfs_mkvolRequest();
            case 2010012351: return new xtreemfs_renew_capabilityRequest();
            case 2010012352: return new xtreemfs_replication_to_masterRequest();
            case 2010012353: return new xtreemfs_replica_addRequest();
            case 2010012354: return new xtreemfs_replica_listRequest();
            case 2010012355: return new xtreemfs_replica_removeRequest();
            case 2010012356: return new xtreemfs_restore_databaseRequest();
            case 2010012357: return new xtreemfs_restore_fileRequest();
            case 2010012358: return new xtreemfs_rmvolRequest();
            case 2010012359: return new xtreemfs_shutdownRequest();

            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2010012314: return new closeResponse();            case 2010012315: return new fsetattrResponse();            case 2010012316: return new ftruncateResponse();            case 2010012317: return new getattrResponse();            case 2010012318: return new getxattrResponse();            case 2010012319: return new linkResponse();            case 2010012320: return new listxattrResponse();            case 2010012321: return new mkdirResponse();            case 2010012322: return new openResponse();            case 2010012323: return new readdirResponse();            case 2010012324: return new readlinkResponse();            case 2010012325: return new removexattrResponse();            case 2010012326: return new renameResponse();            case 2010012327: return new rmdirResponse();            case 2010012328: return new setattrResponse();            case 2010012329: return new setxattrResponse();            case 2010012330: return new statvfsResponse();            case 2010012331: return new symlinkResponse();            case 2010012332: return new unlinkResponse();            case 2010012343: return new xtreemfs_checkpointResponse();            case 2010012344: return new xtreemfs_check_file_existsResponse();            case 2010012345: return new xtreemfs_dump_databaseResponse();            case 2010012346: return new xtreemfs_get_suitable_osdsResponse();            case 2010012347: return new xtreemfs_internal_debugResponse();            case 2010012349: return new xtreemfs_listdirResponse();            case 2010012348: return new xtreemfs_lsvolResponse();            case 2010012350: return new xtreemfs_mkvolResponse();            case 2010012351: return new xtreemfs_renew_capabilityResponse();            case 2010012352: return new xtreemfs_replication_to_masterResponse();            case 2010012353: return new xtreemfs_replica_addResponse();            case 2010012354: return new xtreemfs_replica_listResponse();            case 2010012355: return new xtreemfs_replica_removeResponse();            case 2010012356: return new xtreemfs_restore_databaseResponse();            case 2010012357: return new xtreemfs_restore_fileResponse();            case 2010012358: return new xtreemfs_rmvolResponse();            case 2010012359: return new xtreemfs_shutdownResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }    

}
