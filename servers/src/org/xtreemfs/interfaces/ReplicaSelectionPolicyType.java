package org.xtreemfs.interfaces;
        
        
public enum ReplicaSelectionPolicyType 
{
    REPLICA_SELECTION_POLICY_SIMPLE( 1 );    

    private int __value; 
    
    ReplicaSelectionPolicyType() { this.__value = 0; }
    ReplicaSelectionPolicyType( int value ) { this.__value = value; }    
    public int intValue() { return __value; }
    
    public static ReplicaSelectionPolicyType parseInt( int value )
    {
        ReplicaSelectionPolicyType check_values[] = ReplicaSelectionPolicyType.values();
        for ( int check_value_i = 0; check_value_i < check_values.length; check_value_i++ )
        {
            if ( check_values[check_value_i].intValue() == value )
                return check_values[check_value_i];            
        }
        return null;        
    }
}
