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


    public static int getVersion() { return 2009120414; }

    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
            case 2009120464: return new ConcurrentModificationException();
            case 2009120465: return new errnoException();
            case 2009120466: return new InvalidArgumentException();
            case 2009120467: return new MRCException();
            case 2009120468: return new ProtocolException();
            case 2009120469: return new RedirectException();

            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2009120415: return new accessRequest();
            case 2009120416: return new chmodRequest();
            case 2009120417: return new chownRequest();
            case 2009120436: return new closeRequest();
            case 2009120418: return new creatRequest();
            case 2009120419: return new ftruncateRequest();
            case 2009120420: return new getattrRequest();
            case 2009120421: return new getxattrRequest();
            case 2009120422: return new linkRequest();
            case 2009120423: return new listxattrRequest();
            case 2009120424: return new mkdirRequest();
            case 2009120425: return new openRequest();
            case 2009120426: return new readdirRequest();
            case 2009120427: return new removexattrRequest();
            case 2009120428: return new renameRequest();
            case 2009120429: return new rmdirRequest();
            case 2009120430: return new setattrRequest();
            case 2009120431: return new setxattrRequest();
            case 2009120432: return new statvfsRequest();
            case 2009120433: return new symlinkRequest();
            case 2009120434: return new unlinkRequest();
            case 2009120435: return new utimensRequest();
            case 2009120444: return new xtreemfs_checkpointRequest();
            case 2009120445: return new xtreemfs_check_file_existsRequest();
            case 2009120446: return new xtreemfs_dump_databaseRequest();
            case 2009120447: return new xtreemfs_get_suitable_osdsRequest();
            case 2009120448: return new xtreemfs_internal_debugRequest();
            case 2009120450: return new xtreemfs_listdirRequest();
            case 2009120449: return new xtreemfs_lsvolRequest();
            case 2009120451: return new xtreemfs_mkvolRequest();
            case 2009120452: return new xtreemfs_renew_capabilityRequest();
            case 2009120453: return new xtreemfs_replication_to_masterRequest();
            case 2009120454: return new xtreemfs_replica_addRequest();
            case 2009120455: return new xtreemfs_replica_listRequest();
            case 2009120456: return new xtreemfs_replica_removeRequest();
            case 2009120457: return new xtreemfs_restore_databaseRequest();
            case 2009120458: return new xtreemfs_restore_fileRequest();
            case 2009120459: return new xtreemfs_rmvolRequest();
            case 2009120460: return new xtreemfs_shutdownRequest();
            case 2009120461: return new xtreemfs_update_file_sizeRequest();

            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2009120415: return new accessResponse();            case 2009120416: return new chmodResponse();            case 2009120417: return new chownResponse();            case 2009120436: return new closeResponse();            case 2009120418: return new creatResponse();            case 2009120419: return new ftruncateResponse();            case 2009120420: return new getattrResponse();            case 2009120421: return new getxattrResponse();            case 2009120422: return new linkResponse();            case 2009120423: return new listxattrResponse();            case 2009120424: return new mkdirResponse();            case 2009120425: return new openResponse();            case 2009120426: return new readdirResponse();            case 2009120427: return new removexattrResponse();            case 2009120428: return new renameResponse();            case 2009120429: return new rmdirResponse();            case 2009120430: return new setattrResponse();            case 2009120431: return new setxattrResponse();            case 2009120432: return new statvfsResponse();            case 2009120433: return new symlinkResponse();            case 2009120434: return new unlinkResponse();            case 2009120435: return new utimensResponse();            case 2009120444: return new xtreemfs_checkpointResponse();            case 2009120445: return new xtreemfs_check_file_existsResponse();            case 2009120446: return new xtreemfs_dump_databaseResponse();            case 2009120447: return new xtreemfs_get_suitable_osdsResponse();            case 2009120448: return new xtreemfs_internal_debugResponse();            case 2009120450: return new xtreemfs_listdirResponse();            case 2009120449: return new xtreemfs_lsvolResponse();            case 2009120451: return new xtreemfs_mkvolResponse();            case 2009120452: return new xtreemfs_renew_capabilityResponse();            case 2009120453: return new xtreemfs_replication_to_masterResponse();            case 2009120454: return new xtreemfs_replica_addResponse();            case 2009120455: return new xtreemfs_replica_listResponse();            case 2009120456: return new xtreemfs_replica_removeResponse();            case 2009120457: return new xtreemfs_restore_databaseResponse();            case 2009120458: return new xtreemfs_restore_fileResponse();            case 2009120459: return new xtreemfs_rmvolResponse();            case 2009120460: return new xtreemfs_shutdownResponse();            case 2009120461: return new xtreemfs_update_file_sizeResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }    

}
