package org.xtreemfs.interfaces;
        
        
public enum ServiceType 
{
    SERVICE_TYPE_MIXED( 0 ),
    SERVICE_TYPE_MRC( 1 ),
    SERVICE_TYPE_OSD( 2 ),
    SERVICE_TYPE_VOLUME( 3 );    

    private int __value; 
    
    ServiceType() { this.__value = 0; }
    ServiceType( int value ) { this.__value = value; }    
    public int intValue() { return __value; }
    
    public static ServiceType parseInt( int value )
    {
        ServiceType check_values[] = ServiceType.values();
        for ( int check_value_i = 0; check_value_i < check_values.length; check_value_i++ )
        {
            if ( check_values[check_value_i].intValue() == value )
                return check_values[check_value_i];            
        }
        return null;        
    }
}
