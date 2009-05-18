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


    public static int getVersion() { return 1300; }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getOperationNumber() )
        {
            case 1301: return new readRequest();
            case 1302: return new truncateRequest();
            case 1303: return new unlinkRequest();
            case 1304: return new writeRequest();
            case 2300: return new xtreemfs_broadcast_gmaxRequest();
            case 1403: return new xtreemfs_check_objectRequest();
            case 1400: return new xtreemfs_internal_get_gmaxRequest();
            case 1404: return new xtreemfs_internal_get_file_sizeRequest();
            case 1401: return new xtreemfs_internal_truncateRequest();
            case 1402: return new xtreemfs_internal_read_localRequest();
            case 1350: return new xtreemfs_shutdownRequest();
            case 2301: return new xtreemfs_pingRequest();

            default: throw new Exception( "unknown request number " + Integer.toString( header.getOperationNumber() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 1301: return new readResponse();            case 1302: return new truncateResponse();            case 1303: return new unlinkResponse();            case 1304: return new writeResponse();            case 2300: return new xtreemfs_broadcast_gmaxResponse();            case 1403: return new xtreemfs_check_objectResponse();            case 1400: return new xtreemfs_internal_get_gmaxResponse();            case 1404: return new xtreemfs_internal_get_file_sizeResponse();            case 1401: return new xtreemfs_internal_truncateResponse();            case 1402: return new xtreemfs_internal_read_localResponse();            case 1350: return new xtreemfs_shutdownResponse();            case 2301: return new xtreemfs_pingResponse();
            default: throw new Exception( "unknown response number " + Integer.toString( header.getXID() ) );
        }
    }    

    public static ONCRPCException createException( String exception_type_name ) throws java.io.IOException
    {
        if ( exception_type_name.equals("org::xtreemfs::interfaces::OSDInterface::ConcurrentModificationException") ) return new ConcurrentModificationException();
        else if ( exception_type_name.equals("org::xtreemfs::interfaces::OSDInterface::errnoException") ) return new errnoException();
        else if ( exception_type_name.equals("org::xtreemfs::interfaces::OSDInterface::InvalidArgumentException") ) return new InvalidArgumentException();
        else if ( exception_type_name.equals("org::xtreemfs::interfaces::OSDInterface::OSDException") ) return new OSDException();
        else if ( exception_type_name.equals("org::xtreemfs::interfaces::OSDInterface::ProtocolException") ) return new ProtocolException();
        else if ( exception_type_name.equals("org::xtreemfs::interfaces::OSDInterface::RedirectException") ) return new RedirectException();
        else throw new java.io.IOException( "unknown exception type " + exception_type_name );
    }

}
