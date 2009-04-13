package org.xtreemfs.interfaces;
        
        
public enum DebugLevel 
{
    DEBUG_LEVEL_ERROR( 0 ),
    DEBUG_LEVEL_WARN( 1 ),
    DEBUG_LEVEL_INFO( 2 ),
    DEBUG_LEVEL_DEBUG( 3 ),
    DEBUG_LEVEL_TRACE( 4 );    

    private int __value; 
    
    DebugLevel() { this.__value = 0; }
    DebugLevel( int value ) { this.__value = value; }    
    public int intValue() { return __value; }
    
    public static DebugLevel parseInt( int value )
    {
        DebugLevel check_values[] = DebugLevel.values();
        for ( int check_value_i = 0; check_value_i < check_values.length; check_value_i++ )
        {
            if ( check_values[check_value_i].intValue() == value )
                return check_values[check_value_i];            
        }
        return null;        
    }
}
