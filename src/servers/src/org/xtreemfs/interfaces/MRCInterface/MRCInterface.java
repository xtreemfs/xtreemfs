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


    public static long getProg() { return 2546893327l; }
    public static int getVersion() { return 2010022415; }

    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
            case 2010022465: return new ConcurrentModificationException();
            case 2010022466: return new errnoException();
            case 2010022467: return new InvalidArgumentException();
            case 2010022468: return new MRCException();
            case 2010022469: return new ProtocolException();
            case 2010022470: return new RedirectException();
            case 2010022471: return new StaleETagException();

            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2010022416: return new closeRequest();
            case 2010022417: return new fsetattrRequest();
            case 2010022418: return new ftruncateRequest();
            case 2010022419: return new getattrRequest();
            case 2010022420: return new getxattrRequest();
            case 2010022421: return new linkRequest();
            case 2010022422: return new listxattrRequest();
            case 2010022423: return new mkdirRequest();
            case 2010022424: return new openRequest();
            case 2010022425: return new opendirRequest();
            case 2010022426: return new readdirRequest();
            case 2010022427: return new readlinkRequest();
            case 2010022428: return new removexattrRequest();
            case 2010022429: return new renameRequest();
            case 2010022430: return new rmdirRequest();
            case 2010022431: return new setattrRequest();
            case 2010022432: return new setxattrRequest();
            case 2010022433: return new statvfsRequest();
            case 2010022434: return new symlinkRequest();
            case 2010022435: return new unlinkRequest();
            case 2010022445: return new xtreemfs_checkpointRequest();
            case 2010022446: return new xtreemfs_check_file_existsRequest();
            case 2010022447: return new xtreemfs_dump_databaseRequest();
            case 2010022448: return new xtreemfs_get_suitable_osdsRequest();
            case 2010022449: return new xtreemfs_internal_debugRequest();
            case 2010022450: return new xtreemfs_lsvolRequest();
            case 2010022451: return new xtreemfs_mkvolRequest();
            case 2010022452: return new xtreemfs_renew_capabilityRequest();
            case 2010022453: return new xtreemfs_replication_to_masterRequest();
            case 2010022454: return new xtreemfs_replica_addRequest();
            case 2010022455: return new xtreemfs_replica_listRequest();
            case 2010022456: return new xtreemfs_replica_removeRequest();
            case 2010022457: return new xtreemfs_restore_databaseRequest();
            case 2010022458: return new xtreemfs_restore_fileRequest();
            case 2010022459: return new xtreemfs_rmvolRequest();
            case 2010022460: return new xtreemfs_shutdownRequest();

            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2010022416: return new closeResponse();            case 2010022417: return new fsetattrResponse();            case 2010022418: return new ftruncateResponse();            case 2010022419: return new getattrResponse();            case 2010022420: return new getxattrResponse();            case 2010022421: return new linkResponse();            case 2010022422: return new listxattrResponse();            case 2010022423: return new mkdirResponse();            case 2010022424: return new openResponse();            case 2010022425: return new opendirResponse();            case 2010022426: return new readdirResponse();            case 2010022427: return new readlinkResponse();            case 2010022428: return new removexattrResponse();            case 2010022429: return new renameResponse();            case 2010022430: return new rmdirResponse();            case 2010022431: return new setattrResponse();            case 2010022432: return new setxattrResponse();            case 2010022433: return new statvfsResponse();            case 2010022434: return new symlinkResponse();            case 2010022435: return new unlinkResponse();            case 2010022445: return new xtreemfs_checkpointResponse();            case 2010022446: return new xtreemfs_check_file_existsResponse();            case 2010022447: return new xtreemfs_dump_databaseResponse();            case 2010022448: return new xtreemfs_get_suitable_osdsResponse();            case 2010022449: return new xtreemfs_internal_debugResponse();            case 2010022450: return new xtreemfs_lsvolResponse();            case 2010022451: return new xtreemfs_mkvolResponse();            case 2010022452: return new xtreemfs_renew_capabilityResponse();            case 2010022453: return new xtreemfs_replication_to_masterResponse();            case 2010022454: return new xtreemfs_replica_addResponse();            case 2010022455: return new xtreemfs_replica_listResponse();            case 2010022456: return new xtreemfs_replica_removeResponse();            case 2010022457: return new xtreemfs_restore_databaseResponse();            case 2010022458: return new xtreemfs_restore_fileResponse();            case 2010022459: return new xtreemfs_rmvolResponse();            case 2010022460: return new xtreemfs_shutdownResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }    

}
