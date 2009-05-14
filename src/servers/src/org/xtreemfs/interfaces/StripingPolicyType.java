package org.xtreemfs.interfaces;
        
        
public enum StripingPolicyType 
{
    STRIPING_POLICY_RAID0( 0 );    

    private int __value; 
    
    StripingPolicyType() { this.__value = 0; }
    StripingPolicyType( int value ) { this.__value = value; }    
    public int intValue() { return __value; }
    
    public static StripingPolicyType parseInt( int value )
    {
        StripingPolicyType check_values[] = StripingPolicyType.values();
        for ( int check_value_i = 0; check_value_i < check_values.length; check_value_i++ )
        {
            if ( check_values[check_value_i].intValue() == value )
                return check_values[check_value_i];            
        }
        return null;        
    }
}
