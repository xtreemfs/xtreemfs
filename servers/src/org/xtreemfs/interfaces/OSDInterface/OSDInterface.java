package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.Exceptions.*;




public class OSDInterface
{
    public static final int DEFAULT_ONCRPC_PORT = 32640;
    public static final int DEFAULT_ONCRPCS_PORT = 32640;
    public static final int DEFAULT_HTTP_PORT = 30640;


    public static int getVersion() { return 3; }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getOperationNumber() )
        {
            case 1: return new readRequest();
            case 2: return new truncateRequest();
            case 3: return new unlinkRequest();
            case 4: return new writeRequest();
            case 103: return new xtreemfs_check_objectRequest();
            case 100: return new xtreemfs_internal_get_gmaxRequest();
            case 104: return new xtreemfs_internal_get_file_sizeRequest();
            case 101: return new xtreemfs_internal_truncateRequest();
            case 102: return new xtreemfs_internal_read_localRequest();
            case 50: return new xtreemfs_shutdownRequest();

            default: throw new Exception( "unknown request number " + Integer.toString( header.getOperationNumber() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 1: return new readResponse();            case 2: return new truncateResponse();            case 3: return new unlinkResponse();            case 4: return new writeResponse();            case 103: return new xtreemfs_check_objectResponse();            case 100: return new xtreemfs_internal_get_gmaxResponse();            case 104: return new xtreemfs_internal_get_file_sizeResponse();            case 101: return new xtreemfs_internal_truncateResponse();            case 102: return new xtreemfs_internal_read_localResponse();            case 50: return new xtreemfs_shutdownResponse();
            default: throw new Exception( "unknown response number " + Integer.toString( header.getXID() ) );
        }
    }    

    public static ONCRPCException createException( String exception_type_name ) throws java.io.IOException
    {
        if ( exception_type_name.equals("org::xtreemfs::interfaces::OSDInterface::OSDException") ) return new OSDException();
        else throw new java.io.IOException( "unknown exception type " + exception_type_name );
    }

}
