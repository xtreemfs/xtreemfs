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


    public static long getProg() { return 2546901426l; }
    public static int getVersion() { return 2010030514; }

    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
            case 2010030564: return new ConcurrentModificationException();
            case 2010030565: return new errnoException();
            case 2010030566: return new InvalidArgumentException();
            case 2010030567: return new MRCException();
            case 2010030568: return new ProtocolException();
            case 2010030569: return new RedirectException();
            case 2010030570: return new StaleETagException();

            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2010030515: return new closeRequest();
            case 2010030516: return new fsetattrRequest();
            case 2010030517: return new ftruncateRequest();
            case 2010030518: return new getattrRequest();
            case 2010030519: return new getxattrRequest();
            case 2010030520: return new linkRequest();
            case 2010030521: return new listxattrRequest();
            case 2010030522: return new mkdirRequest();
            case 2010030523: return new openRequest();
            case 2010030524: return new readdirRequest();
            case 2010030525: return new readlinkRequest();
            case 2010030526: return new removexattrRequest();
            case 2010030527: return new renameRequest();
            case 2010030528: return new rmdirRequest();
            case 2010030529: return new setattrRequest();
            case 2010030530: return new setxattrRequest();
            case 2010030531: return new statvfsRequest();
            case 2010030532: return new symlinkRequest();
            case 2010030533: return new unlinkRequest();
            case 2010030544: return new xtreemfs_checkpointRequest();
            case 2010030545: return new xtreemfs_check_file_existsRequest();
            case 2010030546: return new xtreemfs_dump_databaseRequest();
            case 2010030547: return new xtreemfs_get_suitable_osdsRequest();
            case 2010030548: return new xtreemfs_internal_debugRequest();
            case 2010030549: return new xtreemfs_lsvolRequest();
            case 2010030550: return new xtreemfs_mkvolRequest();
            case 2010030551: return new xtreemfs_renew_capabilityRequest();
            case 2010030552: return new xtreemfs_replication_to_masterRequest();
            case 2010030553: return new xtreemfs_replica_addRequest();
            case 2010030554: return new xtreemfs_replica_listRequest();
            case 2010030555: return new xtreemfs_replica_removeRequest();
            case 2010030556: return new xtreemfs_restore_databaseRequest();
            case 2010030557: return new xtreemfs_restore_fileRequest();
            case 2010030558: return new xtreemfs_rmvolRequest();
            case 2010030559: return new xtreemfs_shutdownRequest();

            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2010030515: return new closeResponse();            case 2010030516: return new fsetattrResponse();            case 2010030517: return new ftruncateResponse();            case 2010030518: return new getattrResponse();            case 2010030519: return new getxattrResponse();            case 2010030520: return new linkResponse();            case 2010030521: return new listxattrResponse();            case 2010030522: return new mkdirResponse();            case 2010030523: return new openResponse();            case 2010030524: return new readdirResponse();            case 2010030525: return new readlinkResponse();            case 2010030526: return new removexattrResponse();            case 2010030527: return new renameResponse();            case 2010030528: return new rmdirResponse();            case 2010030529: return new setattrResponse();            case 2010030530: return new setxattrResponse();            case 2010030531: return new statvfsResponse();            case 2010030532: return new symlinkResponse();            case 2010030533: return new unlinkResponse();            case 2010030544: return new xtreemfs_checkpointResponse();            case 2010030545: return new xtreemfs_check_file_existsResponse();            case 2010030546: return new xtreemfs_dump_databaseResponse();            case 2010030547: return new xtreemfs_get_suitable_osdsResponse();            case 2010030548: return new xtreemfs_internal_debugResponse();            case 2010030549: return new xtreemfs_lsvolResponse();            case 2010030550: return new xtreemfs_mkvolResponse();            case 2010030551: return new xtreemfs_renew_capabilityResponse();            case 2010030552: return new xtreemfs_replication_to_masterResponse();            case 2010030553: return new xtreemfs_replica_addResponse();            case 2010030554: return new xtreemfs_replica_listResponse();            case 2010030555: return new xtreemfs_replica_removeResponse();            case 2010030556: return new xtreemfs_restore_databaseResponse();            case 2010030557: return new xtreemfs_restore_fileResponse();            case 2010030558: return new xtreemfs_rmvolResponse();            case 2010030559: return new xtreemfs_shutdownResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }    

}
