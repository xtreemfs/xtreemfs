/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.utils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseResultSet;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.ReplicationPolicy;
import org.xtreemfs.mrc.metadata.StripingPolicy;
import org.xtreemfs.mrc.metadata.XAttr;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.osdselection.OSDStatusManager;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceDataMap;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

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
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "invalid global file ID: " + globalFileId
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
    
    /**
     * Create a global fileId for the file on the volume.
     * 
     * @param volume
     * @param file
     * @return Global fileId
     */
    public static String createGlobalFileId(VolumeInfo volume, FileMetadata file) {
        return volume.getId() + ":" + file.getId();
    }

    public static final String POLICY_ATTR_PREFIX = "policies";
    
    public static final String VOL_ATTR_PREFIX    = "volattr";
    
    public static final String XTREEMFS_POLICY_ATTR_PREFIX = "xtreemfs." + POLICY_ATTR_PREFIX + ".";
    
    public enum SysAttrs {
            locations,
            file_id,
            object_type,
            url,
            owner,
            group,
            default_sp,
            ac_policy_id,
            rsel_policy,
            osel_policy,
            usable_osds,
            free_space,
            used_space,
            num_files,
            num_dirs,
            snapshots,
            snapshots_enabled,
            snapshot_time,
            acl,
            read_only,
            mark_replica_complete,
            set_repl_update_policy,
            default_rp,
            quota
    }
    
    public enum FileType {
        nexists, dir, file
    }
    
    public static Service createDSVolumeInfo(VolumeInfo vol, OSDStatusManager osdMan, StorageManager sMan,
            String mrcUUID) {
        
        String free = String.valueOf(osdMan.getFreeSpace(vol.getId()));
        String volSize = null;
        try {
            volSize = String.valueOf(sMan.getVolumeInfo().getVolumeSize());
            try {
                // Use minimum of free space relative to the quota and free space on osds as free disk space.
                long quota = vol.getVolumeQuota();
                long quotaFreeSpace = quota - vol.getVolumeSize();
                if (quota != 0 && quotaFreeSpace < Long.valueOf(free)) {
                    quotaFreeSpace = quotaFreeSpace < 0 ? 0 : quotaFreeSpace;
                    free = String.valueOf(quotaFreeSpace);
                }
            } catch (DatabaseException e) {
                Logging.logMessage(Logging.LEVEL_WARN, Category.storage, null,
                        "could not retrieve volume quota from database for volume '%s': %s",
                        new Object[] { vol.getName(), e.toString() });
            }
        } catch (DatabaseException e) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.storage, null,
                    "could not retrieve volume size from database for volume '%s': %s",
                    new Object[] { vol.getName(), e.toString() });
        }
        
        ServiceDataMap.Builder dmap = buildServiceDataMap("mrc", mrcUUID, "free", free, "used", volSize);
        
        try {
            
            DatabaseResultSet<XAttr> attrIt = sMan.getXAttrs(1, StorageManager.SYSTEM_UID);
            while (attrIt.hasNext()) {
                XAttr attr = attrIt.next();
                if (attr.getKey().startsWith("xtreemfs.volattr.")) {
                    byte[] value = attr.getValue();
                    dmap.addData(KeyValuePair.newBuilder()
                            .setKey("attr." + attr.getKey().substring("xtreemfs.volattr.".length()))
                            .setValue(value == null ? null : new String(value)));
                }
            }
            attrIt.destroy();
            
        } catch (DatabaseException e) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.storage, null, OutputUtils.stackTraceToString(e),
                    new Object[0]);
        }
        
        Service sreg = Service.newBuilder().setType(ServiceType.SERVICE_TYPE_VOLUME).setUuid(vol.getId()).setVersion(0)
                .setName(vol.getName()).setData(dmap).setLastUpdatedS(0).build();
        
        return sreg;
    }
    
    public static int updateFileTimes(long parentId, FileMetadata file, boolean setATime, boolean setCTime,
            boolean setMTime, StorageManager sMan, int currentTime, AtomicDBUpdate update) throws DatabaseException {
        
        if (parentId == -1)
            return -1;
        
        if (setATime)
            file.setAtime(currentTime);
        if (setCTime)
            file.setCtime(currentTime);
        if (setMTime)
            file.setMtime(currentTime);
        
        sMan.setMetadata(file, FileMetadata.FC_METADATA, update);
        
        return currentTime;
    }
    
    public static XLoc createReplica(StripingPolicy stripingPolicy, StorageManager sMan, OSDStatusManager osdMan,
            VolumeInfo volume, long parentDirId, String path, InetAddress clientAddress,
            VivaldiCoordinates clientCoordinates, XLocList currentXLoc, int replFlags) throws DatabaseException,
            UserException, MRCException {
        
        // if no striping policy is provided, try to retrieve it from the parent
        // directory
        if (stripingPolicy == null)
            stripingPolicy = sMan.getDefaultStripingPolicy(parentDirId);
        
        // if the parent directory has no default policy, take the one
        // associated with the volume
        if (stripingPolicy == null)
            stripingPolicy = sMan.getDefaultStripingPolicy(1);
        
        if (stripingPolicy == null)
            throw new UserException(POSIXErrno.POSIX_ERROR_EIO, "could not open file " + path
                    + ": no default striping policy available");
        
        // determine the set of OSDs to be assigned to the replica
        ServiceSet.Builder usableOSDs = osdMan.getUsableOSDs(volume.getId(), clientAddress, clientCoordinates,
                currentXLoc, stripingPolicy.getWidth());
        
        if (usableOSDs == null || usableOSDs.getServicesCount() == 0) {
            
            Logging.logMessage(Logging.LEVEL_WARN, Category.all, (Object) null,
                    "no suitable OSDs available for file %s", path);
            
            throw new UserException(POSIXErrno.POSIX_ERROR_EIO, "could not assign OSDs to file " + path
                    + ": no feasible OSDs available");
        }
        
        // determine the actual striping width; if not enough OSDs are
        // available, the width will be limited to the amount of available OSDs
        int width = Math.min(stripingPolicy.getWidth(), usableOSDs.getServicesCount());
        
        // convert the set of OSDs to a string array of OSD UUIDs
        List<Service> osdServices = usableOSDs.getServicesList();
        String[] osds = new String[width];
        for (int i = 0; i < width; i++)
            osds[i] = osdServices.get(i).getUuid();
        
        if (width != stripingPolicy.getWidth())
            stripingPolicy = sMan.createStripingPolicy(stripingPolicy.getPattern(), stripingPolicy.getStripeSize(),
                    width);
        
        return sMan.createXLoc(stripingPolicy, osds, replFlags);
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
     * Checks whether all service UUIDs from the list can be resolved, i.e.
     * refer to valid services.
     * 
     * @param newOSDs
     *            the list of OSDs
     * @return <tt>true</tt>, if all OSDs are resolvable, <tt>false</tt>,
     *         otherwise
     */
    public static boolean isResolvable(List<String> newOSDs) {
        if (newOSDs != null)
            for (String osd : newOSDs) {
                try {
                    new ServiceUUID(osd).getAddress();
                } catch (Exception exc) {
                    return false;
                }
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
            FileAccessManager faMan, String path, FileMetadata file, String keyString) throws DatabaseException,
            UserException, JSONException {
        
        if (keyString.startsWith(POLICY_ATTR_PREFIX + "."))
            return getPolicyValue(sMan, keyString);
        
        if (keyString.startsWith(VOL_ATTR_PREFIX + "."))
            return getVolAttrValue(sMan, keyString);
        
        SysAttrs key = null;
        try {
            key = SysAttrs.valueOf(keyString);
        } catch (IllegalArgumentException exc) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "unknown system attribute '" + keyString + "'");
        }
        
        if (key != null) {
            
            switch (key) {
            
            case locations:
                if (file.isDirectory()) {
                    return "";
                } else {
                    XLocList xLocList = file.getXLocList();
                    
                    try {
                        return xLocList == null ? "" : Converter.xLocListToJSON(xLocList, osdMan);
                    } catch (UnknownUUIDException exc) {
                        throw new UserException(POSIXErrno.POSIX_ERROR_EIO, "cannot retrieve '"
                                + SysAttrs.locations.name() + "' attribute value: " + exc);
                    }
                }
            case file_id:
                return createGlobalFileId(sMan.getVolumeInfo(), file);
            case object_type:
                String ref = sMan.getSoftlinkTarget(file.getId());
                return ref != null ? "3" : file.isDirectory() ? "2" : "1";
            case url:
                InetSocketAddress addr = config.getDirectoryService();
                return config.getURLScheme() + "://" + addr.getHostName() + ":" + addr.getPort() + "/" + path;
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
                return file.getId() == 1 ? sMan.getVolumeInfo().getAcPolicyId() + "" : "";
            case osel_policy:
                return file.getId() == 1 ? Converter.shortArrayToString(sMan.getVolumeInfo().getOsdPolicy()) : "";
            case rsel_policy:
                return file.getId() == 1 ? Converter.shortArrayToString(sMan.getVolumeInfo().getReplicaPolicy()) : "";
            case read_only:
                if (file.isDirectory())
                    return "";
                
                return String.valueOf(file.isReadOnly());
                
            case usable_osds: {
                
                // only return a value for the volume root
                if (file.getId() != 1)
                    return "";
                
                try {
                    ServiceSet.Builder srvs = osdMan.getUsableOSDs(sMan.getVolumeInfo().getId());
                    Map<String, String> osds = new HashMap<String, String>();
                    for (Service srv : srvs.getServicesList()) {
                        ServiceUUID uuid = new ServiceUUID(srv.getUuid());
                        osds.put(uuid.toString(), uuid.getAddressString());
                    }
                    return JSONParser.writeJSON(osds);
                    
                } catch (UnknownUUIDException exc) {
                    throw new UserException(POSIXErrno.POSIX_ERROR_EIO, "cannot retrieve '"
                            + SysAttrs.usable_osds.name() + "' attribute value: " + exc);
                }
            }
            case free_space:
                return file.getId() == 1 ? String.valueOf(osdMan.getFreeSpace(sMan.getVolumeInfo().getId())) : "";
            case used_space:
                return file.getId() == 1 ? String.valueOf(sMan.getVolumeInfo().getVolumeSize()) : "";
            case num_files:
                return file.getId() == 1 ? String.valueOf(sMan.getVolumeInfo().getNumFiles()) : "";
            case num_dirs:
                return file.getId() == 1 ? String.valueOf(sMan.getVolumeInfo().getNumDirs()) : "";
                
            case snapshots: {
                
                if (file.getId() != 1 || sMan.getVolumeInfo().isSnapVolume())
                    return "";
                
                String[] snaps = sMan.getAllSnapshots();
                Arrays.sort(snaps);
                List<String> snapshots = new ArrayList<String>(snaps.length);
                for (String snap : snaps) {
                    if (snap.equals(".dump"))
                        continue;
                    
                    snapshots.add(snap);
                }
                
                return JSONParser.writeJSON(snapshots);
            }
            
            case snapshots_enabled:
                return file.getId() == 1 && !sMan.getVolumeInfo().isSnapVolume() ? String.valueOf(sMan.getVolumeInfo()
                        .isSnapshotsEnabled()) : "";
            case snapshot_time:
                return file.getId() == 1 && sMan.getVolumeInfo().isSnapVolume() ? Long.toString(sMan.getVolumeInfo()
                        .getCreationTime()) : "";
                
            case acl:
                
                Map<String, Object> acl;
                try {
                    acl = faMan.getACLEntries(sMan, file);
                } catch (MRCException e) {
                    Logging.logError(Logging.LEVEL_ERROR, null, e);
                    throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL);
                }
                
                if (acl != null) {
                    
                    Map<String, Object> map = new HashMap<String, Object>();
                    for (Entry<String, Object> entry : acl.entrySet())
                        map.put(entry.getKey(), "" + entry.getValue());
                    
                    return JSONParser.writeJSON(map);
                }
                
            case default_rp:
                
                if (!file.isDirectory())
                    return "";
                ReplicationPolicy rp = sMan.getDefaultReplicationPolicy(file.getId());
                if (rp == null)
                    return "";
                
                return Converter.replicationPolicyToJSONString(rp);

            case quota:
                return String.valueOf(sMan.getVolumeInfo().getVolumeQuota());
            }
        }
        
        return "";
    }
    
    public static void setSysAttrValue(StorageManager sMan, VolumeManager vMan, FileAccessManager faMan, long parentId,
            FileMetadata file, String keyString, String value, AtomicDBUpdate update) throws UserException,
            DatabaseException {
        
        // handle policy-specific values
        if (keyString.startsWith(POLICY_ATTR_PREFIX.toString() + ".")) {
            setPolicyValue(sMan, keyString, value, update);
            return;
        }
        
        SysAttrs key = null;
        try {
            key = SysAttrs.valueOf(keyString);
        } catch (IllegalArgumentException exc) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "unknown system attribute '" + keyString + "'");
        }
        
        switch (key) {
        
        case default_sp:
            
            if (!file.isDirectory())
                throw new UserException(POSIXErrno.POSIX_ERROR_EPERM,
                        "default striping policies can only be set on volumes and directories");
            
            try {
                
                org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy sp = null;
                sp = Converter.jsonStringToStripingPolicy(value);
                
                if (file.getId() == 1 && sp == null)
                    throw new UserException(POSIXErrno.POSIX_ERROR_EPERM,
                            "cannot remove the volume's default striping policy");
                
                // check if striping + rw replication would be set
                ReplicationPolicy replPolicy = sMan.getDefaultReplicationPolicy(file.getId());
                if (sp != null
                        && sp.getWidth() > 1
                        && replPolicy != null && (replPolicy.getName().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE) || replPolicy
                                .getName().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ))) {
                    throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                            "Striping of rw-replicated Files is not supported yet.");
                }

                sMan.setDefaultStripingPolicy(file.getId(), sp, update);
                
            } catch (JSONException exc) {
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "invalid default striping policy: " + value);
            } catch (ClassCastException exc) {
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "invalid default striping policy: " + value);
            } catch (NullPointerException exc) {
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "invalid default striping policy: " + value);
            } catch (IllegalArgumentException exc) {
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "invalid default striping policy: " + value);
            }
            
            break;
        
        case osel_policy:
            
            if (file.getId() != 1)
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                        "OSD selection policies can only be set on volumes");
            
            try {
                short[] newPol = Converter.stringToShortArray(value);
                sMan.getVolumeInfo().setOsdPolicy(newPol, update);
                
            } catch (NumberFormatException exc) {
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "invalid OSD selection policy: " + value);
            }
            
            break;
        
        case rsel_policy:
            
            if (file.getId() != 1)
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                        "replica selection policies can only be set and configured on volumes");
            
            try {
                short[] newPol = Converter.stringToShortArray(value);
                sMan.getVolumeInfo().setReplicaPolicy(newPol, update);
                
            } catch (NumberFormatException exc) {
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "invalid replica selection policy: " + value);
            }
            
            break;
        
        case read_only:
            
            // TODO: unify w/ 'set_repl_update_policy'
            
            if (file.isDirectory())
                throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "only files can be made read-only");
            
            boolean readOnly = Boolean.valueOf(value);
            
            if (!readOnly && file.getXLocList() != null && file.getXLocList().getReplicaCount() > 1)
                throw new UserException(POSIXErrno.POSIX_ERROR_EPERM,
                        "read-only flag cannot be removed from files with multiple replicas");
            
            // set the update policy string in the X-Locations list to 'read
            // only replication' and mark the first replica as 'full'
            if (file.getXLocList() != null) {
                XLocList xLoc = file.getXLocList();
                XLoc[] replicas = new XLoc[xLoc.getReplicaCount()];
                for (int i = 0; i < replicas.length; i++)
                    replicas[i] = xLoc.getReplica(i);
                
                replicas[0].setReplicationFlags(ReplicationFlags.setFullReplica(ReplicationFlags
                        .setReplicaIsComplete(replicas[0].getReplicationFlags())));
                
                XLocList newXLoc = sMan.createXLocList(replicas, readOnly ? ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY
                        : ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE, xLoc.getVersion() + 1);
                file.setXLocList(newXLoc);
                sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
            }
            
            // set the read-only flag
            file.setReadOnly(readOnly);
            sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
            
            break;
        
        case snapshots:
            
            if (!file.isDirectory())
                throw new UserException(POSIXErrno.POSIX_ERROR_ENOTDIR,
                        "snapshots of single files are not allowed so far");
            
            // value format: "c|cr|d| name"
            
            // TODO: restrict to admin users
            
            int index = value.indexOf(" ");
            
            String command = null;
            String name = null;
            try {
                command = value.substring(0, index);
                name = value.substring(index + 1);
            } catch (Exception exc) {
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "malformed snapshot configuration");
            }
            
            // create snapshot
            if (command.charAt(0) == 'c')
                vMan.createSnapshot(sMan.getVolumeInfo().getId(), name, parentId, file, command.equals("cr"));
            
            // delete snapshot
            else if (command.equals("d"))
                vMan.deleteSnapshot(sMan.getVolumeInfo().getId(), file, name);
            
            else
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "invalid snapshot command: " + value);
            
            break;
        
        case snapshots_enabled:
            
            if (file.getId() != 1)
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                        "snapshots can only be enabled or disabled on volumes");
            
            boolean enable = Boolean.parseBoolean(value);
            sMan.getVolumeInfo().setAllowSnaps(enable, update);
            
            break;
        
        case mark_replica_complete:
            
            if (file.isDirectory())
                throw new UserException(POSIXErrno.POSIX_ERROR_EISDIR, "file required");
            
            XLocList xlocs = file.getXLocList();
            XLoc[] xlocArray = new XLoc[xlocs.getReplicaCount()];
            Iterator<XLoc> it = xlocs.iterator();
            for (int i = 0; it.hasNext(); i++) {
                XLoc xloc = it.next();
                if (value.equals(xloc.getOSD(0)))
                    xloc.setReplicationFlags(ReplicationFlags.setReplicaIsComplete(xloc.getReplicationFlags()));
                xlocArray[i] = xloc;
            }
            XLocList newXLocList = sMan.createXLocList(xlocArray, xlocs.getReplUpdatePolicy(), xlocs.getVersion());
            file.setXLocList(newXLocList);
            sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
            
            break;
        
        case acl:
            
            // parse the modification command
            index = value.indexOf(" ");
            try {
                command = value.substring(0, index);
                String params = value.substring(index + 1);
                
                // modify an ACL entry
                if (command.equals("m")) {
                    
                    int index2 = params.lastIndexOf(':');
                    
                    String entity = params.substring(0, index2);
                    String rights = params.substring(index2 + 1);
                    
                    Map<String, Object> entries = new HashMap<String, Object>();
                    entries.put(entity, rights);
                    faMan.updateACLEntries(sMan, file, parentId, entries, update);
                }
                
                // remove an ACL entry
                else if (command.equals("x")) {
                    List<Object> entries = new ArrayList<Object>(1);
                    entries.add(params);
                    faMan.removeACLEntries(sMan, file, parentId, entries, update);
                    
                } else
                    throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "invalid ACL modification command: "
                            + command);
                
            } catch (MRCException e) {
                Logging.logError(Logging.LEVEL_ERROR, null, e);
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL);
            } catch (Exception exc) {
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "malformed ACL modification request");
            }
            
            break;
        
        case set_repl_update_policy:
            
            if (file.isDirectory())
                throw new UserException(POSIXErrno.POSIX_ERROR_EISDIR, "file required");
            
            xlocs = file.getXLocList();
            xlocArray = new XLoc[xlocs.getReplicaCount()];
            it = xlocs.iterator();
            for (int i = 0; it.hasNext(); i++)
                xlocArray[i] = it.next();
            
            String replUpdatePolicy = xlocs.getReplUpdatePolicy();
            
            // Check allowed policies.
            if (!ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ.equals(value)
                    && !ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE.equals(value)
                    && !ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE.equals(value)
                    && !ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(value))
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "Invalid replica update policy: " + value);
            
            // WaRa was renamed to WaR1.
            if (ReplicaUpdatePolicies.REPL_UPDATE_PC_WARA.equals(value)) {
                throw new UserException(
                    POSIXErrno.POSIX_ERROR_EINVAL,
                    "Do no longer use the policy WaRa. Instead you're probably looking for the WaR1 policy (write all replicas, read from one)."
                        + value);
            }

            if (ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(replUpdatePolicy) && xlocArray.length > 1)
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                    "changing replica update policies of read-only-replicated files is not allowed");
            
            if (ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE.equals(value)) {
                // if there are more than one replica, report an error
                if (xlocs.getReplicaCount() > 1)
                    throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                        "number of replicas has to be reduced to 1 before replica update policy can be set to "
                            + ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE + " (current replica count = "
                            + xlocs.getReplicaCount() + ")");
            }
            
            // Do not allow to switch between read-only and read/write replication
            // as there are currently no mechanisms in place to guarantee that the replicas are synchronized.
            if ((ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(replUpdatePolicy)
                    && (ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ.equals(value)
                            || ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE.equals(value)))
                ||
                (ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(value)
                        && (ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ.equals(replUpdatePolicy)
                                || ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE.equals(replUpdatePolicy)))) {
                throw new UserException(
                    POSIXErrno.POSIX_ERROR_EINVAL,
                    "Currently, it is not possible to change from a read-only to a read/write replication policy or vise versa.");
            }

            // Update replication policy in new xloc list.
            newXLocList = sMan.createXLocList(xlocArray, value, xlocs.getVersion() + 1);
            
            // mark the first replica in the list as 'complete' (only relevant
            // for read-only replication)
            if (ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(value)) {
                newXLocList.getReplica(0).setReplicationFlags(
                        ReplicationFlags.setFullReplica(ReplicationFlags.setReplicaIsComplete(newXLocList.getReplica(0)
                                .getReplicationFlags())));
                file.setReadOnly(true);
            }

            // check if striping + rw replication would be set
            StripingPolicy stripingPolicy = file.getXLocList().getReplica(0).getStripingPolicy();
            if (stripingPolicy.getWidth() > 1
                    && (value.equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE) || value
                            .equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ))) {
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "RW-replication of striped files is not supported yet.");
            }
            
            // Remove read only state of file if readonly policy gets reverted.
            if (ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(replUpdatePolicy)
                    && ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE.equals(value)) {
                file.setReadOnly(false);
            }
            
            // Write back updated file data.
            file.setXLocList(newXLocList);
            sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
            
            break;
        
        case default_rp:
            
            if (!file.isDirectory())
                throw new UserException(POSIXErrno.POSIX_ERROR_EPERM,
                        "default replication policies can only be set on volumes and directories");
            
            try {
                
                ReplicationPolicy rp = null;
                rp = Converter.jsonStringToReplicationPolicy(value);
                
                if (rp.getFactor() == 1 && !ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE.equals(rp.getName())) {
                    throw new UserException(POSIXErrno.POSIX_ERROR_EPERM,
                            "a default replication policy requires a replication factor >= 2");
                }
                
                // check if rw replication + striping would be set
                if (sMan.getDefaultStripingPolicy(file.getId()).getWidth() > 1
                        && (rp.getName().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE) || rp.getName().equals(
                                ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ))) {
                    throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                            "RW-replication of striped files is not supported yet.");
                }

                sMan.setDefaultReplicationPolicy(file.getId(), rp, update);
                
            } catch (JSONException exc) {
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "invalid default replication policy: " + value);
            } catch (ClassCastException exc) {
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "invalid default replication policy: " + value);
            } catch (NullPointerException exc) {
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "invalid default replication policy: " + value);
            } catch (IllegalArgumentException exc) {
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "invalid default replication policy: " + value);
            }
            
            break;
        
        case quota:
            if (file.getId() != 1)
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "quota must be set on volume root");

            sMan.getVolumeInfo().setVolumeQuota((long) Long.valueOf(value), update);

            break;

        default:
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "system attribute '" + keyString + "' is immutable");
        }
    }
    
    public static List<String> getSpecialAttrNames(StorageManager sMan, String namePrefix) throws DatabaseException {
        
        final String prefix = "xtreemfs." + namePrefix;
        final List<String> result = new LinkedList<String>();
        
        DatabaseResultSet<XAttr> it = sMan.getXAttrs(1, StorageManager.SYSTEM_UID);
        while (it.hasNext()) {
            XAttr attr = it.next();
            if (attr.getKey().startsWith(prefix))
                result.add(attr.getKey());
        }
        it.destroy();
        
        return result;
    }
        
    public static ServiceDataMap.Builder buildServiceDataMap(String... kvPairs) {
        
        assert (kvPairs.length % 2 == 0);
        
        ServiceDataMap.Builder builder = ServiceDataMap.newBuilder();
        for (int i = 0; i < kvPairs.length; i += 2) {
            KeyValuePair.Builder kvp = KeyValuePair.newBuilder();
            kvp.setKey(kvPairs[i]);
            kvp.setValue(kvPairs[i + 1]);
            builder.addData(kvp);
        }
        
        return builder;
    }
    
    private static String getPolicyValue(StorageManager sMan, String keyString) throws DatabaseException {
        byte[] value = sMan.getXAttr(1, StorageManager.SYSTEM_UID, "xtreemfs." + keyString);
        return value == null ? null : new String(value);
    }
    
    private static String getVolAttrValue(StorageManager sMan, String keyString) throws DatabaseException {
        byte[] value = sMan.getXAttr(1, StorageManager.SYSTEM_UID, "xtreemfs." + keyString);
        return value == null ? null : new String(value);
    }
    
    private static void setPolicyValue(StorageManager sMan, String keyString, String value, AtomicDBUpdate update)
            throws DatabaseException, UserException {

        // remove "policies."
        String checkString = keyString.substring(POLICY_ATTR_PREFIX.length()+1);
        int index = checkString.indexOf('.');

        if (index <= 0) {
            // remove trailing "."
            if (keyString.startsWith(".")) {
                keyString = keyString.substring(1);
            }
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM,
                    "'"+keyString+"="+value+" :' " +
                            "XtreemFS no longer supports global policy attributes. " +
                            "It is necessary to specify a policy e.g., '1000."+keyString+"="+value+"'");
        }

        // set the value in the database
        byte[] bytes = value.getBytes();
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, "xtreemfs." + keyString, bytes == null || bytes.length == 0 ? null
                : bytes, update);
    }
}
