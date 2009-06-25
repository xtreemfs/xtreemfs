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
package org.xtreemfs.mrc.utils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceDataMap;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.ServiceType;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.PolicyContainer;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.StripingPolicy;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.osdselection.OSDStatusManager;
import org.xtreemfs.mrc.volumes.VolumeManager;
import org.xtreemfs.mrc.volumes.metadata.VolumeInfo;

public class MRCHelper {
    
    public static class GlobalFileIdResolver {
        
        final String volumeId;
        
        final long   localFileId;
        
        public GlobalFileIdResolver(String globalFileId) throws UserException {
            
            try {
                int i = globalFileId.indexOf(':');
                volumeId = globalFileId.substring(0, i);
                localFileId = Long.parseLong(globalFileId.substring(i + 1));
            } catch (Exception exc) {
                throw new UserException(ErrNo.EINVAL, "invalid global file ID: " + globalFileId
                    + "; expected pattern: <volume_ID>:<local_file_ID>");
            }
        }
        
        public String getVolumeId() {
            return volumeId;
        }
        
        public long getLocalFileId() {
            return localFileId;
        }
    }
    
    public enum SysAttrs {
            locations,
            file_id,
            object_type,
            url,
            owner,
            group,
            default_sp,
            ac_policy_id,
            repl_policy_id,
            osdsel_policy_id,
            osdsel_policy_args,
            usable_osds,
            read_only,
            add_replica,
            free_space,
            used_space,
            num_files,
            num_dirs
    }
    
    public enum FileType {
        nexists, dir, file
    }
    
    public static Service createDSVolumeInfo(VolumeInfo vol, OSDStatusManager osdMan, String mrcUUID) {
        
        String free = String.valueOf(osdMan.getFreeSpace(vol.getId()));
        
        ServiceDataMap dmap = new ServiceDataMap();
        dmap.put("mrc", mrcUUID);
        dmap.put("free", free);
        Service sreg = new Service(ServiceType.SERVICE_TYPE_VOLUME, vol.getId(), 0, vol.getName(), 0, dmap);
        
        return sreg;
    }
    
    public static void updateFileTimes(long parentId, FileMetadata file, boolean setATime, boolean setCTime,
        boolean setMTime, StorageManager sMan, AtomicDBUpdate update) throws DatabaseException {
        
        if (parentId == -1)
            return;
        
        int currentTime = (int) (TimeSync.getGlobalTime() / 1000);
        
        if (setATime)
            file.setAtime(currentTime);
        if (setCTime)
            file.setCtime(currentTime);
        if (setMTime)
            file.setMtime(currentTime);
        
        sMan.setMetadata(file, FileMetadata.FC_METADATA, update);
    }
    
    public static Replica createReplica(StripingPolicy stripingPolicy, StorageManager sMan,
        OSDStatusManager osdMan, PolicyContainer policyContainer, VolumeInfo volume, long parentDirId,
        String path, InetAddress clientAddress) throws DatabaseException, UserException, MRCException {
        
        // if no striping policy is provided, try to retrieve it from the parent
        // directory
        if (stripingPolicy == null)
            stripingPolicy = sMan.getDefaultStripingPolicy(parentDirId);
        
        // if the parent directory has no default policy, take the one
        // associated with the volume
        if (stripingPolicy == null)
            stripingPolicy = sMan.getDefaultStripingPolicy(1);
        
        if (stripingPolicy == null)
            throw new UserException(ErrNo.EIO, "could not open file " + path
                + ": no default striping policy available");
        
        org.xtreemfs.interfaces.StripingPolicy sp = new org.xtreemfs.interfaces.StripingPolicy(
            StripingPolicyType.valueOf(stripingPolicy.getPattern()), stripingPolicy.getStripeSize(),
            stripingPolicy.getWidth());
        
        StringSet osds = new StringSet();
        
        ServiceSet osdMaps = osdMan.getUsableOSDs(volume.getId());
        
        if (osdMaps == null || osdMaps.size() == 0)
            throw new UserException(ErrNo.EIO, "could not open file " + path + ": no feasible OSDs available");
        
        // determine the actual striping width; if not enough OSDs are
        // available, the width will be limited to the amount of available OSDs
        int width = Math.min((int) stripingPolicy.getWidth(), osdMaps.size());
        stripingPolicy = sMan.createStripingPolicy(stripingPolicy.getPattern(), stripingPolicy
                .getStripeSize(), width);
        sp.setWidth(width);
        
        // add the OSDs to the X-Locations list, according to the OSD selection
        // policy
        try {
            for (String osd : policyContainer.getOSDSelectionPolicy(volume.getOsdPolicyId())
                    .getOSDsForNewFile(osdMaps, clientAddress, width, volume.getOsdPolicyArgs()))
                osds.add(osd);
        } catch (MRCException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new MRCException(exc);
        }
        
        return new Replica(sp, 0, osds);
    }
    
    /**
     * Checks whether the given replica (i.e. list of OSDs) can be added to the
     * given X-Locations list without compromising consistency.
     * 
     * @param xLocList
     *            the X-Locations list
     * @param newOSDs
     *            the list of new OSDs to add
     * @return <tt>true</tt>, if adding the OSD list is possible, <tt>false</tt>
     *         , otherwise
     */
    public static boolean isAddable(XLocList xLocList, List<String> newOSDs) {
        if (xLocList != null)
            for (int i = 0; i < xLocList.getReplicaCount(); i++) {
                XLoc replica = xLocList.getReplica(i);
                for (int j = 0; j < replica.getOSDCount(); j++)
                    for (String newOsd : newOSDs)
                        if (replica.getOSD(j).equals(newOsd))
                            return false;
            }
        return true;
    }
    
    /**
     * Checks whether the given X-Locations list is consistent. It is regarded
     * as consistent if no OSD in any replica occurs more than once.
     * 
     * @param xLocList
     *            the X-Locations list to check for consistency
     * @return <tt>true</tt>, if the list is consistent, <tt>false</tt>,
     *         otherwise
     */
    public static boolean isConsistent(XLocList xLocList) {
        Set<String> tmp = new HashSet<String>();
        if (xLocList != null) {
            for (int i = 0; i < xLocList.getReplicaCount(); i++) {
                XLoc replica = xLocList.getReplica(i);
                for (int j = 0; j < replica.getOSDCount(); j++) {
                    String osd = replica.getOSD(j);
                    if (!tmp.contains(osd))
                        tmp.add(osd);
                    else
                        return false;
                }
            }
        }
        
        return true;
    }
    
    public static String getSysAttrValue(MRCConfig config, StorageManager sMan, OSDStatusManager osdMan,
        VolumeInfo volume, String path, FileMetadata file, String keyString) throws DatabaseException,
        JSONException, UnknownUUIDException {
        
        SysAttrs key = null;
        try {
            key = SysAttrs.valueOf(keyString);
        } catch (IllegalArgumentException exc) {
            // ignore, will be handled by the 'default' case
        }
        
        if (key != null) {
            
            switch (key) {
            
            case locations:
                if (file.isDirectory()) {
                    return "";
                } else {
                    XLocList xLocList = file.getXLocList();
                    return xLocList == null ? "" : Converter.xLocListToJSON(xLocList);
                }
            case file_id:
                return volume.getId() + ":" + file.getId();
            case object_type:
                String ref = sMan.getSoftlinkTarget(file.getId());
                return ref != null ? "3" : file.isDirectory() ? "2" : "1";
            case url:
                InetSocketAddress addr = config.getDirectoryService();
                return (config.isUsingSSL() ? Constants.ONCRPC_SCHEME : Constants.ONCRPCS_SCHEME) + "://"
                    + addr.getHostName() + ":" + addr.getPort() + "/" + path;
            case owner:
                return file.getOwnerId();
            case group:
                return file.getOwningGroupId();
            case default_sp:
                if (!file.isDirectory())
                    return "";
                StripingPolicy sp = sMan.getDefaultStripingPolicy(file.getId());
                if (sp == null)
                    return "";
                return Converter.stripingPolicyToJSONString(sp);
            case ac_policy_id:
                return file.getId() == 1 ? volume.getAcPolicyId() + "" : "";
            case osdsel_policy_id:
                return file.getId() == 1 ? volume.getOsdPolicyId() + "" : "";
            case osdsel_policy_args:
                return file.getId() == 1 ? (volume.getOsdPolicyArgs() == null ? "" : volume
                        .getOsdPolicyArgs()) : "";
            case repl_policy_id:
                return file.getId() == 1 ? volume.getReplicaPolicyId() + "" : "";
            case read_only:
                if (file.isDirectory())
                    return "";
                
                return String.valueOf(file.isReadOnly());
                
            case usable_osds: {
                ServiceSet srvs = osdMan.getUsableOSDs(volume.getId());
                Map<String, String> osds = new HashMap();
                for (Service srv : srvs) {
                    ServiceUUID uuid = new ServiceUUID(srv.getUuid());
                    InetAddress ia = uuid.getMappings()[0].resolvedAddr.getAddress();
                    osds.put(uuid.toString(), ia.getCanonicalHostName());
                }
                return JSONParser.writeJSON(osds);
            }
            case free_space:
                return file.getId() == 1 ? String.valueOf(osdMan.getFreeSpace(volume.getId())) : "";
            case used_space:
                return file.getId() == 1 ? String.valueOf(sMan.getVolumeSize()) : "";
            case num_files:
                return file.getId() == 1 ? String.valueOf(sMan.getNumFiles()) : "";
            case num_dirs:
                return file.getId() == 1 ? String.valueOf(sMan.getNumDirs()) : "";
            }
        }
        
        return "";
    }
    
    public static void setSysAttrValue(StorageManager sMan, VolumeManager vMan, VolumeInfo volume,
        long parentId, FileMetadata file, String keyString, String value, AtomicDBUpdate update)
        throws UserException, DatabaseException {
        
        SysAttrs key = null;
        try {
            key = SysAttrs.valueOf(keyString);
        } catch (IllegalArgumentException exc) {
            // ignore, will be handled by the 'default' case
        }
        
        switch (key) {
        
        case default_sp:

            if (!file.isDirectory())
                throw new UserException(ErrNo.EPERM,
                    "default striping policies can only be set on volumes and directories");
            
            try {
                
                org.xtreemfs.interfaces.StripingPolicy sp = null;
                sp = Converter.jsonStringToStripingPolicy(value);
                
                if (file.getId() == 1 && sp == null)
                    throw new UserException(ErrNo.EPERM, "cannot remove the volume's default striping policy");
                
                sMan.setDefaultStripingPolicy(file.getId(), sp, update);
                
            } catch (JSONException exc) {
                throw new UserException(ErrNo.EINVAL, "invalid default striping policy: " + value);
            } catch (ClassCastException exc) {
                throw new UserException(ErrNo.EINVAL, "invalid default striping policy: " + value);
            } catch (NullPointerException exc) {
                throw new UserException(ErrNo.EINVAL, "invalid default striping policy: " + value);
            } catch (IllegalArgumentException exc) {
                throw new UserException(ErrNo.EINVAL, "invalid default striping policy: " + value);
            }
            
            break;
        
        case osdsel_policy_id:

            if (file.getId() != 1)
                throw new UserException(ErrNo.EINVAL, "OSD selection policies can only be set on volumes");
            
            try {
                short newPol = Short.parseShort(value);
                
                volume.setOsdPolicyId(newPol);
                vMan.updateVolume(volume);
                
            } catch (NumberFormatException exc) {
                throw new UserException(ErrNo.EINVAL, "invalid OSD selection policy: " + value);
            }
            
            break;
        
        case osdsel_policy_args:

            if (file.getId() != 1)
                throw new UserException(ErrNo.EINVAL,
                    "OSD selection policies can only be set and configured on volumes");
            
            volume.setOsdPolicyArgs(value);
            vMan.updateVolume(volume);
            
            break;
        
        case repl_policy_id:

            if (file.getId() != 1)
                throw new UserException(ErrNo.EINVAL,
                    "replica selection policies can only be set and configured on volumes");
            
            try {
                short newPol = Short.parseShort(value);
                
                volume.setReplicaPolicyId(newPol);
                vMan.updateVolume(volume);
                
            } catch (NumberFormatException exc) {
                throw new UserException(ErrNo.EINVAL, "invalid replica selection policy: " + value);
            }
            
            break;
        
        case add_replica: {
            try {
                Replica newReplica = Converter.replicaFromJSON(value);
                // FIXME: execute add replica here!
            } catch (JSONException exc) {
                throw new UserException(ErrNo.EINVAL, "invalid default striping policy: " + value);
            } catch (ClassCastException exc) {
                throw new UserException(ErrNo.EINVAL, "invalid default striping policy: " + value);
            } catch (NullPointerException exc) {
                throw new UserException(ErrNo.EINVAL, "invalid default striping policy: " + value);
            } catch (IllegalArgumentException exc) {
                throw new UserException(ErrNo.EINVAL, "invalid default striping policy: " + value);
            }
            break;
        }
            
        case read_only:

            if (file.isDirectory())
                throw new UserException(ErrNo.EPERM, "only files can be made read-only");
            
            boolean readOnly = Boolean.valueOf(value);
            
            if (!readOnly && file.getXLocList() != null && file.getXLocList().getReplicaCount() > 1)
                throw new UserException(ErrNo.EPERM,
                    "read-only flag cannot be removed from files with multiple replicas");
            
            // set the update policy string in the X-Locations list to 'read
            // only replication' and mark the first replica as 'full'
            if (file.getXLocList() != null) {
                XLocList xLoc = file.getXLocList();
                XLoc[] replicas = new XLoc[xLoc.getReplicaCount()];
                for (int i = 0; i < replicas.length; i++)
                    replicas[i] = xLoc.getReplica(i);
                
                replicas[0].setReplicationFlags(replicas[0].getReplicationFlags()
                    | Constants.REPL_FLAG_IS_FULL);
                
                XLocList newXLoc = sMan.createXLocList(replicas, readOnly ? Constants.REPL_UPDATE_PC_RONLY
                    : Constants.REPL_UPDATE_PC_NONE, xLoc.getVersion());
                file.setXLocList(newXLoc);
                sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
            }
            
            // set the read-only flag
            file.setReadOnly(readOnly);
            sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
            
            break;
        
        default:
            throw new UserException(ErrNo.EINVAL, "system attribute '" + key + "' unknown or immutable");
        }
    }
}
