package org.xtreemfs.interfaces;
        
        
public enum OSDSelectionPolicyType 
{
    OSD_SELECTION_POLICY_SIMPLE( 1 ),
    OSD_SELECTION_POLICY_PROXIMITY( 2 ),
    OSD_SELECTION_POLICY_DNS( 3 );    

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
