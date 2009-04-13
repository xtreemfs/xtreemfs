package org.xtreemfs.interfaces;
        
        
public enum AccessControlPolicyType 
{
    ACCESS_CONTROL_POLICY_NULL( 1 ),
    ACCESS_CONTROL_POLICY_POSIX( 2 ),
    ACCESS_CONTROL_POLICY_VOLUME( 3 );    

    private int __value; 
    
    AccessControlPolicyType() { this.__value = 0; }
    AccessControlPolicyType( int value ) { this.__value = value; }    
    public int intValue() { return __value; }
    
    public static AccessControlPolicyType parseInt( int value )
    {
        AccessControlPolicyType check_values[] = AccessControlPolicyType.values();
        for ( int check_value_i = 0; check_value_i < check_values.length; check_value_i++ )
        {
            if ( check_values[check_value_i].intValue() == value )
                return check_values[check_value_i];            
        }
        return null;        
    }
}
