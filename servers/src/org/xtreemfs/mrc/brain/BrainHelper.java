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

package org.xtreemfs.mrc.brain;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.speedy.SpeedyRequest;
import org.xtreemfs.foundation.speedy.SpeedyRequest.RequestStatus;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.brain.Brain.SysAttrs;
import org.xtreemfs.mrc.brain.storage.BackendException;
import org.xtreemfs.mrc.brain.storage.StorageManager;
import org.xtreemfs.mrc.brain.storage.entities.ACLEntry;
import org.xtreemfs.mrc.brain.storage.entities.AbstractFileEntity;
import org.xtreemfs.mrc.brain.storage.entities.DirEntity;
import org.xtreemfs.mrc.brain.storage.entities.FileEntity;
import org.xtreemfs.mrc.brain.storage.entities.StripingPolicy;
import org.xtreemfs.mrc.brain.storage.entities.XLocation;
import org.xtreemfs.mrc.brain.storage.entities.XLocationsList;
import org.xtreemfs.mrc.osdselection.OSDStatusManager;
import org.xtreemfs.mrc.slices.SliceManager;
import org.xtreemfs.mrc.slices.VolumeInfo;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.MessageUtils;

/**
 * A helper class used by the backend.
 * 
 * @author stender
 * 
 */
public class BrainHelper {
    
    /**
     * Creates a map containing information about a volume, as required by the
     * directory service when registering a new volume.
     * 
     * @param vol
     * @param osdMan
     * @param mrcUUID
     * @return a map containing volume information
     */
    public static Map<String, Object> createDSVolumeInfo(VolumeInfo vol, OSDStatusManager osdMan,
        String mrcUUID) {
        
        String free = String.valueOf(osdMan.getFreeSpace(vol));
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("name", vol.getName());
        map.put("mrc", mrcUUID);
        map.put("type", "volume");
        map.put("free", free);
        
        return map;
    }
    
    /**
     * Creates a capability for accessing a file.
     * 
     * @param accessMode
     * @param volumeId
     * @param fileId
     * @param epochNo
     * @param sharedSecret
     * @throws UserException
     * @throws BrainException
     */
    public static Capability createCapability(String accessMode, String volumeId, long fileId,
        long epochNo, String sharedSecret) throws UserException, BrainException {
        
        return new Capability(volumeId + ":" + fileId, accessMode, epochNo, sharedSecret);
    }
    
    /**
     * Creates an HTTP headers object containing an X-Capability and X-Locations
     * list entry.
     * 
     * @param capability
     * @param xLocList
     * @return
     * @throws JSONException
     */
    public static HTTPHeaders createXCapHeaders(String capability, XLocationsList xLocList)
        throws JSONException {
        
        HTTPHeaders headers = new HTTPHeaders();
        if (capability != null)
            headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, capability);
        if (xLocList != null)
            headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, JSONParser.writeJSON(Converter
                    .xLocListToList(xLocList)));
        
        return headers;
    }
    
    /**
     * Creates a new X-Locations list
     * 
     * @param xLocList
     *            an existing X-Locations list containing a version number (may
     *            be <tt>null</tt>)
     * @param sMan
     *            the Storage Manager responsible for the file
     * @param osdMan
     *            the OSD Status Manager that periodically reports a list of
     *            usable OSDs
     * @param path
     *            the path to the file
     * @param fileId
     *            the file ID
     * @param parentDirId
     *            the ID of the parent directory
     * @param volume
     *            information about the volume of the file
     * @return an X-Locations list
     * @throws UserException
     * @throws BackendException
     * @throws BrainException
     */
    public static XLocationsList createXLocList(XLocationsList xLocList, StorageManager sMan,
        OSDStatusManager osdMan, Path path, long fileId, long parentDirId, VolumeInfo volume,
        InetSocketAddress clientAddress) throws UserException, BackendException, BrainException {
        
        xLocList = new XLocationsList(new XLocation[0], xLocList != null ? xLocList.getVersion()
            : 0);
        
        // first, try to get the striping policy from the file itself
        StripingPolicy stripingPolicy = sMan.getStripingPolicy(fileId);
        
        // if no such policy exists, try to retrieve it from the parent
        // directory
        if (stripingPolicy == null)
            stripingPolicy = sMan.getStripingPolicy(parentDirId);
        
        // if the parent directory has no default policy, take the one
        // associated with the volume
        if (stripingPolicy == null)
            stripingPolicy = sMan.getVolumeStripingPolicy();
        
        if (stripingPolicy == null)
            throw new UserException("could not open file " + path
                + ": no default striping policy available");
        
        Map<String, Map<String, Object>> osdMaps = (Map<String, Map<String, Object>>) osdMan
                .getUsableOSDs(volume.getId());
        
        if (osdMaps == null || osdMaps.size() == 0)
            throw new BrainException("could not open file " + path + ": no feasible OSDs available");
        
        // determine the actual striping width; if not enough OSDs are
        // available, the width will be limited to the amount of
        // available OSDs
        int width = Math.min((int) stripingPolicy.getWidth(), osdMaps.size());
        stripingPolicy.setWidth(width);
        
        // add the OSDs to the X-Locations list, according to the OSD
        // selection policy
        String[] osds = osdMan.getOSDSelectionPolicy(volume.getOsdPolicyId()).getOSDsForNewFile(
            osdMaps, clientAddress.getAddress(), width, volume.getOsdPolicyArgs());
        
        XLocation xLoc = new XLocation(stripingPolicy, osds);
        xLocList.addReplica(xLoc);
        
        return xLocList;
    }
    
    /**
     * Creates a map containing the result of a 'stat' invocation.
     * 
     * @param faMan
     * @param file
     * @param ref
     * @param volumeId
     * @param userId
     * @param xLocList
     * @param xAttrs
     * @param acl
     * @return
     * @throws UserException
     * @throws BrainException
     */
    public static Map<String, Object> createStatInfo(FileAccessManager faMan,
        AbstractFileEntity file, String ref, String volumeId, String userId, List<String> groupIds,
        XLocationsList xLocList, Map<String, Object> xAttrs, ACLEntry[] acl) throws UserException,
        BrainException {
        
        FileEntity fileAsFile = file.isDirectory() ? null : (FileEntity) file;
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("fileId", volumeId + ":" + file.getId());
        map.put("objType", ref != null ? 3 : file.isDirectory() ? 2 : 1);
        map.put("ownerId", file.getUserId());
        map.put("groupId", file.getGroupId());
        map.put("size", ref != null ? ref.length() : file.isDirectory() ? 0 : fileAsFile.getSize());
        map.put("epoch", file.isDirectory() ? 0 : fileAsFile.getEpoch());
        map.put("atime", file.getAtime());
        map.put("ctime", file.getCtime());
        map.put("mtime", file.getMtime());
        map.put("linkCount", file.getLinkCount());
        
        if (ref != null)
            map.put("linkTarget", ref);
        if (xLocList != null)
            map.put("replicas", Converter.xLocListToList(xLocList));
        if (xAttrs != null)
            map.put("xAttrs", xAttrs);
        if (acl != null)
            map.put("acl", Converter.aclToMap(acl));
        
        map.put("posixAccessMode", faMan.getPosixAccessMode(volumeId, file.getId(), userId,
            groupIds));
        
        return map;
    }
    
    public static void submitRequest(Brain brain, MRCRequest req, InetSocketAddress endpoint,
        String cmd, Object args) {
        submitRequest(brain, req, endpoint, cmd, args, null);
    }
    
    public static void submitRequest(Brain brain, MRCRequest req, InetSocketAddress endpoint,
        String cmd, Object args, String authString) {
        
        try {
            if (authString == null)
                authString = req.getPinkyRequest().requestHeaders
                        .getHeader(HTTPHeaders.HDR_AUTHORIZATION);
            
            ReusableBuffer body = ReusableBuffer.wrap(JSONParser.writeJSON(args).getBytes(
                HTTPUtils.ENC_UTF8));
            req.sr = new SpeedyRequest("GET", cmd, null, authString, body, HTTPUtils.DATA_TYPE.JSON);
            req.srEndpoint = endpoint;
            brain.notifyRequestListener(req);
            
        } catch (Exception exc) {
            MessageUtils.marshallException(req, new BrainException("could not send request to '"
                + endpoint + "'", exc));
            brain.notifyRequestListener(req);
        }
    }
    
    public static Object parseResponse(SpeedyRequest theRequest) throws Exception {
        
        assert (theRequest != null);
        
        try {
            String body = null;
            
            if (theRequest.status == RequestStatus.FAILED)
                throw new BrainException("sending request failed", null);
            
            if (theRequest.responseBody != null) {
                byte bdy[] = null;
                if (theRequest.responseBody.hasArray()) {
                    bdy = theRequest.responseBody.array();
                } else {
                    bdy = new byte[theRequest.responseBody.capacity()];
                    theRequest.responseBody.position(0);
                    theRequest.responseBody.get(bdy);
                }
                
                body = new String(bdy, "utf-8");
                if (body.startsWith("{\"exception\":")) {
                    Map<String, Object> map = (Map) JSONParser.parseJSON(new JSONString(body));
                    throw new RemoteException(map.get("exception").toString());
                } else {
                    return body == null ? null : JSONParser.parseJSON(new JSONString(body));
                }
            }
            
        } finally {
            theRequest.freeBuffer();
        }
        
        return null;
    }
    
    public static void updateFileSize(StorageManager sMan, boolean updateATime, String volumeId,
        long fileId, String newFileSizeString) throws UserException, BackendException {
        
        try {
            
            FileEntity fileEntity = (FileEntity) sMan.getFileEntity(fileId);
            if (fileEntity == null)
                throw new UserException(ErrNo.ENOENT, "file '" + fileId + "' does not exist");
            
            int index = newFileSizeString.indexOf(',');
            if (index == -1)
                throw new UserException(ErrNo.EINVAL, "invalid " + HTTPHeaders.HDR_XNEWFILESIZE
                    + " header");
            
            // parse the file size and epoch number
            long newFileSize = Long.parseLong(newFileSizeString.substring(1, index));
            long epochNo = Long.parseLong(newFileSizeString.substring(index + 1, newFileSizeString
                    .length() - 1));
            
            // discard outdated file size updates
            if (epochNo < fileEntity.getEpoch())
                return;
            
            // accept any file size in a new epoch but only larger file sizes in
            // the current epoch
            if (epochNo > fileEntity.getEpoch() || newFileSize > fileEntity.getSize()) {
                
                sMan.setFileSize(fileId, newFileSize, epochNo, fileEntity.getIssuedEpoch());
                
                // update POSIX timestamps
                sMan.updateFileTimes(fileId, updateATime, false, true);
            }
            
        } catch (NumberFormatException exc) {
            throw new UserException("invalid file size: " + newFileSizeString);
        } catch (ClassCastException exc) {
            throw new UserException("file ID " + fileId + " refers to a directory");
        }
    }
    
    public static String getSysAttrValue(MRCConfig config, StorageManager sMan,
        OSDStatusManager osdMan, VolumeInfo volume, Path p, AbstractFileEntity file,
        String keyString) throws JSONException, BackendException, UnknownUUIDException {
        
        SysAttrs key = null;
        try {
            key = SysAttrs.valueOf(keyString);
        } catch (IllegalArgumentException exc) {
            // ignore, will be handled by the 'default' case
        }
        
        switch (key) {
        
        case locations:
            return file instanceof FileEntity ? JSONParser.writeJSON(Converter
                    .xLocListToList(((FileEntity) file).getXLocationsList())) : "";
        case file_id:
            return volume.getId() + ":" + file.getId();
        case object_type:
            String ref = sMan.getFileReference(file.getId());
            return ref != null ? "3" : file.isDirectory() ? "2" : "1";
        case url:
            return "uuid:" + config.getUUID().toString() + "/" + p.getVolumeName() + "/"
                + p.getPathWithoutVolume();
        case owner:
            return file.getUserId();
        case group:
            return file.getGroupId();
        case default_sp:
            if (!(file instanceof DirEntity))
                return "";
            StripingPolicy sp = sMan.getStripingPolicy(file.getId());
            if (sp == null)
                return "";
            return sp.toString();
        case ac_policy_id:
            return file.getId() == 1 ? volume.getAcPolicyId() + "" : "";
        case osdsel_policy_id:
            return file.getId() == 1 ? volume.getOsdPolicyId() + "" : "";
        case osdsel_policy_args:
            return file.getId() == 1 ? (volume.getOsdPolicyArgs() == null ? "" : volume
                    .getOsdPolicyArgs()) : "";
        case read_only:
            if (!(file instanceof FileEntity))
                return String.valueOf(false);
            
            return String.valueOf(sMan.isReadOnly(file.getId()));
        case free_space:
            return file.getId() == 1 ? String.valueOf(osdMan.getFreeSpace(volume)) : "";
        }
        
        return "";
    }
    
    public static void setSysAttrValue(StorageManager sMan, SliceManager sliceMan,
        VolumeInfo volume, AbstractFileEntity file, String keyString, String value)
        throws UserException, BackendException, IOException {
        
        SysAttrs key = null;
        try {
            key = SysAttrs.valueOf(keyString);
        } catch (IllegalArgumentException exc) {
            // ignore, will be handled by the 'default' case
        }
        
        switch (key) {
        
        case locations:

            // explicitly setting X-Locations lists is only permitted for files
            // that haven't yet been assigned an X-Locations list!
            if (((FileEntity) file).getXLocationsList() != null)
                throw new UserException(ErrNo.EPERM,
                    "cannot set X-Locations: OSDs have been assigned already");
            
            try {
                // parse the X-Locations list, ensure that it is correctly
                // formatted and consistent
                XLocationsList newXLoc = Converter.listToXLocList((List<Object>) JSONParser
                        .parseJSON(new JSONString(value)));
                
                if (!BrainHelper.isConsistent(newXLoc))
                    throw new UserException(ErrNo.EINVAL, "inconsistent X-Locations list:"
                        + "at least one OSD occurs more than once");
                
                sMan.setXLocationsList(file.getId(), newXLoc);
                
            } catch (JSONException exc) {
                throw new UserException(ErrNo.EINVAL, "invalid X-Locations-List: " + value);
            }
            
            break;
        
        case default_sp:

            if (!file.isDirectory())
                throw new UserException(ErrNo.EPERM,
                    "default striping policies can only be set on volumes and directories");
            
            try {
                Map<String, Object> sp = null;
                if (!value.equals("null")) {
                    StringTokenizer st = new StringTokenizer(value, ", \t");
                    sp = new HashMap<String, Object>();
                    sp.put("policy", st.nextToken());
                    sp.put("stripe-size", Long.parseLong(st.nextToken()));
                    sp.put("width", Long.parseLong(st.nextToken()));
                }
                
                if (file.getId() == 1 && sp == null)
                    throw new UserException(ErrNo.EPERM,
                        "cannot remove volume default striping policy");
                
                sMan.setStripingPolicy(file.getId(), sp);
            } catch (NumberFormatException exc) {
                throw new UserException(ErrNo.EINVAL, "invalid default striping policy: " + value);
            }
            
            break;
        
        case osdsel_policy_id:

            if (file.getId() != 1)
                throw new UserException(ErrNo.EINVAL,
                    "OSD selection policies can only be set on volumes");
            
            try {
                long newPol = Long.parseLong(value);
                volume.setOsdPolicyId(newPol);
                sliceMan.notifyVolumeChangeListeners(VolumeChangeListener.MOD_CHANGED, volume);
                
            } catch (NumberFormatException exc) {
                throw new UserException(ErrNo.EINVAL, "invalid OSD selection policy: " + value);
            }
            
            break;
        
        case osdsel_policy_args:

            if (file.getId() != 1)
                throw new UserException(ErrNo.EINVAL,
                    "OSD selection policies can only be set and configured on volumes");
            
            volume.setOsdPolicyArgs(value);
            sliceMan.notifyVolumeChangeListeners(VolumeChangeListener.MOD_CHANGED, volume);
            
            break;
        
        case read_only:

            if (!(file instanceof FileEntity))
                throw new UserException(ErrNo.EPERM, "only files can be made read-only");
            
            boolean readOnly = Boolean.valueOf(value);
            
            FileEntity fileAsFile = (FileEntity) file;
            if (!readOnly && fileAsFile.getXLocationsList().getReplicas().length > 1)
                throw new UserException(ErrNo.EPERM,
                    "read-only flag cannot be removed from files with multiple replicas");
            
            sMan.setReadOnly(file.getId(), readOnly);
            
            break;
        
        default:
            throw new UserException(ErrNo.EINVAL, "system attribute '" + key
                + "' unknown or immutable");
        }
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
    public static boolean isAddable(XLocationsList xLocList, List<Object> newOSDs) {
        if (xLocList != null)
            for (XLocation loc : xLocList.getReplicas())
                for (String osd : loc.getOsdList())
                    for (Object newOsd : newOSDs)
                        if (osd.equals(newOsd))
                            return false;
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
    public static boolean isConsistent(XLocationsList xLocList) {
        Set<String> tmp = new HashSet<String>();
        if (xLocList != null) {
            for (XLocation loc : xLocList.getReplicas())
                for (String osd : loc.getOsdList())
                    if (!tmp.contains(osd))
                        tmp.add(osd);
                    else
                        return false;
        }
        
        return true;
    }
    
}
