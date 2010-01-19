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


    public static int getVersion() { return 2010012111; }

    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
            case 2010012161: return new ConcurrentModificationException();
            case 2010012162: return new errnoException();
            case 2010012163: return new InvalidArgumentException();
            case 2010012164: return new MRCException();
            case 2010012165: return new ProtocolException();
            case 2010012166: return new RedirectException();

            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2010012112: return new accessRequest();
            case 2010012113: return new chmodRequest();
            case 2010012114: return new chownRequest();
            case 2010012115: return new closeRequest();
            case 2010012116: return new creatRequest();
            case 2010012117: return new ftruncateRequest();
            case 2010012118: return new getattrRequest();
            case 2010012119: return new getxattrRequest();
            case 2010012120: return new linkRequest();
            case 2010012121: return new listxattrRequest();
            case 2010012122: return new mkdirRequest();
            case 2010012123: return new openRequest();
            case 2010012124: return new readdirRequest();
            case 2010012125: return new readlinkRequest();
            case 2010012126: return new removexattrRequest();
            case 2010012127: return new renameRequest();
            case 2010012128: return new rmdirRequest();
            case 2010012129: return new setattrRequest();
            case 2010012130: return new setxattrRequest();
            case 2010012131: return new statvfsRequest();
            case 2010012132: return new symlinkRequest();
            case 2010012133: return new unlinkRequest();
            case 2010012134: return new utimensRequest();
            case 2010012141: return new xtreemfs_checkpointRequest();
            case 2010012142: return new xtreemfs_check_file_existsRequest();
            case 2010012143: return new xtreemfs_dump_databaseRequest();
            case 2010012144: return new xtreemfs_get_suitable_osdsRequest();
            case 2010012145: return new xtreemfs_internal_debugRequest();
            case 2010012147: return new xtreemfs_listdirRequest();
            case 2010012146: return new xtreemfs_lsvolRequest();
            case 2010012148: return new xtreemfs_mkvolRequest();
            case 2010012149: return new xtreemfs_renew_capabilityRequest();
            case 2010012150: return new xtreemfs_replication_to_masterRequest();
            case 2010012151: return new xtreemfs_replica_addRequest();
            case 2010012152: return new xtreemfs_replica_listRequest();
            case 2010012153: return new xtreemfs_replica_removeRequest();
            case 2010012154: return new xtreemfs_restore_databaseRequest();
            case 2010012155: return new xtreemfs_restore_fileRequest();
            case 2010012156: return new xtreemfs_rmvolRequest();
            case 2010012157: return new xtreemfs_shutdownRequest();
            case 2010012158: return new xtreemfs_update_file_sizeRequest();

            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2010012112: return new accessResponse();            case 2010012113: return new chmodResponse();            case 2010012114: return new chownResponse();            case 2010012115: return new closeResponse();            case 2010012116: return new creatResponse();            case 2010012117: return new ftruncateResponse();            case 2010012118: return new getattrResponse();            case 2010012119: return new getxattrResponse();            case 2010012120: return new linkResponse();            case 2010012121: return new listxattrResponse();            case 2010012122: return new mkdirResponse();            case 2010012123: return new openResponse();            case 2010012124: return new readdirResponse();            case 2010012125: return new readlinkResponse();            case 2010012126: return new removexattrResponse();            case 2010012127: return new renameResponse();            case 2010012128: return new rmdirResponse();            case 2010012129: return new setattrResponse();            case 2010012130: return new setxattrResponse();            case 2010012131: return new statvfsResponse();            case 2010012132: return new symlinkResponse();            case 2010012133: return new unlinkResponse();            case 2010012134: return new utimensResponse();            case 2010012141: return new xtreemfs_checkpointResponse();            case 2010012142: return new xtreemfs_check_file_existsResponse();            case 2010012143: return new xtreemfs_dump_databaseResponse();            case 2010012144: return new xtreemfs_get_suitable_osdsResponse();            case 2010012145: return new xtreemfs_internal_debugResponse();            case 2010012147: return new xtreemfs_listdirResponse();            case 2010012146: return new xtreemfs_lsvolResponse();            case 2010012148: return new xtreemfs_mkvolResponse();            case 2010012149: return new xtreemfs_renew_capabilityResponse();            case 2010012150: return new xtreemfs_replication_to_masterResponse();            case 2010012151: return new xtreemfs_replica_addResponse();            case 2010012152: return new xtreemfs_replica_listResponse();            case 2010012153: return new xtreemfs_replica_removeResponse();            case 2010012154: return new xtreemfs_restore_databaseResponse();            case 2010012155: return new xtreemfs_restore_fileResponse();            case 2010012156: return new xtreemfs_rmvolResponse();            case 2010012157: return new xtreemfs_shutdownResponse();            case 2010012158: return new xtreemfs_update_file_sizeResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }    

}
