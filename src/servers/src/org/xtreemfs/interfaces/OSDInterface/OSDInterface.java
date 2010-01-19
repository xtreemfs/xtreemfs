package org.xtreemfs.interfaces.OSDInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.PrettyPrinter;




public class OSDInterface
{
    public static final int HTTP_PORT_DEFAULT = 30640;
    public static final int ONCRPC_PORT_DEFAULT = 32640;
    public static final int ONCRPCG_PORT_DEFAULT = 32640;
    public static final int ONCRPCS_PORT_DEFAULT = 32640;
    public static final int ONCRPCU_PORT_DEFAULT = 32640;


    public static int getVersion() { return 2010012211; }

    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
            case 2010012212: return new ConcurrentModificationException();
            case 2010012213: return new errnoException();
            case 2010012214: return new InvalidArgumentException();
            case 2010012215: return new OSDException();
            case 2010012216: return new ProtocolException();
            case 2010012217: return new RedirectException();

            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2010012221: return new readRequest();
            case 2010012222: return new truncateRequest();
            case 2010012223: return new unlinkRequest();
            case 2010012224: return new writeRequest();
            case 2010012231: return new xtreemfs_broadcast_gmaxRequest();
            case 2010012232: return new xtreemfs_check_objectRequest();
            case 2010012241: return new xtreemfs_cleanup_get_resultsRequest();
            case 2010012242: return new xtreemfs_cleanup_is_runningRequest();
            case 2010012243: return new xtreemfs_cleanup_startRequest();
            case 2010012244: return new xtreemfs_cleanup_statusRequest();
            case 2010012245: return new xtreemfs_cleanup_stopRequest();
            case 2010012251: return new xtreemfs_internal_get_gmaxRequest();
            case 2010012252: return new xtreemfs_internal_truncateRequest();
            case 2010012253: return new xtreemfs_internal_get_file_sizeRequest();
            case 2010012254: return new xtreemfs_internal_read_localRequest();
            case 2010012255: return new xtreemfs_internal_get_object_setRequest();
            case 2010012261: return new xtreemfs_lock_acquireRequest();
            case 2010012262: return new xtreemfs_lock_checkRequest();
            case 2010012263: return new xtreemfs_lock_releaseRequest();
            case 2010012271: return new xtreemfs_pingRequest();
            case 2010012281: return new xtreemfs_shutdownRequest();

            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2010012221: return new readResponse();            case 2010012222: return new truncateResponse();            case 2010012223: return new unlinkResponse();            case 2010012224: return new writeResponse();            case 2010012231: return new xtreemfs_broadcast_gmaxResponse();            case 2010012232: return new xtreemfs_check_objectResponse();            case 2010012241: return new xtreemfs_cleanup_get_resultsResponse();            case 2010012242: return new xtreemfs_cleanup_is_runningResponse();            case 2010012243: return new xtreemfs_cleanup_startResponse();            case 2010012244: return new xtreemfs_cleanup_statusResponse();            case 2010012245: return new xtreemfs_cleanup_stopResponse();            case 2010012251: return new xtreemfs_internal_get_gmaxResponse();            case 2010012252: return new xtreemfs_internal_truncateResponse();            case 2010012253: return new xtreemfs_internal_get_file_sizeResponse();            case 2010012254: return new xtreemfs_internal_read_localResponse();            case 2010012255: return new xtreemfs_internal_get_object_setResponse();            case 2010012261: return new xtreemfs_lock_acquireResponse();            case 2010012262: return new xtreemfs_lock_checkResponse();            case 2010012263: return new xtreemfs_lock_releaseResponse();            case 2010012271: return new xtreemfs_pingResponse();            case 2010012281: return new xtreemfs_shutdownResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }    

}
