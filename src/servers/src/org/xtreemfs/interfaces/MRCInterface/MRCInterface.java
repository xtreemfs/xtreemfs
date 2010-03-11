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

    public static long getProg() { return 2546902028l; }
    public static int getVersion() { return 2010031116; }

    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
            case 2010031166: return new ConcurrentModificationException();
            case 2010031167: return new errnoException();
            case 2010031168: return new InvalidArgumentException();
            case 2010031169: return new MRCException();
            case 2010031170: return new ProtocolException();
            case 2010031171: return new RedirectException();
            case 2010031172: return new StaleETagException();
            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2010031117: return new closeRequestRequest();
            case 2010031118: return new fsetattrRequestRequest();
            case 2010031119: return new ftruncateRequestRequest();
            case 2010031120: return new getattrRequestRequest();
            case 2010031121: return new getxattrRequestRequest();
            case 2010031122: return new linkRequestRequest();
            case 2010031123: return new listxattrRequestRequest();
            case 2010031124: return new mkdirRequestRequest();
            case 2010031125: return new openRequestRequest();
            case 2010031126: return new readdirRequestRequest();
            case 2010031127: return new readlinkRequestRequest();
            case 2010031128: return new removexattrRequestRequest();
            case 2010031129: return new renameRequestRequest();
            case 2010031130: return new rmdirRequestRequest();
            case 2010031131: return new setattrRequestRequest();
            case 2010031132: return new setxattrRequestRequest();
            case 2010031133: return new statvfsRequestRequest();
            case 2010031134: return new symlinkRequestRequest();
            case 2010031135: return new unlinkRequestRequest();
            case 2010031146: return new xtreemfs_checkpointRequestRequest();
            case 2010031147: return new xtreemfs_check_file_existsRequestRequest();
            case 2010031148: return new xtreemfs_dump_databaseRequestRequest();
            case 2010031149: return new xtreemfs_get_suitable_osdsRequestRequest();
            case 2010031150: return new xtreemfs_internal_debugRequestRequest();
            case 2010031151: return new xtreemfs_lsvolRequestRequest();
            case 2010031152: return new xtreemfs_mkvolRequestRequest();
            case 2010031153: return new xtreemfs_renew_capabilityRequestRequest();
            case 2010031154: return new xtreemfs_replication_to_masterRequestRequest();
            case 2010031155: return new xtreemfs_replica_addRequestRequest();
            case 2010031156: return new xtreemfs_replica_listRequestRequest();
            case 2010031157: return new xtreemfs_replica_removeRequestRequest();
            case 2010031158: return new xtreemfs_restore_databaseRequestRequest();
            case 2010031159: return new xtreemfs_restore_fileRequestRequest();
            case 2010031160: return new xtreemfs_rmvolRequestRequest();
            case 2010031161: return new xtreemfs_shutdownRequestRequest();
            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }

    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2010031117: return new closeResponseResponse();case 2010031118: return new fsetattrResponseResponse();case 2010031119: return new ftruncateResponseResponse();case 2010031120: return new getattrResponseResponse();case 2010031121: return new getxattrResponseResponse();case 2010031122: return new linkResponseResponse();case 2010031123: return new listxattrResponseResponse();case 2010031124: return new mkdirResponseResponse();case 2010031125: return new openResponseResponse();case 2010031126: return new readdirResponseResponse();case 2010031127: return new readlinkResponseResponse();case 2010031128: return new removexattrResponseResponse();case 2010031129: return new renameResponseResponse();case 2010031130: return new rmdirResponseResponse();case 2010031131: return new setattrResponseResponse();case 2010031132: return new setxattrResponseResponse();case 2010031133: return new statvfsResponseResponse();case 2010031134: return new symlinkResponseResponse();case 2010031135: return new unlinkResponseResponse();case 2010031146: return new xtreemfs_checkpointResponseResponse();case 2010031147: return new xtreemfs_check_file_existsResponseResponse();case 2010031148: return new xtreemfs_dump_databaseResponseResponse();case 2010031149: return new xtreemfs_get_suitable_osdsResponseResponse();case 2010031150: return new xtreemfs_internal_debugResponseResponse();case 2010031151: return new xtreemfs_lsvolResponseResponse();case 2010031152: return new xtreemfs_mkvolResponseResponse();case 2010031153: return new xtreemfs_renew_capabilityResponseResponse();case 2010031154: return new xtreemfs_replication_to_masterResponseResponse();case 2010031155: return new xtreemfs_replica_addResponseResponse();case 2010031156: return new xtreemfs_replica_listResponseResponse();case 2010031157: return new xtreemfs_replica_removeResponseResponse();case 2010031158: return new xtreemfs_restore_databaseResponseResponse();case 2010031159: return new xtreemfs_restore_fileResponseResponse();case 2010031160: return new xtreemfs_rmvolResponseResponse();case 2010031161: return new xtreemfs_shutdownResponseResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }

}
