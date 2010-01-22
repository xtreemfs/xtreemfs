package org.xtreemfs.interfaces;
        
        
public enum SnapConfig 
{
    SNAP_CONFIG_SNAPS_DISABLED( 0 ),
    SNAP_CONFIG_ACCESS_CURRENT( 1 ),
    SNAP_CONFIG_ACCESS_SNAP( 2 );    

    private int __value; 
    
    SnapConfig() { this.__value = 0; }
    SnapConfig( int value ) { this.__value = value; }    
    public int intValue() { return __value; }
    
    public static SnapConfig parseInt( int value )
    {
        SnapConfig check_values[] = SnapConfig.values();
        for ( int check_value_i = 0; check_value_i < check_values.length; check_value_i++ )
        {
            if ( check_values[check_value_i].intValue() == value )
                return check_values[check_value_i];            
        }
        return null;        
    }
}
