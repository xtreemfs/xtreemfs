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




public class xtreemfs_lock_checkResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2010031267;

    public xtreemfs_lock_checkResponse() { _return_value = new Lock();  }
    public xtreemfs_lock_checkResponse( Lock _return_value ) { this._return_value = _return_value; }

    public Lock get_return_value() { return _return_value; }
    public void set_return_value( Lock _return_value ) { this._return_value = _return_value; }

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
    public static final long serialVersionUID = 2010031267;

    // yidl.runtime.Object
    public int getTag() { return 2010031267; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_lock_checkResponse"; }

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
        _return_value = new Lock(); unmarshaller.readStruct( "_return_value", _return_value );
    }

    private Lock _return_value;
}
