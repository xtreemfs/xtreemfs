package org.xtreemfs.dir;


public interface DIRStatusListener {
    
    public void addressMappingAdded();
    
    public void addressMappingDeleted();
    
    public void DIRConfigChanged(DIRConfig config);
    
    public void serviceRegistered();
    
    public void serviceDeregistered();
}
