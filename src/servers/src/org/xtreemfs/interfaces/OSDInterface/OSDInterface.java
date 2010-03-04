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


    public static long getProg() { return 2546901526l; }
    public static int getVersion() { return 2010030614; }

    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
            case 2010030615: return new ConcurrentModificationException();
            case 2010030616: return new errnoException();
            case 2010030617: return new InvalidArgumentException();
            case 2010030618: return new OSDException();
            case 2010030619: return new ProtocolException();
            case 2010030620: return new RedirectException();

            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2010030624: return new readRequest();
            case 2010030625: return new truncateRequest();
            case 2010030626: return new unlinkRequest();
            case 2010030627: return new writeRequest();
            case 2010030634: return new xtreemfs_broadcast_gmaxRequest();
            case 2010030635: return new xtreemfs_check_objectRequest();
            case 2010030644: return new xtreemfs_cleanup_get_resultsRequest();
            case 2010030645: return new xtreemfs_cleanup_is_runningRequest();
            case 2010030646: return new xtreemfs_cleanup_startRequest();
            case 2010030647: return new xtreemfs_cleanup_statusRequest();
            case 2010030648: return new xtreemfs_cleanup_stopRequest();
            case 2010030687: return new xtreemfs_rwr_fetchRequest();
            case 2010030685: return new xtreemfs_rwr_flease_msgRequest();
            case 2010030689: return new xtreemfs_rwr_notifyRequest();
            case 2010030690: return new xtreemfs_rwr_statusRequest();
            case 2010030688: return new xtreemfs_rwr_truncateRequest();
            case 2010030686: return new xtreemfs_rwr_updateRequest();
            case 2010030654: return new xtreemfs_internal_get_gmaxRequest();
            case 2010030655: return new xtreemfs_internal_truncateRequest();
            case 2010030656: return new xtreemfs_internal_get_file_sizeRequest();
            case 2010030657: return new xtreemfs_internal_read_localRequest();
            case 2010030658: return new xtreemfs_internal_get_object_setRequest();
            case 2010030664: return new xtreemfs_lock_acquireRequest();
            case 2010030665: return new xtreemfs_lock_checkRequest();
            case 2010030666: return new xtreemfs_lock_releaseRequest();
            case 2010030674: return new xtreemfs_pingRequest();
            case 2010030684: return new xtreemfs_shutdownRequest();

            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2010030624: return new readResponse();            case 2010030625: return new truncateResponse();            case 2010030626: return new unlinkResponse();            case 2010030627: return new writeResponse();            case 2010030634: return new xtreemfs_broadcast_gmaxResponse();            case 2010030635: return new xtreemfs_check_objectResponse();            case 2010030644: return new xtreemfs_cleanup_get_resultsResponse();            case 2010030645: return new xtreemfs_cleanup_is_runningResponse();            case 2010030646: return new xtreemfs_cleanup_startResponse();            case 2010030647: return new xtreemfs_cleanup_statusResponse();            case 2010030648: return new xtreemfs_cleanup_stopResponse();            case 2010030687: return new xtreemfs_rwr_fetchResponse();            case 2010030685: return new xtreemfs_rwr_flease_msgResponse();            case 2010030689: return new xtreemfs_rwr_notifyResponse();            case 2010030690: return new xtreemfs_rwr_statusResponse();            case 2010030688: return new xtreemfs_rwr_truncateResponse();            case 2010030686: return new xtreemfs_rwr_updateResponse();            case 2010030654: return new xtreemfs_internal_get_gmaxResponse();            case 2010030655: return new xtreemfs_internal_truncateResponse();            case 2010030656: return new xtreemfs_internal_get_file_sizeResponse();            case 2010030657: return new xtreemfs_internal_read_localResponse();            case 2010030658: return new xtreemfs_internal_get_object_setResponse();            case 2010030664: return new xtreemfs_lock_acquireResponse();            case 2010030665: return new xtreemfs_lock_checkResponse();            case 2010030666: return new xtreemfs_lock_releaseResponse();            case 2010030674: return new xtreemfs_pingResponse();            case 2010030684: return new xtreemfs_shutdownResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }    

}
