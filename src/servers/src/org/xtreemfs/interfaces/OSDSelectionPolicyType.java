package org.xtreemfs.interfaces;
        
        
public enum OSDSelectionPolicyType 
{
    OSD_SELECTION_POLICY_FILTER_DEFAULT( 1000 ),
    OSD_SELECTION_POLICY_FILTER_FQDN( 1001 ),
    OSD_SELECTION_POLICY_FILTER_UUID( 1002 ),
    OSD_SELECTION_POLICY_GROUP_DCMAP( 2000 ),
    OSD_SELECTION_POLICY_GROUP_FQDN( 2001 ),
    OSD_SELECTION_POLICY_SORT_DCMAP( 3000 ),
    OSD_SELECTION_POLICY_SORT_FQDN( 3001 ),
    OSD_SELECTION_POLICY_SORT_RANDOM( 3002 ),
    OSD_SELECTION_POLICY_SORT_VIVALDI( 3003 );    

    private int __value; 
    
    OSDSelectionPolicyType() { this.__value = 0; }
    OSDSelectionPolicyType( int value ) { this.__value = value; }    
    public int intValue() { return __value; }
    
    public static OSDSelectionPolicyType parseInt( int value )
    {
        OSDSelectionPolicyType check_values[] = OSDSelectionPolicyType.values();
        for ( int check_value_i = 0; check_value_i < check_values.length; check_value_i++ )
        {
            if ( check_values[check_value_i].intValue() == value )
                return check_values[check_value_i];            
        }
        return null;        
    }
}
