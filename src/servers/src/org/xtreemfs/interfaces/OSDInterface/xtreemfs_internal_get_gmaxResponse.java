package org.xtreemfs.interfaces.OSDInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_internal_get_gmaxResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2010031256;

    public xtreemfs_internal_get_gmaxResponse() { _return_value = new InternalGmax();  }
    public xtreemfs_internal_get_gmaxResponse( InternalGmax _return_value ) { this._return_value = _return_value; }

    public InternalGmax get_return_value() { return _return_value; }
    public void set_return_value( InternalGmax _return_value ) { this._return_value = _return_value; }

    // java.lang.Object
    public String toString()
    {
        StringWriter string_writer = new StringWriter();
        string_writer.append(this.getClass().getCanonicalName());
        string_writer.append(" ");
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }

    // java.io.Serializable
    public static final long serialVersionUID = 2010031256;

    // yidl.runtime.Object
    public int getTag() { return 2010031256; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_get_gmaxResponse"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += _return_value.getXDRSize(); // _return_value
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "_return_value", _return_value );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        _return_value = new InternalGmax(); unmarshaller.readStruct( "_return_value", _return_value );
    }

    private InternalGmax _return_value;
}
