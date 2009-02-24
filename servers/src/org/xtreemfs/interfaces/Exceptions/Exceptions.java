package org.xtreemfs.interfaces.Exceptions;

import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.interfaces.utils.Request;
import org.xtreemfs.interfaces.utils.Response;


public class Exceptions
{
    public static int getVersion() { return 1; }

    public static Exception createException( String exception_type_name ) throws Exception
    {
        if ( exception_type_name == "ProtocolException" ) return new ProtocolException();
        else if ( exception_type_name == "errnoException" ) return new errnoException();
        else throw new Exception( "unknown exception type " + exception_type_name );
    }
}
