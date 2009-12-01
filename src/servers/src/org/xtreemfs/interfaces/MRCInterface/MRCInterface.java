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


    public static int getVersion() { return 2009120311; }

    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
            case 2009120361: return new ConcurrentModificationException();
            case 2009120362: return new errnoException();
            case 2009120363: return new InvalidArgumentException();
            case 2009120364: return new MRCException();
            case 2009120365: return new ProtocolException();
            case 2009120366: return new RedirectException();

            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2009120312: return new accessRequest();
            case 2009120313: return new chmodRequest();
            case 2009120314: return new chownRequest();
            case 2009120333: return new closeRequest();
            case 2009120315: return new creatRequest();
            case 2009120316: return new ftruncateRequest();
            case 2009120317: return new getattrRequest();
            case 2009120318: return new getxattrRequest();
            case 2009120319: return new linkRequest();
            case 2009120320: return new listxattrRequest();
            case 2009120321: return new mkdirRequest();
            case 2009120322: return new openRequest();
            case 2009120323: return new readdirRequest();
            case 2009120324: return new removexattrRequest();
            case 2009120325: return new renameRequest();
            case 2009120326: return new rmdirRequest();
            case 2009120327: return new setattrRequest();
            case 2009120328: return new setxattrRequest();
            case 2009120329: return new statvfsRequest();
            case 2009120330: return new symlinkRequest();
            case 2009120331: return new unlinkRequest();
            case 2009120332: return new utimensRequest();
            case 2009120341: return new xtreemfs_checkpointRequest();
            case 2009120342: return new xtreemfs_check_file_existsRequest();
            case 2009120343: return new xtreemfs_dump_databaseRequest();
            case 2009120344: return new xtreemfs_get_suitable_osdsRequest();
            case 2009120345: return new xtreemfs_internal_debugRequest();
            case 2009120347: return new xtreemfs_listdirRequest();
            case 2009120346: return new xtreemfs_lsvolRequest();
            case 2009120348: return new xtreemfs_mkvolRequest();
            case 2009120349: return new xtreemfs_renew_capabilityRequest();
            case 2009120350: return new xtreemfs_replication_to_masterRequest();
            case 2009120351: return new xtreemfs_replica_addRequest();
            case 2009120352: return new xtreemfs_replica_listRequest();
            case 2009120353: return new xtreemfs_replica_removeRequest();
            case 2009120354: return new xtreemfs_restore_databaseRequest();
            case 2009120355: return new xtreemfs_restore_fileRequest();
            case 2009120356: return new xtreemfs_rmvolRequest();
            case 2009120357: return new xtreemfs_shutdownRequest();
            case 2009120358: return new xtreemfs_update_file_sizeRequest();

            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2009120312: return new accessResponse();            case 2009120313: return new chmodResponse();            case 2009120314: return new chownResponse();            case 2009120333: return new closeResponse();            case 2009120315: return new creatResponse();            case 2009120316: return new ftruncateResponse();            case 2009120317: return new getattrResponse();            case 2009120318: return new getxattrResponse();            case 2009120319: return new linkResponse();            case 2009120320: return new listxattrResponse();            case 2009120321: return new mkdirResponse();            case 2009120322: return new openResponse();            case 2009120323: return new readdirResponse();            case 2009120324: return new removexattrResponse();            case 2009120325: return new renameResponse();            case 2009120326: return new rmdirResponse();            case 2009120327: return new setattrResponse();            case 2009120328: return new setxattrResponse();            case 2009120329: return new statvfsResponse();            case 2009120330: return new symlinkResponse();            case 2009120331: return new unlinkResponse();            case 2009120332: return new utimensResponse();            case 2009120341: return new xtreemfs_checkpointResponse();            case 2009120342: return new xtreemfs_check_file_existsResponse();            case 2009120343: return new xtreemfs_dump_databaseResponse();            case 2009120344: return new xtreemfs_get_suitable_osdsResponse();            case 2009120345: return new xtreemfs_internal_debugResponse();            case 2009120347: return new xtreemfs_listdirResponse();            case 2009120346: return new xtreemfs_lsvolResponse();            case 2009120348: return new xtreemfs_mkvolResponse();            case 2009120349: return new xtreemfs_renew_capabilityResponse();            case 2009120350: return new xtreemfs_replication_to_masterResponse();            case 2009120351: return new xtreemfs_replica_addResponse();            case 2009120352: return new xtreemfs_replica_listResponse();            case 2009120353: return new xtreemfs_replica_removeResponse();            case 2009120354: return new xtreemfs_restore_databaseResponse();            case 2009120355: return new xtreemfs_restore_fileResponse();            case 2009120356: return new xtreemfs_rmvolResponse();            case 2009120357: return new xtreemfs_shutdownResponse();            case 2009120358: return new xtreemfs_update_file_sizeResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }    

}
