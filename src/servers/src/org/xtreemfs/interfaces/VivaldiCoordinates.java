package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class VivaldiCoordinates implements org.xtreemfs.interfaces.utils.Serializable
{
    public static final int TAG = 1003;

    
    public VivaldiCoordinates() { x_coordinate = 0; y_coordinate = 0; local_error = 0; }
    public VivaldiCoordinates( double x_coordinate, double y_coordinate, double local_error ) { this.x_coordinate = x_coordinate; this.y_coordinate = y_coordinate; this.local_error = local_error; }
    public VivaldiCoordinates( Object from_hash_map ) { x_coordinate = 0; y_coordinate = 0; local_error = 0; this.deserialize( from_hash_map ); }
    public VivaldiCoordinates( Object[] from_array ) { x_coordinate = 0; y_coordinate = 0; local_error = 0;this.deserialize( from_array ); }

    public double getX_coordinate() { return x_coordinate; }
    public void setX_coordinate( double x_coordinate ) { this.x_coordinate = x_coordinate; }
    public double getY_coordinate() { return y_coordinate; }
    public void setY_coordinate( double y_coordinate ) { this.y_coordinate = y_coordinate; }
    public double getLocal_error() { return local_error; }
    public void setLocal_error( double local_error ) { this.local_error = local_error; }

    // Object
    public String toString()
    {
        return "VivaldiCoordinates( " + Double.toString( x_coordinate ) + ", " + Double.toString( y_coordinate ) + ", " + Double.toString( local_error ) + " )";
    }

    // Serializable
    public int getTag() { return 1003; }
    public String getTypeName() { return "org::xtreemfs::interfaces::VivaldiCoordinates"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.x_coordinate = ( ( Double )from_hash_map.get( "x_coordinate" ) ).doubleValue();
        this.y_coordinate = ( ( Double )from_hash_map.get( "y_coordinate" ) ).doubleValue();
        this.local_error = ( ( Double )from_hash_map.get( "local_error" ) ).doubleValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.x_coordinate = ( ( Double )from_array[0] ).doubleValue();
        this.y_coordinate = ( ( Double )from_array[1] ).doubleValue();
        this.local_error = ( ( Double )from_array[2] ).doubleValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        x_coordinate = buf.getDouble();
        y_coordinate = buf.getDouble();
        local_error = buf.getDouble();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "x_coordinate", new Double( x_coordinate ) );
        to_hash_map.put( "y_coordinate", new Double( y_coordinate ) );
        to_hash_map.put( "local_error", new Double( local_error ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        writer.putDouble( x_coordinate );
        writer.putDouble( y_coordinate );
        writer.putDouble( local_error );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += ( Double.SIZE / 8 );
        my_size += ( Double.SIZE / 8 );
        my_size += ( Double.SIZE / 8 );
        return my_size;
    }


    private double x_coordinate;
    private double y_coordinate;
    private double local_error;    

}

