package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.Exceptions.*;




public class OSDInterface
{        
    public static int getVersion() { return 3; }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getOperationNumber() )
        {
            case 102: return new internal_read_localRequest();
            case 103: return new check_objectRequest();
            case 2: return new truncateRequest();
            case 1: return new readRequest();
            case 5: return new keep_file_openRequest();
            case 4: return new writeRequest();
            case 50: return new admin_shutdownRequest();
            case 100: return new internal_get_gmaxRequest();
            case 101: return new internal_truncateRequest();
            case 3: return new unlinkRequest();

            default: throw new Exception( "unknown request number " + Integer.toString( header.getOperationNumber() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 102: return new internal_read_localResponse();            case 103: return new check_objectResponse();            case 2: return new truncateResponse();            case 1: return new readResponse();            case 5: return new keep_file_openResponse();            case 4: return new writeResponse();            case 50: return new admin_shutdownResponse();            case 100: return new internal_get_gmaxResponse();            case 101: return new internal_truncateResponse();            case 3: return new unlinkResponse();
            default: throw new Exception( "unknown response number " + Integer.toString( header.getXID() ) );
        }
    }    

}
