package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;




public class MRCInterface
{
    public static final int DEFAULT_ONCRPC_PORT = 32636;
    public static final int DEFAULT_ONCRPCS_PORT = 32636;
    public static final int DEFAULT_HTTP_PORT = 30636;


    public static int getVersion() { return 2009082818; }

    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
            case 2009082868: return new ConcurrentModificationException();
            case 2009082869: return new errnoException();
            case 2009082870: return new InvalidArgumentException();
            case 2009082871: return new MRCException();
            case 2009082872: return new ProtocolException();
            case 2009082873: return new RedirectException();

            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2009082819: return new accessRequest();
            case 2009082820: return new chmodRequest();
            case 2009082821: return new chownRequest();
            case 2009082840: return new closeRequest();
            case 2009082822: return new creatRequest();
            case 2009082823: return new ftruncateRequest();
            case 2009082824: return new getattrRequest();
            case 2009082825: return new getxattrRequest();
            case 2009082826: return new linkRequest();
            case 2009082827: return new listxattrRequest();
            case 2009082828: return new mkdirRequest();
            case 2009082829: return new openRequest();
            case 2009082830: return new readdirRequest();
            case 2009082831: return new removexattrRequest();
            case 2009082832: return new renameRequest();
            case 2009082833: return new rmdirRequest();
            case 2009082834: return new setattrRequest();
            case 2009082835: return new setxattrRequest();
            case 2009082836: return new statvfsRequest();
            case 2009082837: return new symlinkRequest();
            case 2009082838: return new unlinkRequest();
            case 2009082839: return new utimensRequest();
            case 2009082848: return new xtreemfs_checkpointRequest();
            case 2009082849: return new xtreemfs_check_file_existsRequest();
            case 2009082850: return new xtreemfs_dump_databaseRequest();
            case 2009082851: return new xtreemfs_get_suitable_osdsRequest();
            case 2009082852: return new xtreemfs_internal_debugRequest();
            case 2009082853: return new xtreemfs_lsvolRequest();
            case 2009082854: return new xtreemfs_listdirRequest();
            case 2009082855: return new xtreemfs_mkvolRequest();
            case 2009082856: return new xtreemfs_renew_capabilityRequest();
            case 2009082857: return new xtreemfs_replica_addRequest();
            case 2009082858: return new xtreemfs_replica_listRequest();
            case 2009082859: return new xtreemfs_replica_removeRequest();
            case 2009082860: return new xtreemfs_restore_databaseRequest();
            case 2009082861: return new xtreemfs_restore_fileRequest();
            case 2009082862: return new xtreemfs_rmvolRequest();
            case 2009082863: return new xtreemfs_shutdownRequest();
            case 2009082864: return new xtreemfs_update_file_sizeRequest();

            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2009082819: return new accessResponse();            case 2009082820: return new chmodResponse();            case 2009082821: return new chownResponse();            case 2009082840: return new closeResponse();            case 2009082822: return new creatResponse();            case 2009082823: return new ftruncateResponse();            case 2009082824: return new getattrResponse();            case 2009082825: return new getxattrResponse();            case 2009082826: return new linkResponse();            case 2009082827: return new listxattrResponse();            case 2009082828: return new mkdirResponse();            case 2009082829: return new openResponse();            case 2009082830: return new readdirResponse();            case 2009082831: return new removexattrResponse();            case 2009082832: return new renameResponse();            case 2009082833: return new rmdirResponse();            case 2009082834: return new setattrResponse();            case 2009082835: return new setxattrResponse();            case 2009082836: return new statvfsResponse();            case 2009082837: return new symlinkResponse();            case 2009082838: return new unlinkResponse();            case 2009082839: return new utimensResponse();            case 2009082848: return new xtreemfs_checkpointResponse();            case 2009082849: return new xtreemfs_check_file_existsResponse();            case 2009082850: return new xtreemfs_dump_databaseResponse();            case 2009082851: return new xtreemfs_get_suitable_osdsResponse();            case 2009082852: return new xtreemfs_internal_debugResponse();            case 2009082853: return new xtreemfs_lsvolResponse();            case 2009082854: return new xtreemfs_listdirResponse();            case 2009082855: return new xtreemfs_mkvolResponse();            case 2009082856: return new xtreemfs_renew_capabilityResponse();            case 2009082857: return new xtreemfs_replica_addResponse();            case 2009082858: return new xtreemfs_replica_listResponse();            case 2009082859: return new xtreemfs_replica_removeResponse();            case 2009082860: return new xtreemfs_restore_databaseResponse();            case 2009082861: return new xtreemfs_restore_fileResponse();            case 2009082862: return new xtreemfs_rmvolResponse();            case 2009082863: return new xtreemfs_shutdownResponse();            case 2009082864: return new xtreemfs_update_file_sizeResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }    

}
