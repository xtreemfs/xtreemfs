package org.xtreemfs.interfaces.MRCInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.PrettyPrinter;




public class MRCInterface
{
    public static final int DEFAULT_ONCRPC_PORT = 32636;
    public static final int DEFAULT_ONCRPCS_PORT = 32636;
    public static final int DEFAULT_ONCRPCG_PORT = 32636;
    public static final int DEFAULT_HTTP_PORT = 30636;


    public static int getVersion() { return 2009090409; }

    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
            case 2009090459: return new ConcurrentModificationException();
            case 2009090460: return new errnoException();
            case 2009090461: return new InvalidArgumentException();
            case 2009090462: return new MRCException();
            case 2009090463: return new ProtocolException();
            case 2009090464: return new RedirectException();

            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2009090410: return new accessRequest();
            case 2009090411: return new chmodRequest();
            case 2009090412: return new chownRequest();
            case 2009090431: return new closeRequest();
            case 2009090413: return new creatRequest();
            case 2009090414: return new ftruncateRequest();
            case 2009090415: return new getattrRequest();
            case 2009090416: return new getxattrRequest();
            case 2009090417: return new linkRequest();
            case 2009090418: return new listxattrRequest();
            case 2009090419: return new mkdirRequest();
            case 2009090420: return new openRequest();
            case 2009090421: return new readdirRequest();
            case 2009090422: return new removexattrRequest();
            case 2009090423: return new renameRequest();
            case 2009090424: return new rmdirRequest();
            case 2009090425: return new setattrRequest();
            case 2009090426: return new setxattrRequest();
            case 2009090427: return new statvfsRequest();
            case 2009090428: return new symlinkRequest();
            case 2009090429: return new unlinkRequest();
            case 2009090430: return new utimensRequest();
            case 2009090439: return new xtreemfs_checkpointRequest();
            case 2009090440: return new xtreemfs_check_file_existsRequest();
            case 2009090441: return new xtreemfs_dump_databaseRequest();
            case 2009090442: return new xtreemfs_get_suitable_osdsRequest();
            case 2009090443: return new xtreemfs_internal_debugRequest();
            case 2009090444: return new xtreemfs_lsvolRequest();
            case 2009090445: return new xtreemfs_listdirRequest();
            case 2009090446: return new xtreemfs_mkvolRequest();
            case 2009090447: return new xtreemfs_renew_capabilityRequest();
            case 2009090448: return new xtreemfs_replica_addRequest();
            case 2009090449: return new xtreemfs_replica_listRequest();
            case 2009090450: return new xtreemfs_replica_removeRequest();
            case 2009090451: return new xtreemfs_restore_databaseRequest();
            case 2009090452: return new xtreemfs_restore_fileRequest();
            case 2009090453: return new xtreemfs_rmvolRequest();
            case 2009090454: return new xtreemfs_shutdownRequest();
            case 2009090455: return new xtreemfs_update_file_sizeRequest();

            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2009090410: return new accessResponse();            case 2009090411: return new chmodResponse();            case 2009090412: return new chownResponse();            case 2009090431: return new closeResponse();            case 2009090413: return new creatResponse();            case 2009090414: return new ftruncateResponse();            case 2009090415: return new getattrResponse();            case 2009090416: return new getxattrResponse();            case 2009090417: return new linkResponse();            case 2009090418: return new listxattrResponse();            case 2009090419: return new mkdirResponse();            case 2009090420: return new openResponse();            case 2009090421: return new readdirResponse();            case 2009090422: return new removexattrResponse();            case 2009090423: return new renameResponse();            case 2009090424: return new rmdirResponse();            case 2009090425: return new setattrResponse();            case 2009090426: return new setxattrResponse();            case 2009090427: return new statvfsResponse();            case 2009090428: return new symlinkResponse();            case 2009090429: return new unlinkResponse();            case 2009090430: return new utimensResponse();            case 2009090439: return new xtreemfs_checkpointResponse();            case 2009090440: return new xtreemfs_check_file_existsResponse();            case 2009090441: return new xtreemfs_dump_databaseResponse();            case 2009090442: return new xtreemfs_get_suitable_osdsResponse();            case 2009090443: return new xtreemfs_internal_debugResponse();            case 2009090444: return new xtreemfs_lsvolResponse();            case 2009090445: return new xtreemfs_listdirResponse();            case 2009090446: return new xtreemfs_mkvolResponse();            case 2009090447: return new xtreemfs_renew_capabilityResponse();            case 2009090448: return new xtreemfs_replica_addResponse();            case 2009090449: return new xtreemfs_replica_listResponse();            case 2009090450: return new xtreemfs_replica_removeResponse();            case 2009090451: return new xtreemfs_restore_databaseResponse();            case 2009090452: return new xtreemfs_restore_fileResponse();            case 2009090453: return new xtreemfs_rmvolResponse();            case 2009090454: return new xtreemfs_shutdownResponse();            case 2009090455: return new xtreemfs_update_file_sizeResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }    

}
