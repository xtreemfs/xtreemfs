package org.xtreemfs.interfaces;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class ObjectList extends Struct
{
    public static final int TAG = 2009082673;
    
    public ObjectList() {  }
    public ObjectList( ReusableBuffer set, int stripeWidth, int firstObjectNo ) { this.set = set; this.stripeWidth = stripeWidth; this.firstObjectNo = firstObjectNo; }

    public ReusableBuffer getSet() { return set; }
    public void setSet( ReusableBuffer set ) { this.set = set; }
    public int getStripeWidth() { return stripeWidth; }
    public void setStripeWidth( int stripeWidth ) { this.stripeWidth = stripeWidth; }
    public int getFirstObjectNo() { return firstObjectNo; }
    public void setFirstObjectNo( int firstObjectNo ) { this.firstObjectNo = firstObjectNo; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082673;    

    // yidl.Object
    public int getTag() { return 2009082673; }
    public String getTypeName() { return "org::xtreemfs::interfaces::ObjectList"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += set != null ? ( ( set.remaining() + Integer.SIZE/8 ) % 4 == 0 ) ? ( set.remaining() + Integer.SIZE/8 ) : ( set.remaining() + Integer.SIZE/8 + 4 - ( set.remaining() + Integer.SIZE/8 ) % 4 ) : 0;
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeBuffer( "set", set );
        marshaller.writeUint32( "stripeWidth", stripeWidth );
        marshaller.writeUint32( "firstObjectNo", firstObjectNo );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        set = ( ReusableBuffer )unmarshaller.readBuffer( "set" );
        stripeWidth = unmarshaller.readUint32( "stripeWidth" );
        firstObjectNo = unmarshaller.readUint32( "firstObjectNo" );    
    }
        
    

    private ReusableBuffer set;
    private int stripeWidth;
    private int firstObjectNo;    

}

