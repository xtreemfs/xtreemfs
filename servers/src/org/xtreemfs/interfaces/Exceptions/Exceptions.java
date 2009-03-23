package org.xtreemfs.interfaces.Exceptions;

import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.Exceptions.*;




public class Exceptions
{        
    public static int getVersion() { return 1; }

    public static ONCRPCException createException( String exception_type_name ) throws java.io.IOException
    {
        if ( exception_type_name.equals("org::xtreemfs::interfaces::Exceptions::ProtocolException") ) return new ProtocolException();
        else if ( exception_type_name.equals("org::xtreemfs::interfaces::Exceptions::errnoException") ) return new errnoException();
        else if ( exception_type_name.equals("org::xtreemfs::interfaces::Exceptions::RedirectException") ) return new RedirectException();
        else if ( exception_type_name.equals("org::xtreemfs::interfaces::Exceptions::ConcurrentModificationException") ) return new ConcurrentModificationException();
        else if ( exception_type_name.equals("org::xtreemfs::interfaces::Exceptions::InvalidArgumentException") ) return new InvalidArgumentException();
        else throw new java.io.IOException( "unknown exception type " + exception_type_name );
    }

}
