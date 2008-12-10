/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc.slices;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.xtreemfs.mrc.brain.storage.SliceID;

/**
 * This class stores all immutable metadata about an XtreemFS volume.
 * 
 * Volumes are the top-level structural elements of the file system. Each volume
 * has its own directory tree. A volume has a globally unique name and id.
 * 
 * In some cases, it might be required that a volume is not registered at the
 * directory service upon creation or system restart. For this purpose, a flag
 * 'registerAtDS' exists. At the moment, this feature is used in connection with
 * master-slave replication for slave replicas of volumes.
 * 
 * Moreover, a volume holds several immutable policies. The OSD policy
 * determines which OSDs may by default be allocated to files. The access
 * control policy defines the circumstances under which users are allowed to
 * access the volume. The partitioning policy determines how a volume is split
 * up into slices.
 * 
 * @author stender
 * 
 */
public class VolumeInfo implements Serializable {
    
    private String                        id;
    
    private String                        name;
    
    private long                          osdPolicyId;
    
    private String                        osdPolicyArgs;
    
    private long                          partitioningPolicyId;
    
    private long                          acPolicyId;
    
    private boolean                       registerAtDS;
    
    private final Map<SliceID, SliceInfo> slices;
    
    public VolumeInfo(String id, String name, long fileAccessPolicyId, long osdPolicyId,
        long partitioningPolicyId, boolean registerAtDS) {
        
        this.id = id;
        this.name = name;
        this.acPolicyId = fileAccessPolicyId;
        this.osdPolicyId = osdPolicyId;
        this.partitioningPolicyId = partitioningPolicyId;
        this.registerAtDS = registerAtDS;
        this.slices = new HashMap<SliceID, SliceInfo>();
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public long getOsdPolicyId() {
        return osdPolicyId;
    }
    
    public void setOsdPolicyId(long osdPolicyID) {
        this.osdPolicyId = osdPolicyID;
    }
    
    public String getOsdPolicyArgs() {
        return osdPolicyArgs;
    }
    
    public void setOsdPolicyArgs(String osdPolicyArgs) {
        this.osdPolicyArgs = osdPolicyArgs;
    }
    
    public long getAcPolicyId() {
        return acPolicyId;
    }
    
    public void setAcPolicyId(long fileAccessPolicyId) {
        this.acPolicyId = fileAccessPolicyId;
    }
    
    public long getPartitioningPolicyId() {
        return partitioningPolicyId;
    }
    
    public void setPartitioningPolicyId(long partitioningPolicyId) {
        this.partitioningPolicyId = partitioningPolicyId;
    }
    
    public boolean isRegisterAtDS() {
        return registerAtDS;
    }
    
    public void setRegisterAtDS(boolean registerAtDSOnStartup) {
        this.registerAtDS = registerAtDSOnStartup;
    }
    
    public SliceInfo getSlice(SliceID sliceId) {
        return slices.get(sliceId);
    }
    
    public void setSlice(SliceID sliceId, SliceInfo slice) {
        if (slice == null)
            slices.remove(sliceId);
        else
            slices.put(sliceId, slice);
    }
    
    public Collection<SliceInfo> getSlices() {
        return slices.values();
    }
    
}
