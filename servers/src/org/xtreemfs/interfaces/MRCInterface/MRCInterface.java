package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.Exceptions.*;




public class MRCInterface
{        
    public static int getVersion() { return 2; }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getOperationNumber() )
        {
            case 1: return new accessRequest();
            case 2: return new chmodRequest();
            case 3: return new chownRequest();
            case 4: return new createRequest();
            case 30: return new ftruncateRequest();
            case 5: return new getattrRequest();
            case 6: return new getxattrRequest();
            case 7: return new linkRequest();
            case 8: return new listxattrRequest();
            case 9: return new mkdirRequest();
            case 11: return new openRequest();
            case 12: return new readdirRequest();
            case 13: return new removexattrRequest();
            case 14: return new renameRequest();
            case 15: return new rmdirRequest();
            case 17: return new setattrRequest();
            case 18: return new setxattrRequest();
            case 19: return new statvfsRequest();
            case 20: return new symlinkRequest();
            case 21: return new unlinkRequest();
            case 22: return new utimensRequest();
            case 51: return new xtreemfs_checkpointRequest();
            case 23: return new xtreemfs_check_file_existsRequest();
            case 52: return new xtreemfs_dump_databaseRequest();
            case 24: return new xtreemfs_get_suitable_osdsRequest();
            case 100: return new xtreemfs_internal_debugRequest();
            case 31: return new xtreemfs_lsvolRequest();
            case 10: return new xtreemfs_mkvolRequest();
            case 25: return new xtreemfs_renew_capabilityRequest();
            case 26: return new xtreemfs_replica_addRequest();
            case 27: return new xtreemfs_replica_removeRequest();
            case 53: return new xtreemfs_restore_databaseRequest();
            case 28: return new xtreemfs_restore_fileRequest();
            case 16: return new xtreemfs_rmvolRequest();
            case 50: return new xtreemfs_shutdownRequest();
            case 29: return new xtreemfs_update_file_sizeRequest();

            default: throw new Exception( "unknown request number " + Integer.toString( header.getOperationNumber() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 1: return new accessResponse();            case 2: return new chmodResponse();            case 3: return new chownResponse();            case 4: return new createResponse();            case 30: return new ftruncateResponse();            case 5: return new getattrResponse();            case 6: return new getxattrResponse();            case 7: return new linkResponse();            case 8: return new listxattrResponse();            case 9: return new mkdirResponse();            case 11: return new openResponse();            case 12: return new readdirResponse();            case 13: return new removexattrResponse();            case 14: return new renameResponse();            case 15: return new rmdirResponse();            case 17: return new setattrResponse();            case 18: return new setxattrResponse();            case 19: return new statvfsResponse();            case 20: return new symlinkResponse();            case 21: return new unlinkResponse();            case 22: return new utimensResponse();            case 51: return new xtreemfs_checkpointResponse();            case 23: return new xtreemfs_check_file_existsResponse();            case 52: return new xtreemfs_dump_databaseResponse();            case 24: return new xtreemfs_get_suitable_osdsResponse();            case 100: return new xtreemfs_internal_debugResponse();            case 31: return new xtreemfs_lsvolResponse();            case 10: return new xtreemfs_mkvolResponse();            case 25: return new xtreemfs_renew_capabilityResponse();            case 26: return new xtreemfs_replica_addResponse();            case 27: return new xtreemfs_replica_removeResponse();            case 53: return new xtreemfs_restore_databaseResponse();            case 28: return new xtreemfs_restore_fileResponse();            case 16: return new xtreemfs_rmvolResponse();            case 50: return new xtreemfs_shutdownResponse();            case 29: return new xtreemfs_update_file_sizeResponse();
            default: throw new Exception( "unknown response number " + Integer.toString( header.getXID() ) );
        }
    }    

    public static ONCRPCException createException( String exception_type_name ) throws java.io.IOException
    {
        if ( exception_type_name.equals("org::xtreemfs::interfaces::MRCInterface::MRCException") ) return new MRCException();
        else throw new java.io.IOException( "unknown exception type " + exception_type_name );
    }

}
