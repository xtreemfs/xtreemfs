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
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.new_mrc.volumes.metadata;

/**
 * This interface defines how volume-related metadata is accessed.
 * 
 * XtreemFS file system content is arranged in volumes, with each volume having
 * its own directory tree. A volume has a globally unique name and id.
 * 
 * A volume holds different policies. The OSD policy determines which OSDs may
 * by default be allocated to files. The access control policy defines the
 * circumstances under which users are allowed to access the volume. The
 * partitioning policy determines how a volume is split up into slices.
 * 
 * @author stender
 * 
 */
public interface VolumeInfo {
    
    public String getId();
    
    public String getName();
    
    public short getOsdPolicyId();
    
    public String getOsdPolicyArgs();
    
    public short getAcPolicyId();
    
    public void setOsdPolicyId(short osdPolicyId);
    
    public void setOsdPolicyArgs(String osdPolicyArgs);
    
}