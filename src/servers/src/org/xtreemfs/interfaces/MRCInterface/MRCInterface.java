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


    public static int getVersion() { return 2009121110; }

    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
            case 2009121160: return new ConcurrentModificationException();
            case 2009121161: return new errnoException();
            case 2009121162: return new InvalidArgumentException();
            case 2009121163: return new MRCException();
            case 2009121164: return new ProtocolException();
            case 2009121165: return new RedirectException();

            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2009121111: return new accessRequest();
            case 2009121112: return new chmodRequest();
            case 2009121113: return new chownRequest();
            case 2009121132: return new closeRequest();
            case 2009121114: return new creatRequest();
            case 2009121115: return new ftruncateRequest();
            case 2009121116: return new getattrRequest();
            case 2009121117: return new getxattrRequest();
            case 2009121118: return new linkRequest();
            case 2009121119: return new listxattrRequest();
            case 2009121120: return new mkdirRequest();
            case 2009121121: return new openRequest();
            case 2009121122: return new readdirRequest();
            case 2009121123: return new removexattrRequest();
            case 2009121124: return new renameRequest();
            case 2009121125: return new rmdirRequest();
            case 2009121126: return new setattrRequest();
            case 2009121127: return new setxattrRequest();
            case 2009121128: return new statvfsRequest();
            case 2009121129: return new symlinkRequest();
            case 2009121130: return new unlinkRequest();
            case 2009121131: return new utimensRequest();
            case 2009121140: return new xtreemfs_checkpointRequest();
            case 2009121141: return new xtreemfs_check_file_existsRequest();
            case 2009121142: return new xtreemfs_dump_databaseRequest();
            case 2009121143: return new xtreemfs_get_suitable_osdsRequest();
            case 2009121144: return new xtreemfs_internal_debugRequest();
            case 2009121146: return new xtreemfs_listdirRequest();
            case 2009121145: return new xtreemfs_lsvolRequest();
            case 2009121147: return new xtreemfs_mkvolRequest();
            case 2009121148: return new xtreemfs_renew_capabilityRequest();
            case 2009121149: return new xtreemfs_replication_to_masterRequest();
            case 2009121150: return new xtreemfs_replica_addRequest();
            case 2009121151: return new xtreemfs_replica_listRequest();
            case 2009121152: return new xtreemfs_replica_removeRequest();
            case 2009121153: return new xtreemfs_restore_databaseRequest();
            case 2009121154: return new xtreemfs_restore_fileRequest();
            case 2009121155: return new xtreemfs_rmvolRequest();
            case 2009121156: return new xtreemfs_shutdownRequest();
            case 2009121157: return new xtreemfs_update_file_sizeRequest();

            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2009121111: return new accessResponse();            case 2009121112: return new chmodResponse();            case 2009121113: return new chownResponse();            case 2009121132: return new closeResponse();            case 2009121114: return new creatResponse();            case 2009121115: return new ftruncateResponse();            case 2009121116: return new getattrResponse();            case 2009121117: return new getxattrResponse();            case 2009121118: return new linkResponse();            case 2009121119: return new listxattrResponse();            case 2009121120: return new mkdirResponse();            case 2009121121: return new openResponse();            case 2009121122: return new readdirResponse();            case 2009121123: return new removexattrResponse();            case 2009121124: return new renameResponse();            case 2009121125: return new rmdirResponse();            case 2009121126: return new setattrResponse();            case 2009121127: return new setxattrResponse();            case 2009121128: return new statvfsResponse();            case 2009121129: return new symlinkResponse();            case 2009121130: return new unlinkResponse();            case 2009121131: return new utimensResponse();            case 2009121140: return new xtreemfs_checkpointResponse();            case 2009121141: return new xtreemfs_check_file_existsResponse();            case 2009121142: return new xtreemfs_dump_databaseResponse();            case 2009121143: return new xtreemfs_get_suitable_osdsResponse();            case 2009121144: return new xtreemfs_internal_debugResponse();            case 2009121146: return new xtreemfs_listdirResponse();            case 2009121145: return new xtreemfs_lsvolResponse();            case 2009121147: return new xtreemfs_mkvolResponse();            case 2009121148: return new xtreemfs_renew_capabilityResponse();            case 2009121149: return new xtreemfs_replication_to_masterResponse();            case 2009121150: return new xtreemfs_replica_addResponse();            case 2009121151: return new xtreemfs_replica_listResponse();            case 2009121152: return new xtreemfs_replica_removeResponse();            case 2009121153: return new xtreemfs_restore_databaseResponse();            case 2009121154: return new xtreemfs_restore_fileResponse();            case 2009121155: return new xtreemfs_rmvolResponse();            case 2009121156: return new xtreemfs_shutdownResponse();            case 2009121157: return new xtreemfs_update_file_sizeResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }    

}
