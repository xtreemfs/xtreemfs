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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.VersionManagement;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.dir.DIRClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.PolicyContainer;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.ac.YesToAnyoneFileAccessPolicy;
import org.xtreemfs.mrc.brain.storage.BackendException;
import org.xtreemfs.mrc.brain.storage.SliceID;
import org.xtreemfs.mrc.brain.storage.StorageManager;
import org.xtreemfs.mrc.brain.storage.entities.ACLEntry;
import org.xtreemfs.mrc.brain.storage.entities.AbstractFileEntity;
import org.xtreemfs.mrc.brain.storage.entities.DirEntity;
import org.xtreemfs.mrc.brain.storage.entities.FileEntity;
import org.xtreemfs.mrc.brain.storage.entities.StripingPolicy;
import org.xtreemfs.mrc.brain.storage.entities.XLocation;
import org.xtreemfs.mrc.brain.storage.entities.XLocationsList;
import org.xtreemfs.mrc.osdselection.OSDStatusManager;
import org.xtreemfs.mrc.osdselection.RandomSelectionPolicy;
import org.xtreemfs.mrc.slices.DefaultPartitioningPolicy;
import org.xtreemfs.mrc.slices.SliceInfo;
import org.xtreemfs.mrc.slices.SliceManager;
import org.xtreemfs.mrc.slices.VolumeInfo;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.MessageUtils;

/**
 * An implementation of the MRC interface.
 * 
 * @author stender
 * 
 */
public class Brain {
    
    protected enum AccessMode {
        r, w, x, a, ga, c, t, sr, d
    }
    
    protected enum SysAttrs {
            locations,
            file_id,
            object_type,
            url,
            owner,
            group,
            default_sp,
            ac_policy_id,
            osdsel_policy_id,
            osdsel_policy_args,
            read_only,
            free_space
    }
    
    public static final int         FILETYPE_NOTEXIST       = 0;
    
    public static final int         FILETYPE_DIR            = 1;
    
    public static final int         FILETYPE_FILE           = 2;
    
    public static final String      DEFAULT_STRIPING_POLICY = "PLAIN";
    
    private final InetSocketAddress dirService;
    
    private FileAccessManager       faMan;
    
    private OSDStatusManager        osdMan;
    
    private BrainRequestListener    requestListener;
    
    private final SliceManager      sliceMan;
    
    private final DIRClient         dirClient;
    
    private final boolean           updateATime;
    
    private final MRCConfig         config;
    
    private final String            authString;
    
    public Brain(MRCConfig config, DIRClient client, OSDStatusManager osdMan, SliceManager slices,
        PolicyContainer policyContainer, String authString) throws BrainException {
        
        try {
            
            this.dirClient = client;
            this.sliceMan = slices;
            this.osdMan = osdMan;
            this.config = config;
            this.authString = authString;
            
            dirService = config.getDirectoryService();
            updateATime = !config.isNoAtime();
            faMan = new FileAccessManager(sliceMan, policyContainer);
            
        } catch (Exception e) {
            throw new BrainException(e);
        }
    }
    
    public void getProtocolVersion(MRCRequest request, List<Long> proposedVersions)
        throws BrainException {
        
        long result = VersionManagement.getMatchingProtVers(proposedVersions);
        if (result == -1)
            throw new BrainException("No matching protocol version found. Server supports "
                + VersionManagement.getSupportedProtVersAsString());
        
        MessageUtils.marshallResponse(request, result);
        this.notifyRequestListener(request);
    }
    
    public void createVolume(MRCRequest request, String volumeName) throws BrainException,
        UserException {
        
        HashMap<String, Object> simpleSP = new HashMap<String, Object>();
        simpleSP.put("policy", "RAID0");
        simpleSP.put("stripe-size", Long.valueOf(64));
        simpleSP.put("width", Long.valueOf(1));
        
        createVolume(request, volumeName, RandomSelectionPolicy.POLICY_ID, simpleSP,
            YesToAnyoneFileAccessPolicy.POLICY_ID, DefaultPartitioningPolicy.POLICY_ID, null);
    }
    
    public void createVolume(MRCRequest request, String volumeName, long osdSelectionPolicyId,
        Map<String, Object> defaultStripingPolicy, long acPolicyId, long partitioningPolicyId,
        Map<String, Object> acl) throws BrainException, UserException {
        
        // first, check whether the given policies are supported
        
        if (osdMan.getOSDSelectionPolicy(osdSelectionPolicyId) == null)
            throw new UserException(ErrNo.EINVAL, "invalid OSD selection policy ID: "
                + osdSelectionPolicyId);
        
        if (faMan.getFileAccessPolicy(acPolicyId) == null)
            throw new UserException(ErrNo.EINVAL, "invalid file access policy ID: " + acPolicyId);
        
        // in order to allow volume creation in a single-threaded non-blocking
        // manner, it needs to be performed in two steps:
        // * first, the volume is registered with the directory service needs
        // * when registration has been confirmed at the directory service,
        // request processing is continued with createVolumeStep2
        
        try {
            
            String volumeId = SliceManager.generateNewVolumeId();
            VolumeInfo vol = new VolumeInfo(volumeId, volumeName, acPolicyId, osdSelectionPolicyId,
                partitioningPolicyId, true);
            
            request.details.context = new HashMap<String, Object>();
            request.details.context.put("nextMethod", "createVolumeStep2");
            request.details.context.put("volumeInfo", vol);
            request.details.context.put("defaultStripingPolicy", defaultStripingPolicy);
            request.details.context.put("acl", acl);
            request.details.context.put("volumeId", volumeId);
            
            Map<String, Object> queryMap = new HashMap<String, Object>();
            queryMap.put("name", volumeName);
            List<String> attrs = new LinkedList<String>();
            attrs.add("version");
            
            List<Object> args = new LinkedList<Object>();
            args.add(queryMap);
            args.add(attrs);
            
            // check whether a volume with the same name has already been
            // registered at the Directory Service
            BrainHelper.submitRequest(this, request, dirService, "getEntities", args, authString);
            
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void createVolumeStep2(MRCRequest request) throws BrainException {
        
        try {
            
            // check the response; if a volume with the same name has already
            // been registered, throw an exception
            Map<String, Map<String, Object>> response = (Map<String, Map<String, Object>>) BrainHelper
                    .parseResponse(request.sr);
            
            // check if the volume already exists
            if (!response.isEmpty()) {
                
                String uuid = response.keySet().iterator().next();
                throw new UserException(ErrNo.EEXIST, "volume '"
                    + request.details.context.get("volumeName")
                    + "' already exists in Directory Service, id='" + uuid + "'");
            }
            
            // otherwise, register the volume at the Directory Service
            request.details.context.put("nextMethod", "createVolumeStep3");
            
            VolumeInfo vol = (VolumeInfo) request.details.context.get("volumeInfo");
            Map<String, Object> dsVolumeInfo = BrainHelper.createDSVolumeInfo(vol, osdMan, config
                    .getUUID().toString());
            
            List<Object> args = new LinkedList<Object>();
            args.add(vol.getId());
            args.add(dsVolumeInfo);
            args.add(0L);
            
            BrainHelper
                    .submitRequest(this, request, dirService, "registerEntity", args, authString);
            
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void createVolumeStep3(MRCRequest request) throws BrainException {
        
        try {
            
            // check whether an exception has occured; if so, an exception is
            // thrown when trying to parse the response
            BrainHelper.parseResponse(request.sr);
            
            // get arguments from context
            VolumeInfo vol = (VolumeInfo) request.details.context.get("volumeInfo");
            Map<String, Object> defaultStripingPolicy = (Map<String, Object>) request.details.context
                    .get("defaultStripingPolicy");
            Map<String, Object> acl = (Map<String, Object>) request.details.context.get("acl");
            
            // perform the local volume creation
            createVolumeLocally(request, vol.getName(), vol.getId(), vol.getOsdPolicyId(),
                defaultStripingPolicy, vol.getAcPolicyId(), vol.getPartitioningPolicyId(), acl);
            
            // prepare the request for the log replay
            List<Object> args = new ArrayList<Object>(2);
            args.add(vol.getName());
            args.add(vol.getId());
            args.add(vol.getOsdPolicyId());
            args.add(defaultStripingPolicy);
            args.add(vol.getAcPolicyId());
            args.add(vol.getPartitioningPolicyId());
            args.add(acl);
            ReusableBuffer body = ReusableBuffer.wrap(JSONParser.writeJSON(args).getBytes(
                HTTPUtils.ENC_UTF8));
            
            request.getPinkyRequest().setURIAndBody("createVolumeLocally", body);
            request.details.persistentOperation = true;
            
            MessageUtils.marshallResponse(request, null);
            this.notifyRequestListener(request);
            
        } catch (Exception exc) {
            // FIXME: roll back DIR registration
            throw new BrainException(exc);
        }
    }
    
    public void createVolumeLocally(MRCRequest request, String volumeName, String volumeId,
        long osdSelectionPolicyId, Map<String, Object> defaultStripingPolicy, long acPolicyId,
        long partitioningPolicyId, Map<String, Object> acl) throws BrainException, UserException {
        
        try {
            
            // create the volume
            VolumeInfo volume = sliceMan.createVolume(volumeId, volumeName, acPolicyId,
                osdSelectionPolicyId, null, partitioningPolicyId, true, true);
            
            // if no volume ACL has been set, use the default volume ACL
            // returned by the access control manager
            if (acl == null)
                acl = faMan.createDefaultVolumeACL(volume.getId());
            
            StorageManager sMan = sliceMan.getSliceDB(volume.getId(), "/", 'r');
            
            // set slice to ONLINE
            SliceInfo info = sliceMan.getSliceInfo(sMan.getSliceId());
            assert (info != null);
            info.setStatus(SliceInfo.SliceStatus.ONLINE);
            
            // set the volume ACL
            request.details.authorized = true;
            request.details.sliceId = sMan.getSliceId();
            doChangeOwner(request, volumeName, request.details.userId, request.details.groupIds
                    .get(0), false);
            doSetACLEntries(request, volumeName, acl, false);
            doSetStripingPolicy(request, volumeName, defaultStripingPolicy, false);
            
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
        
    }
    
    public void deleteVolume(MRCRequest request, String name) throws BrainException, UserException {
        
        try {
            
            // check whether the volume exists locally
            VolumeInfo volume = sliceMan.getVolumeByName(name);
            
            // check whether privileged permissions are granted for deleting the
            // volume
            faMan.checkPrivilegedPermissions(volume.getId(), 1, request.details.userId,
                request.details.superUser, request.details.groupIds);
            
            request.details.context = new HashMap<String, Object>();
            request.details.context.put("nextMethod", "deleteVolumeStep2");
            request.details.context.put("volume", volume);
            
            List<Object> args = new LinkedList<Object>();
            args.add(volume.getId());
            
            BrainHelper.submitRequest(this, request, dirService, "deregisterEntity", args,
                authString);
            
        } catch (UserException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void deleteVolumeStep2(MRCRequest request) throws BrainException, UserException {
        
        try {
            
            // check whether an exception has occured; if so, an exception is
            // thrown when trying to parse the response
            BrainHelper.parseResponse(request.sr);
            
            VolumeInfo volume = (VolumeInfo) request.details.context.get("volume");
            
            deleteVolumeLocally(request, volume.getName());
            
            request.details.sliceId = volume.getSlices().iterator().next().sliceID;
            request.getPinkyRequest().requestURI = "deleteVolumeLocally";
            request.details.persistentOperation = true;
            
            MessageUtils.marshallResponse(request, null);
            this.notifyRequestListener(request);
            
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void deleteVolumeLocally(MRCRequest request, String volumeName) throws BrainException {
        
        try {
            // delete the volume from the local slice manager
            sliceMan.deleteVolume(volumeName);
            
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
        
    }
    
    public void createDir(MRCRequest request, String dirPath) throws UserException, BrainException {
        createDir(request, dirPath, null, 511L);
    }
    
    public void createDir(MRCRequest request, String dirPath, Map<String, Object> xAttrs, long mode)
        throws UserException, BrainException {
        
        try {
            
            Path p = new Path(dirPath);
            VolumeInfo volume = sliceMan.getVolumeByName(p.getVolumeName());
            StorageManager sMan = sliceMan.getSliceDB(volume.getId(), p.getPathWithoutVolume(),
                request.syncPseudoRequest ? '*' : 'w');
            
            // find the parent directory
            AbstractFileEntity parentDir = sMan.getFileEntity(p.getInnerPath(), true);
            
            if (!request.details.authorized) {
                
                // check whether the parent directory is searchable
                faMan.checkSearchPermission(volume.getId(), p.getInnerPath(),
                    request.details.userId, request.details.superUser, request.details.groupIds);
                
                // check whether the parent directory grants write access
                faMan.checkPermission(FileAccessManager.WRITE_ACCESS, volume.getId(), parentDir
                        .getId(), 0, request.details.userId, request.details.superUser,
                    request.details.groupIds);
            }
            
            if (sMan.fileExists(parentDir.getId(), p.getLastPathComponent()))
                throw new UserException(ErrNo.EEXIST, "file or directory '" + dirPath
                    + "' already exists");
            
            // convert the given access mode to an ACL
            Map<String, Object> acl = faMan.convertToACL(volume.getId(), mode);
            
            // create the metadata object
            long fileId = sMan.createFile(null, request.details.userId, request.details.groupIds
                    .get(0), null, true, acl);
            
            // link the metadata object to the given parent directory
            sMan.linkFile(p.getLastPathComponent(), fileId, parentDir.getId());
            
            // create the extended attributes
            sMan.addXAttributes(fileId, xAttrs);
            
            // update POSIX timestamps of parent directory
            sMan.updateFileTimes(parentDir.getId(), false, true, true);
            
            request.details.sliceId = sMan.getSliceId();
            request.details.persistentOperation = true;
            
            MessageUtils.marshallResponse(request, null);
            this.notifyRequestListener(request);
            
        } catch (UserException exc) {
            throw exc;
        } catch (BrainException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void createFile(MRCRequest request, String filePath) throws UserException,
        BrainException {
        createFile(request, filePath, null, null, 511L, false, null);
    }
    
    public void createFile(MRCRequest request, String filePath, Map<String, Object> xAttrs,
        Map<String, Object> stripingPolicy, long mode) throws UserException, BrainException {
        createFile(request, filePath, xAttrs, stripingPolicy, mode, false, null);
    }
    
    public void createFile(MRCRequest request, String filePath, Map<String, Object> xAttrs,
        Map<String, Object> stripingPolicy, long mode, boolean open) throws UserException,
        BrainException {
        createFile(request, filePath, xAttrs, stripingPolicy, mode, open, null);
    }
    
    public void createFile(MRCRequest request, String filePath, Map<String, Object> xAttrs,
        Map<String, Object> stripingPolicy, long mode, boolean open, List<Object> assignedXLocList)
        throws UserException, BrainException {
        
        try {
            
            Path p = new Path(filePath);
            VolumeInfo volume = sliceMan.getVolumeByName(p.getVolumeName());
            StorageManager sMan = sliceMan.getSliceDB(volume.getId(), p.getPathWithoutVolume(),
                request.syncPseudoRequest ? '*' : 'w');
            
            // find the parent directory
            AbstractFileEntity parentDir = sMan.getFileEntity(p.getInnerPath(), true);
            
            if (!request.details.authorized) {
                
                // check whether the parent directory is searchable
                faMan.checkSearchPermission(volume.getId(), p.getInnerPath(),
                    request.details.userId, request.details.superUser, request.details.groupIds);
                
                // check whether the parent directory grants write access
                faMan.checkPermission(FileAccessManager.WRITE_ACCESS, volume.getId(), parentDir
                        .getId(), 0, request.details.userId, request.details.superUser,
                    request.details.groupIds);
            }
            
            if (p.getPathWithoutVolume().length() == 0
                || sMan.fileExists(parentDir.getId(), p.getLastPathComponent()))
                throw new UserException(ErrNo.EEXIST, "file or directory '" + filePath
                    + "' already exists");
            
            // derive the ACL for the file in accordance with the volume's file
            // access policy
            Map<String, Object> acl = faMan.convertToACL(volume.getId(), mode);
            
            // create the metadata object
            long fileId = sMan.createFile(null, request.details.userId, request.details.groupIds
                    .get(0), stripingPolicy, false, acl);
            
            // link the metadata object to the given parent directory
            sMan.linkFile(p.getLastPathComponent(), fileId, parentDir.getId());
            
            // create the user attributes
            sMan.addXAttributes(fileId, xAttrs);
            
            HTTPHeaders headers = null;
            
            if (open) {
                // create a capability for O_CREAT open calls
                String capability = BrainHelper.createCapability(AccessMode.w.toString(),
                    volume.getId(), fileId, 0, config.getCapabilitySecret()).toString();
                
                XLocationsList xLocList = null;
                if (assignedXLocList == null) {
                    // assign a new list
                    xLocList = BrainHelper.createXLocList(null, sMan, osdMan, p, fileId, parentDir
                            .getId(), volume, request.getPinkyRequest().getClientAddress());
                } else {
                    // log replay, use assigned list
                    xLocList = Converter.listToXLocList(assignedXLocList);
                }
                
                // assign the OSDs
                sMan.setXLocationsList(sMan.getFileEntity(p.getPathWithoutVolume()).getId(),
                    xLocList);
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "assigned xloc list to " + p
                        + ": " + xLocList);
                
                headers = BrainHelper.createXCapHeaders(capability, xLocList);
                
                if (assignedXLocList == null) {
                    // not necessary when in log replay mode!
                    // rewrite body
                    // prepare the request for the log replay
                    List<Object> args = new ArrayList<Object>(5);
                    args.add(filePath);
                    args.add(xAttrs);
                    args.add(stripingPolicy);
                    args.add(mode);
                    args.add(true);
                    args.add(Converter.xLocListToList(xLocList));
                    
                    ReusableBuffer body = ReusableBuffer.wrap(JSONParser.writeJSON(args).getBytes(
                        HTTPUtils.ENC_UTF8));
                    
                    request.getPinkyRequest().setURIAndBody("createFile", body);
                }
            }
            
            // update POSIX timestamps of parent directory
            sMan.updateFileTimes(parentDir.getId(), false, true, true);
            
            request.details.sliceId = sMan.getSliceId();
            request.details.persistentOperation = true;
            
            MessageUtils.marshallResponse(request, null, headers);
            this.notifyRequestListener(request);
            
        } catch (UserException exc) {
            throw exc;
        } catch (BrainException exc) {
            throw exc;
        } catch (Exception exc) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
            throw new BrainException(exc);
        }
    }
    
    public void createSymbolicLink(MRCRequest request, String linkPath, String targetPath)
        throws UserException, BrainException {
        
        try {
            
            Path p = new Path(linkPath);
            VolumeInfo volume = sliceMan.getVolumeByName(p.getVolumeName());
            StorageManager sMan = sliceMan.getSliceDB(volume.getId(), p.getPathWithoutVolume(),
                request.syncPseudoRequest ? '*' : 'w');
            
            // find the parent directory
            AbstractFileEntity parentDir = sMan.getFileEntity(p.getInnerPath(), true);
            
            if (!request.details.authorized) {
                
                // check whether the parent directory is searchable
                faMan.checkSearchPermission(volume.getId(), p.getInnerPath(),
                    request.details.userId, request.details.superUser, request.details.groupIds);
                
                // check whether the parent directory grants write access
                faMan.checkPermission(FileAccessManager.WRITE_ACCESS, volume.getId(), parentDir
                        .getId(), 0, request.details.userId, request.details.superUser,
                    request.details.groupIds);
            }
            
            if (sMan.fileExists(parentDir.getId(), p.getLastPathComponent()))
                throw new UserException(ErrNo.EEXIST, "file '" + linkPath + "' already exists");
            
            // TODO: check whether the target path refers to a file or a
            // directory
            boolean isDirectory = false;
            
            // create an ACL with all permissions for anyone
            Map<String, Object> acl = faMan.convertToACL(volume.getId(), 0777);
            
            // create the metadata object
            long fileId = sMan.createFile(targetPath, request.details.userId,
                request.details.groupIds.get(0), null, isDirectory, acl);
            
            // link the metadata object to the given parent directory
            sMan.linkFile(p.getLastPathComponent(), fileId, parentDir.getId());
            
            // update POSIX timestamps of parent directory
            sMan.updateFileTimes(parentDir.getId(), false, true, true);
            
            request.details.sliceId = sMan.getSliceId();
            request.details.persistentOperation = true;
            
            MessageUtils.marshallResponse(request, null);
            this.notifyRequestListener(request);
            
        } catch (UserException exc) {
            throw exc;
        } catch (BrainException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void createLink(MRCRequest request, String linkPath, String targetPath)
        throws UserException, BrainException {
        
        try {
            
            Path lPath = new Path(linkPath);
            Path tPath = new Path(targetPath);
            
            if (!lPath.getVolumeName().equals(tPath.getVolumeName()))
                throw new UserException(ErrNo.EXDEV,
                    "cannot create hard links across volume boundaries");
            
            VolumeInfo volume = sliceMan.getVolumeByName(lPath.getVolumeName());
            StorageManager sMan = sliceMan.getSliceDB(volume.getId(), lPath.getPathWithoutVolume(),
                request.syncPseudoRequest ? '*' : 'w');
            
            // find the parent directory
            AbstractFileEntity linkParentDir = sMan.getFileEntity(lPath.getInnerPath(), true);
            
            if (!request.details.authorized) {
                
                // check whether the parent directory is searchable
                faMan.checkSearchPermission(volume.getId(), lPath.getInnerPath(),
                    request.details.userId, request.details.superUser, request.details.groupIds);
                
                // check whether the parent directory grants write access
                faMan.checkPermission(FileAccessManager.WRITE_ACCESS, volume.getId(), linkParentDir
                        .getId(), 0, request.details.userId, request.details.superUser,
                    request.details.groupIds);
            }
            
            if (sMan.fileExists(linkParentDir.getId(), lPath.getLastPathComponent()))
                throw new UserException(ErrNo.EEXIST, "file '" + linkPath + "' already exists");
            
            AbstractFileEntity targetParentDir = sMan.getFileEntity(tPath.getInnerPath());
            
            if (!request.details.authorized) {
                // check whether the target's parent directory is searchable
                faMan.checkSearchPermission(volume.getId(), tPath.getInnerPath(),
                    request.details.userId, request.details.superUser, request.details.groupIds);
            }
            
            AbstractFileEntity target = sMan.getChild(tPath.getLastPathComponent(), targetParentDir
                    .getId());
            if (target instanceof DirEntity)
                throw new UserException(ErrNo.EPERM, "no support for links to directories");
            
            if (!request.details.authorized) {
                // check whether the target grants write access
                faMan.checkPermission(FileAccessManager.WRITE_ACCESS, volume.getId(), target
                        .getId(), targetParentDir.getId(), request.details.userId,
                    request.details.superUser, request.details.groupIds);
            }
            
            // create the link
            sMan.linkFile(lPath.getLastPathComponent(), target.getId(), linkParentDir.getId());
            
            // update POSIX timestamps
            sMan.updateFileTimes(linkParentDir.getId(), false, true, true);
            sMan.updateFileTimes(target.getId(), false, true, false);
            
            request.details.sliceId = sMan.getSliceId();
            request.details.persistentOperation = true;
            
            MessageUtils.marshallResponse(request, null);
            this.notifyRequestListener(request);
            
        } catch (UserException exc) {
            throw exc;
        } catch (BrainException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void delete(MRCRequest request, String path) throws UserException, BrainException {
        
        try {
            
            Path p = new Path(path);
            VolumeInfo volume = sliceMan.getVolumeByName(p.getVolumeName());
            StorageManager sMan = sliceMan.getSliceDB(volume.getId(), p.getPathWithoutVolume(),
                request.syncPseudoRequest ? '*' : 'w');
            
            AbstractFileEntity parentDir = sMan.getFileEntity(p.getInnerPath());
            
            if (!request.details.authorized) {
                
                // check whether the parent directory is searchable
                faMan.checkSearchPermission(volume.getId(), p.getInnerPath(),
                    request.details.userId, request.details.superUser, request.details.groupIds);
                
                // check whether the parent directory grants write access
                faMan.checkPermission(FileAccessManager.WRITE_ACCESS, volume.getId(), parentDir
                        .getId(), 0, request.details.userId, request.details.superUser,
                    request.details.groupIds);
            }
            
            AbstractFileEntity file = sMan.getChild(p.getLastPathComponent(), parentDir.getId());
            if (file == null)
                throw new UserException(ErrNo.ENOENT, "file or directory '"
                    + p.getLastPathComponent() + "' does not exist");
            
            if (!request.details.authorized) {
                // check whether the entry itself can be deleted (this is e.g.
                // important w/ POSIX access control if the sticky bit is set)
                faMan.checkPermission(FileAccessManager.RM_MV_IN_DIR_ACCESS, volume.getId(), file
                        .getId(), parentDir.getId(), request.details.userId,
                    request.details.superUser, request.details.groupIds);
            }
            
            if (file.isDirectory() && sMan.hasChildren(file.getId()))
                throw new UserException(ErrNo.ENOTEMPTY, "'" + path + "' is not empty");
            
            HTTPHeaders xCapHeaders = null;
            
            // unless the file is a directory, retrieve X-headers for file
            // deletion on OSDs; if the request was authorized before,
            // assume that a capability has been issued already.
            if (!request.details.authorized && !file.isDirectory()) {
                
                // obtain a deletion capability for the file
                String aMode = faMan.translateAccessMode(volume.getId(),
                    FileAccessManager.DELETE_ACCESS);
                String capability = BrainHelper.createCapability(aMode, volume.getId(),
                    file.getId(), Integer.MAX_VALUE, config.getCapabilitySecret()).toString();
                
                // set the XCapability and XLocationsList headers
                xCapHeaders = BrainHelper.createXCapHeaders(capability, sMan.getXLocationsList(file
                        .getId()));
            }
            
            // unlink the file; if there are still links to the file, reset the
            // X-headers to null, as the file content must not be deleted
            long linkCount = sMan.unlinkFile(p.getLastPathComponent(), file.getId(), parentDir
                    .getId());
            if (linkCount > 0)
                xCapHeaders = null;
            
            // update POSIX timestamps of parent directory
            sMan.updateFileTimes(parentDir.getId(), false, true, true);
            
            request.details.sliceId = sMan.getSliceId();
            request.details.persistentOperation = true;
            
            MessageUtils.marshallResponse(request, null, xCapHeaders);
            this.notifyRequestListener(request);
            
        } catch (UserException exc) {
            throw exc;
        } catch (BrainException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    /**
     * This method has the semantics of the POSIX 'rename' operation. It can be
     * used to rename or move files and directories inside a volume, across
     * volume boundaries or even between MRCs.
     * 
     * In the first step, 'move' checks whether the source path points to an
     * existing file or directory. If this is the case, the method checks
     * whether the target volume is the same as the source volume. If this is
     * not the case, the movement operation itself will be executed by the
     * 'interVolumeMove' method.
     * 
     * Note that inter-volume moves may be fairly expensive if large directory
     * structures are moved, since the entire directory subtree has to be
     * transferred to and restored on the target site.
     * 
     * @param request
     * @param sourcePath
     * @param targetPath
     * @throws UserException
     * @throws BrainException
     */
    public void move(MRCRequest request, String sourcePath, String targetPath)
        throws UserException, BrainException {
        
        try {
            
            Path sPath = new Path(sourcePath);
            VolumeInfo volume = sliceMan.getVolumeByName(sPath.getVolumeName());
            StorageManager sMan = sliceMan.getSliceDB(volume.getId(), sPath.getPathWithoutVolume(),
                'w');
            
            request.details.sliceId = sMan.getSliceId();
            request.details.persistentOperation = true;
            
            Path tPath = new Path(targetPath);
            
            // find out what the source path refers to (1 = directory, 2 = file)
            AbstractFileEntity sourceParentDir = sMan.getFileEntity(sPath.getInnerPath());
            
            if (sPath.getLastPathComponent() == null)
                throw new UserException(ErrNo.ENOENT, "cannot move a volume");
            
            if (!tPath.getVolumeName().equals(sPath.getVolumeName()))
                throw new UserException(ErrNo.ENOENT, "cannot move between volumes");
            
            if (!request.details.authorized) {
                
                // check whether the parent directory of the source file is
                // searchable
                faMan.checkSearchPermission(volume.getId(), sPath.getInnerPath(),
                    request.details.userId, request.details.superUser, request.details.groupIds);
                
                // check whether the parent directory of the source file grants
                // write access
                faMan.checkPermission(FileAccessManager.WRITE_ACCESS, volume.getId(),
                    sourceParentDir.getId(), 0, request.details.userId, request.details.superUser,
                    request.details.groupIds);
            }
            
            AbstractFileEntity source = sMan.getChild(sPath.getLastPathComponent(), sourceParentDir
                    .getId());
            if (source == null)
                throw new UserException(ErrNo.ENOENT, "file or directory '"
                    + sPath.getLastPathComponent() + "' does not exist");
            
            if (!request.details.authorized) {
                // check whether the entry itself can be moved (this is e.g.
                // important w/ POSIX access control if the sticky bit is set)
                faMan.checkPermission(FileAccessManager.RM_MV_IN_DIR_ACCESS, volume.getId(), source
                        .getId(), sourceParentDir.getId(), request.details.userId,
                    request.details.superUser, request.details.groupIds);
            }
            
            int sourceType = source.isDirectory() ? FILETYPE_DIR : FILETYPE_FILE;
            
            // if the target path refers to a different volume, perform an
            // inter-volume move
            // if (!tPath.getVolumeName().equals(sPath.getVolumeName())) {
            // interVolumeMove(request, sMan, source, tPath, sliceMan
            // .hasVolume(tPath.getVolumeName()));
            // return;
            // }
            
            // find out what the target path refers to (0 = does not exist, 1 =
            // directory, 2 = file)
            AbstractFileEntity targetParentDir = sMan.getFileEntity(tPath.getInnerPath(), true);
            AbstractFileEntity tChild = sMan.getChild(tPath.getLastPathComponent(), targetParentDir
                    .getId());
            int targetType = tPath.getPathWithoutVolume().length() == 0 ? FILETYPE_DIR
                : tChild == null ? FILETYPE_NOTEXIST : tChild.isDirectory() ? FILETYPE_DIR
                    : FILETYPE_FILE;
            
            // if both the old and the new directory point to the same
            // entity, do nothing
            if (sPath.toString().equals(tPath.toString())) {
                MessageUtils.marshallResponse(request, null);
                this.notifyRequestListener(request);
                return;
            }
            
            if (!request.details.authorized) {
                
                // check whether the parent directory of the target file is
                // searchable
                faMan.checkSearchPermission(volume.getId(), tPath.getInnerPath(),
                    request.details.userId, request.details.superUser, request.details.groupIds);
                
                // check whether the parent directory of the target file
                // grants write access
                faMan.checkPermission(FileAccessManager.WRITE_ACCESS, volume.getId(),
                    targetParentDir.getId(), 0, request.details.userId, request.details.superUser,
                    request.details.groupIds);
            }
            
            HTTPHeaders xCapHeaders = null;
            
            // if the source is a directory
            if (sourceType == FILETYPE_DIR) {
                
                // check whether the target is a subdirectory of the
                // source directory; if so, throw an exception
                if (targetPath.startsWith(sourcePath + "/"))
                    throw new UserException(ErrNo.EINVAL, "cannot move '" + sourcePath
                        + "' to one of its own subdirectories");
                
                switch (targetType) {
                
                case FILETYPE_NOTEXIST: // target does not exist
                {
                    // relink the metadata object to the parent directory of
                    // the target path and remove the former link
                    sMan.linkFile(tPath.getLastPathComponent(), source.getId(), targetParentDir
                            .getId());
                    sMan.unlinkFile(sPath.getLastPathComponent(), source.getId(), sourceParentDir
                            .getId());
                    
                    break;
                }
                    
                case FILETYPE_DIR: // target is a directory
                {
                    // unlink the target directory if existing
                    
                    if (!request.details.authorized) {
                        // check whether the target directory may be overwritten
                        faMan.checkPermission(FileAccessManager.DELETE_ACCESS, volume.getId(),
                            tChild.getId(), targetParentDir.getId(), request.details.userId,
                            request.details.superUser, request.details.groupIds);
                    }
                    
                    if (sMan.hasChildren(tChild.getId()))
                        throw new UserException(ErrNo.ENOTEMPTY, "target directory '" + targetPath
                            + "' is not empty");
                    else
                        sMan.unlinkFile(tPath.getLastPathComponent(), tChild.getId(),
                            targetParentDir.getId());
                    
                    // relink the metadata object to the parent directory of
                    // the target path and remove the former link
                    sMan.linkFile(tPath.getLastPathComponent(), source.getId(), targetParentDir
                            .getId());
                    sMan.unlinkFile(sPath.getLastPathComponent(), source.getId(), sourceParentDir
                            .getId());
                    
                    break;
                }
                    
                case FILETYPE_FILE: // target is a file
                    throw new UserException(ErrNo.ENOTDIR, "cannot rename directory '" + sourcePath
                        + "' to file '" + targetPath + "'");
                    
                }
                
            }

            // if the source is a file
            else {
                
                switch (targetType) {
                
                case FILETYPE_NOTEXIST: // target does not exist
                {
                    
                    // relink the metadata object to the parent directory of
                    // the target path and remove the former link
                    sMan.linkFile(tPath.getLastPathComponent(), source.getId(), targetParentDir
                            .getId());
                    sMan.unlinkFile(sPath.getLastPathComponent(), source.getId(), sourceParentDir
                            .getId());
                    
                    break;
                }
                    
                case FILETYPE_DIR: // target is a directory
                {
                    throw new UserException(ErrNo.EISDIR, "cannot rename file '" + sourcePath
                        + "' to directory '" + targetPath + "'");
                }
                    
                case FILETYPE_FILE: // target is a file
                {
                    
                    if (!request.details.authorized) {
                        
                        // obtain a deletion capability for the file
                        String aMode = faMan.translateAccessMode(volume.getId(),
                            FileAccessManager.DELETE_ACCESS);
                        String capability = BrainHelper.createCapability(aMode, volume.getId(),
                            tChild.getId(), Integer.MAX_VALUE, config.getCapabilitySecret())
                                .toString();
                        
                        // set the XCapability and XLocationsList headers
                        xCapHeaders = BrainHelper.createXCapHeaders(capability, sMan
                                .getXLocationsList(tChild.getId()));
                    }
                    
                    // unlink the target file
                    long linkCount = sMan.unlinkFile(tPath.getLastPathComponent(), tChild.getId(),
                        targetParentDir.getId());
                    
                    // reset the x-header to null if there is still another link
                    // to the metadata object, i.e. the metadata object must not
                    // be deleted yet
                    if (linkCount > 0)
                        xCapHeaders = null;
                    
                    // relink the metadata object to the parent directory of
                    // the target path and remove the former link
                    sMan.linkFile(tPath.getLastPathComponent(), source.getId(), targetParentDir
                            .getId());
                    sMan.unlinkFile(sPath.getLastPathComponent(), source.getId(), sourceParentDir
                            .getId());
                    
                    break;
                }
                    
                }
            }
            
            // update POSIX timestamps of parent directories
            sMan.updateFileTimes(sourceParentDir.getId(), false, true, true);
            sMan.updateFileTimes(targetParentDir.getId(), false, true, true);
            
            MessageUtils.marshallResponse(request, null, xCapHeaders);
            this.notifyRequestListener(request);
            
        } catch (UserException exc) {
            throw exc;
        } catch (BrainException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    /**
     * Restore the MetaData for the given fileID.
     * 
     * @param request
     * @param filePath
     * @param fileNumber
     * @param fileSize
     * @param xAttrs
     * @param osd
     * @param objectSize
     * @param volumeID
     * 
     * @throws UserException
     * @throws BrainException
     */
    public void restoreFile(MRCRequest request, String filePath, long fileNumber, long fileSize,
        Map<String, Object> xAttrs, String osd, long objectSize, String volumeID) throws UserException,
        BrainException {
        try {
            VolumeInfo volume = getVolumeData(volumeID);
            String path = volume.getName()+"/"+filePath;
            Path p = new Path(path);
            
            StorageManager sMan = sliceMan.getSliceDB(volumeID, p.getPathWithoutVolume(),
                request.syncPseudoRequest ? '*' : 'w');
            
            // find the parent directory
            AbstractFileEntity parentDir = sMan.getFileEntity("/", true);
            
            // derive the ACL for the file in accordance with the volume's file
            // access policy
            Map<String, Object> acl = faMan.convertToACL(volumeID, 511L);
            
            if (!request.details.authorized) {
                
                // check whether the parent directory is searchable
                faMan.checkSearchPermission(volumeID, "/",
                    request.details.userId, request.details.superUser, request.details.groupIds);
                
                // check whether the parent directory grants write access
                faMan.checkPermission(FileAccessManager.WRITE_ACCESS, volumeID, parentDir
                        .getId(), 0, request.details.userId, request.details.superUser,
                    request.details.groupIds);
            }
            
            long lostFoundID = 0L;
            try{
                lostFoundID = sMan.getFileEntity(filePath).getId();
            }catch (UserException ue){
                // create lost and found DIR, if necessary
                lostFoundID = sMan.createFile(null, request.details.userId, request.details.groupIds
                        .get(0), null, true, acl);
                
                // link the metadata object to the given parent directory
                sMan.linkFile(filePath, lostFoundID, parentDir.getId());
            }
            
            long size = (objectSize<1024L ? 1L : (objectSize % 1024L != 0L) ? objectSize/1024L+1L : objectSize/1024L);
            
            // make a new xlocl
            XLocationsList xloc = new XLocationsList(new XLocation[] { new XLocation(
                new StripingPolicy("RAID0", size, 1L), (new String[] { osd })) }, 0L);
            
            // generate the metadata
            long time = System.currentTimeMillis();
            AbstractFileEntity file = new FileEntity(fileNumber, request.details.userId,
                request.details.groupIds.get(0), time, time, time, fileSize, xloc, Converter
                        .mapToACL(acl), 0L, 0L, 0L);
            
            // create the metadata object
            sMan.createFile(file, null);
            
            // link the metadata object to the given parent directory
            sMan.linkFile(volumeID + ":" + fileNumber, fileNumber, lostFoundID);
            
            // create the user attributes
            sMan.addXAttributes(fileNumber, xAttrs);
            
            // log entry
            request.details.sliceId = sMan.getSliceId();
            request.details.persistentOperation = true;
            
            // return
            MessageUtils.marshallResponse(request, null);
            this.notifyRequestListener(request);
            
        } catch (BackendException e) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, e);
            throw new BrainException(e);
        }
    }
    
    public void stat(MRCRequest request, String path, boolean inclReplicas, boolean inclXAttrs,
        boolean inclACLs) throws UserException, BrainException {
        
        try {
            
            Path p = new Path(path);
            VolumeInfo volume = sliceMan.getVolumeByName(p.getVolumeName());
            StorageManager sMan = sliceMan
                    .getSliceDB(volume.getId(), p.getPathWithoutVolume(), 'r');
            
            AbstractFileEntity parentDir = sMan.getFileEntity(p.getInnerPath());
            
            // check whether the parent directory of the file grants search
            // access
            if (p.getLastPathComponent().length() != 0)
                faMan.checkSearchPermission(volume.getId(), p.getInnerPath(),
                    request.details.userId, request.details.superUser, request.details.groupIds);
            
            AbstractFileEntity file = sMan.getChild(p.getLastPathComponent(), parentDir.getId());
            if (file == null)
                throw new UserException(ErrNo.ENOENT, "file or directory '"
                    + p.getLastPathComponent() + "' does not exist");
            
            String ref = sMan.getFileReference(file.getId());
            
            XLocationsList xLocList = file instanceof FileEntity && inclReplicas ? ((FileEntity) file)
                    .getXLocationsList()
                : null;
            
            Map<String, Object> xAttrs = null;
            if (inclXAttrs) {
                xAttrs = sMan.getXAttributes(file.getId());
                if (xAttrs == null)
                    xAttrs = new HashMap<String, Object>();
                for (SysAttrs attr : SysAttrs.values()) {
                    String key = "xtreemfs." + attr.toString();
                    Object value = BrainHelper.getSysAttrValue(config, sMan, osdMan, volume, p,
                        file, attr.toString());
                    if (!value.equals(""))
                        xAttrs.put(key, value);
                }
            }
            
            ACLEntry[] acl = inclACLs ? file.getAcl() : null;
            
            Object statInfo = BrainHelper.createStatInfo(faMan, file, ref, volume.getId(),
                request.details.userId, request.details.groupIds, xLocList, xAttrs, acl);
            
            MessageUtils.marshallResponse(request, statInfo);
            this.notifyRequestListener(request);
            
        } catch (UserException exc) {
            throw exc;
        } catch (BrainException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void getXAttr(MRCRequest request, String path, String attrKey) throws UserException,
        BrainException {
        
        try {
            
            Path p = new Path(path);
            VolumeInfo volume = sliceMan.getVolumeByName(p.getVolumeName());
            StorageManager sMan = sliceMan
                    .getSliceDB(volume.getId(), p.getPathWithoutVolume(), 'r');
            
            AbstractFileEntity parentDir = sMan.getFileEntity(p.getInnerPath());
            
            // check whether the parent directory of the file grants search
            // access
            if (p.getLastPathComponent().length() != 0)
                faMan.checkSearchPermission(volume.getId(), p.getInnerPath(),
                    request.details.userId, request.details.superUser, request.details.groupIds);
            
            AbstractFileEntity file = sMan.getChild(p.getLastPathComponent(), parentDir.getId());
            if (file == null)
                throw new UserException(ErrNo.ENOENT, "file or directory '"
                    + p.getLastPathComponent() + "' does not exist");
            
            String value = null;
            
            if (attrKey.startsWith("xtreemfs."))
                value = BrainHelper.getSysAttrValue(config, sMan, osdMan, volume, p, file, attrKey
                        .substring(9));
            else {
                value = String.valueOf(sMan.getXAttributes(file.getId()).get(attrKey));
            }
            
            if (value == null)
                value = "";
            
            MessageUtils.marshallResponse(request, value);
            this.notifyRequestListener(request);
            
        } catch (UserException exc) {
            throw exc;
        } catch (BrainException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
        
    }
    
    public void readDir(MRCRequest request, String path) throws UserException, BrainException {
        
        try {
            
            Path p = new Path(path);
            VolumeInfo volume = sliceMan.getVolumeByName(p.getVolumeName());
            StorageManager sMan = sliceMan
                    .getSliceDB(volume.getId(), p.getPathWithoutVolume(), 'r');
            
            AbstractFileEntity parentDir = sMan.getFileEntity(p.getInnerPath(), true);
            
            // check whether the parent directory is searchable
            if (p.getLastPathComponent().length() != 0)
                faMan.checkSearchPermission(volume.getId(), p.getInnerPath(),
                    request.details.userId, request.details.superUser, request.details.groupIds);
            
            AbstractFileEntity dir = sMan.getChild(p.getLastPathComponent(), parentDir.getId());
            if (dir == null)
                throw new UserException(ErrNo.ENOENT, "file or directory '"
                    + p.getLastPathComponent() + "' does not exist");
            
            // check whether the directory grants read access
            faMan.checkPermission(FileAccessManager.READ_ACCESS, volume.getId(), dir.getId(),
                parentDir.getId(), request.details.userId, request.details.superUser,
                request.details.groupIds);
            
            // update POSIX timestamps
            if (updateATime)
                sMan.updateFileTimes(dir.getId(), true, false, false);
            
            MessageUtils.marshallResponse(request, sMan.getChildren(dir.getId()));
            this.notifyRequestListener(request);
            
        } catch (UserException exc) {
            throw exc;
        } catch (BrainException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void readDirAndStat(MRCRequest request, String path) throws UserException,
        BrainException {
        
        try {
            
            Path p = new Path(path);
            VolumeInfo volume = sliceMan.getVolumeByName(p.getVolumeName());
            StorageManager sMan = sliceMan
                    .getSliceDB(volume.getId(), p.getPathWithoutVolume(), 'r');
            
            AbstractFileEntity parentDir = sMan.getFileEntity(p.getInnerPath(), true);
            
            // check whether the directory is searchable
            if (p.getLastPathComponent().length() != 0)
                faMan.checkSearchPermission(volume.getId(), p.getInnerPath(),
                    request.details.userId, request.details.superUser, request.details.groupIds);
            
            AbstractFileEntity dir = sMan.getChild(p.getLastPathComponent(), parentDir.getId());
            if (dir == null)
                throw new UserException(ErrNo.ENOENT, "file or directory '"
                    + p.getLastPathComponent() + "' does not exist");
            
            // check whether the directory grants read access
            faMan.checkPermission(FileAccessManager.READ_ACCESS, volume.getId(), dir.getId(),
                parentDir.getId(), request.details.userId, request.details.superUser,
                request.details.groupIds);
            
            Map<String, AbstractFileEntity> fileData = sMan.getChildData(dir.getId());
            
            Map<String, Map<String, Object>> result = new HashMap<String, Map<String, Object>>();
            for (String name : fileData.keySet()) {
                AbstractFileEntity data = fileData.get(name);
                String ref = sMan.getFileReference(data.getId());
                result.put(name, BrainHelper.createStatInfo(faMan, data, ref, volume.getId(),
                    request.details.userId, request.details.groupIds, null, null, null));
            }
            
            // update POSIX timestamps
            if (updateATime)
                sMan.updateFileTimes(dir.getId(), true, false, false);
            
            MessageUtils.marshallResponse(request, result);
            this.notifyRequestListener(request);
            
        } catch (UserException exc) {
            throw exc;
        } catch (BrainException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void open(MRCRequest request, String path, String accessMode) throws BrainException,
        UserException {
        
        try {
            
            Path p = new Path(path);
            VolumeInfo volume = sliceMan.getVolumeByName(p.getVolumeName());
            StorageManager sMan = sliceMan
                    .getSliceDB(volume.getId(), p.getPathWithoutVolume(), 'r');
            
            AbstractFileEntity parentDir = sMan.getFileEntity(p.getInnerPath(), true);
            
            // check whether the parent directory of the source file is
            // searchable
            faMan.checkSearchPermission(volume.getId(), p.getInnerPath(), request.details.userId,
                request.details.superUser, request.details.groupIds);
            
            AbstractFileEntity file = sMan.getChild(p.getLastPathComponent(), parentDir.getId());
            if (file == null)
                throw new UserException(ErrNo.ENOENT, "file '" + p.getLastPathComponent()
                    + "' does not exist");
            
            // if the file refers to a symbolic link, resolve the link
            String target = sMan.getFileReference(file.getId());
            if (target != null) {
                path = target;
                p = new Path(path);
                
                // if the local MRC is not responsible, send a redirect
                if (!sliceMan.hasVolume(p.getVolumeName())) {
                    MessageUtils.setRedirect(request, target);
                    this.notifyRequestListener(request);
                    return;
                }
                
                volume = sliceMan.getVolumeByName(p.getVolumeName());
                sMan = sliceMan.getSliceDB(volume.getId(), p.getPathWithoutVolume(), 'r');
                
                file = sMan.getFileEntity(p.getPathWithoutVolume());
            }
            
            if (file.isDirectory())
                throw new UserException(ErrNo.EISDIR, "open is restricted to files");
            
            AccessMode mode = null;
            try {
                mode = AccessMode.valueOf(accessMode);
            } catch (IllegalArgumentException exc) {
                throw new UserException(ErrNo.EINVAL, "invalid access mode for 'open': "
                    + accessMode);
            }
            
            // get the current epoch, use (and increase) the truncate number if
            // the open mode is truncate
            long epochNo;
            FileEntity fileAsFile = (FileEntity) file;
            if (mode == AccessMode.t) {
                epochNo = fileAsFile.getIssuedEpoch() + 1;
                setTruncateEpoch(request, volume.getId(), fileAsFile.getId(), epochNo);
            } else
                epochNo = fileAsFile.getEpoch();
            
            // create the capability; return if the operation fails
            String capability = null;
            try {
                
                // check whether the file is marked as 'read-only'; in this
                // case, throw an exception if write access is requested
                if ((mode == AccessMode.w || mode == AccessMode.a || mode == AccessMode.ga || mode == AccessMode.t)
                    && sMan.isReadOnly(file.getId()))
                    throw new UserException(ErrNo.EPERM, "read-only files cannot be written");
                
                // check whether the permission is granted
                faMan.checkPermission(accessMode, volume.getId(), file.getId(), 0,
                    request.details.userId, request.details.superUser, request.details.groupIds);
                
                capability = BrainHelper.createCapability(accessMode, volume.getId(), file.getId(),
                    epochNo, config.getCapabilitySecret()).toString();
                
            } catch (UserException exc) {
                
                MessageUtils.marshallResponse(request, null);
                this.notifyRequestListener(request);
                return;
            }
            
            // get the list of replicas associated with the file
            XLocationsList xLocList = sMan.getXLocationsList(file.getId());
            
            // if no replica exists yet, create one using the default striping
            // policy together with a set of feasible OSDs from the OSD status
            // manager
            if (xLocList == null || xLocList.getReplicas() == null) {
                
                xLocList = BrainHelper.createXLocList(xLocList, sMan, osdMan, p, file.getId(),
                    parentDir.getId(), volume, request.getPinkyRequest().getClientAddress());
                
                assignOSDs(request, volume.getId(), p.getPathWithoutVolume(), Converter
                        .xLocListToList(xLocList));
            }
            
            HTTPHeaders headers = BrainHelper.createXCapHeaders(capability, xLocList);
            
            // update POSIX timestamps
            if (updateATime)
                sMan.updateFileTimes(file.getId(), true, false, false);
            
            MessageUtils.marshallResponse(request, null, headers);
            this.notifyRequestListener(request);
            
        } catch (UserException e) {
            throw e;
        } catch (BrainException e) {
            throw e;
        } catch (Exception e) {
            throw new BrainException(e);
        }
    }
    
    public void renew(MRCRequest request) throws UserException, BrainException {
        
        try {
            
            String capString = request.getPinkyRequest().requestHeaders
                    .getHeader(HTTPHeaders.HDR_XCAPABILITY);
            String newsizeString = request.getPinkyRequest().requestHeaders
                    .getHeader(HTTPHeaders.HDR_XNEWFILESIZE);
            
            if (capString == null)
                throw new UserException("missing " + HTTPHeaders.HDR_XCAPABILITY + " header");
            
            Capability cap = new Capability(capString, config.getCapabilitySecret());
            
            // check whether the received capability has a valid signature
            if (!cap.isValid())
                throw new UserException(capString + " is invalid");
            
            // if an X-NewFileSize header has been sent, update the file size if
            // necessary ...
            if (newsizeString != null) {
                
                // parse volume and file ID from global file ID
                String globalFileId = cap.getFileId();
                int i = globalFileId.indexOf(':');
                String volumeId = cap.getFileId().substring(0, i);
                long fileId = Long.parseLong(cap.getFileId().substring(i + 1));
                
                StorageManager sMan = sliceMan.getSliceDB(volumeId, fileId, 'r');
                
                // if the file still exists, update the file size
                if (sMan.getFileEntity(fileId) != null) {
                    
                    setFileSize(request, volumeId, fileId, newsizeString);
                    
                    request.details.sliceId = sMan.getSliceId();
                    request.details.persistentOperation = true;
                }
            }
            
            Capability newCap = new Capability(cap.getFileId(), cap.getAccessMode(), cap
                    .getEpochNo(), config.getCapabilitySecret());
            
            HTTPHeaders headers = BrainHelper.createXCapHeaders(newCap.toString(), null);
            
            MessageUtils.marshallResponse(request, null, headers);
            this.notifyRequestListener(request);
            
        } catch (UserException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    /**
     * Look up the files identified by volumeID and fileID.
     * 
     * @param request
     * 
     * @param volumeID
     * 
     * @param fileIDs
     */
    public void checkFileList(MRCRequest request, String volumeID, List<String> fileIDs)
        throws BrainException {
        String response = "";
        
        try {
            if (fileIDs == null || fileIDs.size() == 0)
                throw new BackendException("fileList was empty!");
            for (String fileID : fileIDs) {
                if (fileID == null)
                    throw new BackendException("fileID was null!");
                response += (sliceMan.exists(volumeID, fileID.toString())) ? "1" : "0";
            }
        } catch (UserException ue) {
            response = "2";
        } catch (BackendException be) {
            throw new BrainException("checkFileList caused an Exception: " + be.getMessage());
        }
        // send an answer to the osd
        MessageUtils.marshallResponse(request, response);
        this.notifyRequestListener(request);
    }
    
    public void updateFileSize(MRCRequest request) throws BrainException {
        
        try {
            
            String capString = request.getPinkyRequest().requestHeaders
                    .getHeader(HTTPHeaders.HDR_XCAPABILITY);
            String newsizeString = request.getPinkyRequest().requestHeaders
                    .getHeader(HTTPHeaders.HDR_XNEWFILESIZE);
            
            if (capString == null)
                throw new UserException("missing " + HTTPHeaders.HDR_XCAPABILITY + " header");
            if (newsizeString == null)
                throw new UserException("missing " + HTTPHeaders.HDR_XNEWFILESIZE + " header");
            
            // create a capability object to verify the capability
            Capability cap = new Capability(capString, config.getCapabilitySecret());
            
            // check whether the received capability has a valid signature
            if (!cap.isValid())
                throw new UserException(capString + " is invalid");
            
            // parse volume and file ID from global file ID
            String globalFileId = cap.getFileId();
            int i = globalFileId.indexOf(':');
            String volumeId = cap.getFileId().substring(0, i);
            long fileId = Long.parseLong(cap.getFileId().substring(i + 1));
            StorageManager sMan = sliceMan.getSliceDB(volumeId, fileId, 'r');
            
            setFileSize(request, volumeId, fileId, newsizeString);
            
            request.details.sliceId = sMan.getSliceId();
            request.details.persistentOperation = true;
            
            MessageUtils.marshallResponse(request, null, null);
            this.notifyRequestListener(request);
            
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void setFileSize(MRCRequest request, String volumeId, long fileId, String newsizeString)
        throws UserException, BrainException {
        
        try {
            // prepare the request for the log replay
            List<Object> args = new ArrayList<Object>(3);
            args.add(volumeId);
            args.add(fileId);
            args.add(newsizeString);
            ReusableBuffer body = ReusableBuffer.wrap(JSONParser.writeJSON(args).getBytes(
                HTTPUtils.ENC_UTF8));
            request.getPinkyRequest().setURIAndBody("setFileSize", body);
            
            StorageManager sMan = sliceMan.getSliceDB(volumeId, fileId, 'w');
            
            BrainHelper.updateFileSize(sMan, updateATime, volumeId, fileId, newsizeString);
            
        } catch (UserException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void setTruncateEpoch(MRCRequest request, String volumeId, long fileId,
        long truncateEpoch) throws BrainException {
        
        try {
            // prepare the request for the log replay
            List<Object> args = new ArrayList<Object>(3);
            args.add(volumeId);
            args.add(fileId);
            args.add(truncateEpoch);
            ReusableBuffer body = ReusableBuffer.wrap(JSONParser.writeJSON(args).getBytes(
                HTTPUtils.ENC_UTF8));
            request.getPinkyRequest().setURIAndBody("setTruncateEpoch", body);
            
            StorageManager sMan = sliceMan.getSliceDB(volumeId, fileId, 'w');
            FileEntity file = (FileEntity) sMan.getFileEntity(fileId);
            sMan.setFileSize(fileId, file.getSize(), file.getEpoch(), truncateEpoch);
            
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void assignOSDs(MRCRequest request, String volumeId, String path, List<Object> xLocList)
        throws BrainException {
        
        try {
            // prepare the request for the log replay
            List<Object> args = new ArrayList<Object>(3);
            args.add(volumeId);
            args.add(path);
            args.add(xLocList);
            ReusableBuffer body = ReusableBuffer.wrap(JSONParser.writeJSON(args).getBytes(
                HTTPUtils.ENC_UTF8));
            
            // assign the OSDs
            StorageManager sMan = sliceMan.getSliceDB(volumeId, path,
                request.syncPseudoRequest ? '*' : 'w');
            sMan.setXLocationsList(sMan.getFileEntity(path).getId(), Converter
                    .listToXLocList(xLocList));
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "assigned xloc list to " + path
                    + ": " + xLocList);
            
            request.getPinkyRequest().setURIAndBody("assignOSDs", body);
            request.details.sliceId = sMan.getSliceId();
            request.details.persistentOperation = true;
            
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void setDefaultStripingPolicy(MRCRequest request, String path,
        Map<String, Object> stripingPolicy) throws BrainException {
        
        try {
            
            doSetStripingPolicy(request, path, stripingPolicy, true);
            
            MessageUtils.marshallResponse(request, null);
            this.notifyRequestListener(request);
            
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
        
    }
    
    public void getDefaultStripingPolicy(MRCRequest request, String path) throws UserException,
        BackendException, BrainException {
        
        Path p = new Path(path);
        VolumeInfo volume = sliceMan.getVolumeByName(p.getVolumeName());
        StorageManager sMan = sliceMan.getSliceDB(volume.getId(), p.getPathWithoutVolume(),
            request.syncPseudoRequest ? '*' : 'w');
        
        AbstractFileEntity parentDir = sMan.getFileEntity(p.getInnerPath(), true);
        
        if (!request.details.authorized) {
            // check whether the parent directory of the file grants search
            // access
            if (p.getLastPathComponent().length() != 0)
                faMan.checkSearchPermission(volume.getId(), p.getInnerPath(),
                    request.details.userId, request.details.superUser, request.details.groupIds);
        }
        
        AbstractFileEntity dir = sMan.getChild(p.getLastPathComponent(), parentDir.getId());
        if (dir == null)
            throw new UserException(ErrNo.ENOENT, "directory '" + p.getLastPathComponent()
                + "' does not exist");
        
        // if the directory refers to a symbolic link, resolve the link
        String target = sMan.getFileReference(dir.getId());
        if (target != null) {
            path = target;
            p = new Path(path);
            
            // if the local MRC is not responsible, send a redirect
            if (!sliceMan.hasVolume(p.getVolumeName())) {
                MessageUtils.setRedirect(request, target);
                this.notifyRequestListener(request);
                return;
            }
            
            volume = sliceMan.getVolumeByName(p.getVolumeName());
            sMan = sliceMan.getSliceDB(volume.getId(), p.getPathWithoutVolume(), 'r');
            
            dir = sMan.getFileEntity(p.getPathWithoutVolume());
        }
        
        if (!dir.isDirectory())
            throw new UserException(ErrNo.ENOTDIR, path + " does not point to a directory");
        
        Map<String, Object> sp = Converter.stripingPolicyToMap(sMan.getStripingPolicy(dir.getId()));
        
        MessageUtils.marshallResponse(request, sp);
        this.notifyRequestListener(request);
    }
    
    public void changeAccessMode(MRCRequest request, String path, long mode) throws UserException,
        BrainException {
        
        try {
            
            Path p = new Path(path);
            VolumeInfo volume = sliceMan.getVolumeByName(p.getVolumeName());
            StorageManager sMan = sliceMan.getSliceDB(volume.getId(), p.getPathWithoutVolume(),
                request.syncPseudoRequest ? '*' : 'w');
            
            AbstractFileEntity parentDir = sMan.getFileEntity(p.getInnerPath());
            
            if (!request.details.authorized) {
                // check whether the parent directory of the file grants search
                // access
                if (p.getLastPathComponent().length() != 0)
                    faMan
                            .checkSearchPermission(volume.getId(), p.getInnerPath(),
                                request.details.userId, request.details.superUser,
                                request.details.groupIds);
            }
            
            AbstractFileEntity file = sMan.getChild(p.getLastPathComponent(), parentDir.getId());
            if (file == null)
                throw new UserException(ErrNo.ENOENT, "file or directory '"
                    + p.getLastPathComponent() + "' does not exist");
            
            if (!request.details.authorized) {
                // check whether the access mode may be changed
                faMan.checkPrivilegedPermissions(volume.getId(), file.getId(),
                    request.details.userId, request.details.superUser, request.details.groupIds);
            }
            
            // change the access mode
            faMan.setPosixAccessMode(volume.getId(), file.getId(), request.details.userId,
                request.details.groupIds, mode);
            
            // update POSIX timestamps
            sMan.updateFileTimes(file.getId(), false, true, false);
            
            request.details.sliceId = sMan.getSliceId();
            request.details.persistentOperation = true;
            
            MessageUtils.marshallResponse(request, null);
            this.notifyRequestListener(request);
            
        } catch (UserException e) {
            throw e;
        } catch (BrainException e) {
            throw e;
        } catch (Exception e) {
            throw new BrainException(e);
        }
    }
    
    public void changeOwner(MRCRequest request, String path, String userId, String groupId)
        throws UserException, BrainException {
        
        try {
            
            doChangeOwner(request, path, userId, groupId, true);
            
            MessageUtils.marshallResponse(request, null);
            this.notifyRequestListener(request);
            
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
        
    }
    
    public void setACLEntries(MRCRequest request, String path, Map<String, Object> aclEntries)
        throws BrainException, UserException {
        
        try {
            
            doSetACLEntries(request, path, aclEntries, true);
            
            MessageUtils.marshallResponse(request, null);
            this.notifyRequestListener(request);
            
        } catch (UserException e) {
            throw e;
        } catch (BrainException e) {
            throw e;
        } catch (Exception e) {
            throw new BrainException(e);
        }
        
    }
    
    public void removeACLEntries(MRCRequest request, String path, List<Object> entities)
        throws UserException, BrainException {
        
        try {
            
            Path p = new Path(path);
            VolumeInfo volume = sliceMan.getVolumeByName(p.getVolumeName());
            StorageManager sMan = sliceMan.getSliceDB(volume.getId(), p.getPathWithoutVolume(),
                request.syncPseudoRequest ? '*' : 'w');
            
            AbstractFileEntity parentDir = sMan.getFileEntity(p.getInnerPath());
            
            if (!request.details.authorized) {
                // check whether the parent directory of the file grants search
                // access
                if (p.getLastPathComponent().length() != 0)
                    faMan
                            .checkSearchPermission(volume.getId(), p.getInnerPath(),
                                request.details.userId, request.details.superUser,
                                request.details.groupIds);
            }
            
            AbstractFileEntity file = sMan.getChild(p.getLastPathComponent(), parentDir.getId());
            if (file == null)
                throw new UserException(ErrNo.ENOENT, "file or directory '"
                    + p.getLastPathComponent() + "' does not exist");
            
            if (!request.details.authorized) {
                // check whether the access mode may be changed
                faMan.checkPrivilegedPermissions(volume.getId(), file.getId(),
                    request.details.userId, request.details.superUser, request.details.groupIds);
            }
            
            // change the ACL
            faMan.removeACLEntries(volume.getId(), file.getId(), request.details.userId,
                request.details.groupIds, entities);
            
            // update POSIX timestamps
            sMan.updateFileTimes(file.getId(), false, true, false);
            
            request.details.sliceId = sMan.getSliceId();
            request.details.persistentOperation = true;
            
            MessageUtils.marshallResponse(request, null);
            this.notifyRequestListener(request);
            
        } catch (UserException e) {
            throw e;
        } catch (BrainException e) {
            throw e;
        } catch (Exception e) {
            throw new BrainException(e);
        }
        
    }
    
    public void setXAttrs(MRCRequest request, String path, Map<String, Object> xAttrs)
        throws UserException, BrainException {
        
        try {
            Path p = new Path(path);
            VolumeInfo volume = sliceMan.getVolumeByName(p.getVolumeName());
            StorageManager sMan = sliceMan.getSliceDB(volume.getId(), p.getPathWithoutVolume(),
                request.syncPseudoRequest ? '*' : 'w');
            
            AbstractFileEntity parentDir = sMan.getFileEntity(p.getInnerPath());
            
            if (!request.details.authorized) {
                // check whether the parent directory of the file grants search
                // access
                if (p.getLastPathComponent().length() != 0)
                    faMan
                            .checkSearchPermission(volume.getId(), p.getInnerPath(),
                                request.details.userId, request.details.superUser,
                                request.details.groupIds);
            }
            
            AbstractFileEntity file = sMan.getChild(p.getLastPathComponent(), parentDir.getId());
            if (file == null)
                throw new UserException(ErrNo.ENOENT, "file or directory '"
                    + p.getLastPathComponent() + "' does not exist");
            
            // set all system attributes included in the map
            for (String attrKey : new HashSet<String>(xAttrs.keySet())) {
                
                if (attrKey.startsWith("xtreemfs.")) {
                    
                    // check whether the user has privileged permissions to set
                    // system attributes
                    faMan
                            .checkPrivilegedPermissions(volume.getId(), file.getId(),
                                request.details.userId, request.details.superUser,
                                request.details.groupIds);
                    
                    BrainHelper.setSysAttrValue(sMan, sliceMan, volume, file, attrKey.substring(9), xAttrs
                            .get(attrKey).toString());
                    
                    xAttrs.remove(attrKey);
                }
            }
            
            sMan.addXAttributes(file.getId(), xAttrs);
            
            // update POSIX timestamps
            sMan.updateFileTimes(file.getId(), false, true, false);
            
            request.details.sliceId = sMan.getSliceId();
            request.details.persistentOperation = true;
            
            MessageUtils.marshallResponse(request, null);
            this.notifyRequestListener(request);
            
        } catch (UserException exc) {
            throw exc;
        } catch (BrainException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void removeXAttrs(MRCRequest request, String path, List<Object> keys)
        throws UserException, BrainException {
        
        try {
            
            Path p = new Path(path);
            VolumeInfo volume = sliceMan.getVolumeByName(p.getVolumeName());
            StorageManager sMan = sliceMan.getSliceDB(volume.getId(), p.getPathWithoutVolume(),
                request.syncPseudoRequest ? '*' : 'w');
            
            AbstractFileEntity parentDir = sMan.getFileEntity(p.getInnerPath());
            
            if (!request.details.authorized) {
                // check whether the parent directory of the file grants search
                // access
                if (p.getLastPathComponent().length() != 0)
                    faMan
                            .checkSearchPermission(volume.getId(), p.getInnerPath(),
                                request.details.userId, request.details.superUser,
                                request.details.groupIds);
            }
            
            AbstractFileEntity file = sMan.getChild(p.getLastPathComponent(), parentDir.getId());
            if (file == null)
                throw new UserException(ErrNo.ENOENT, "file or directory '"
                    + p.getLastPathComponent() + "' does not exist");
            
            sMan.deleteXAttributes(file.getId(), keys);
            
            // update POSIX timestamps
            sMan.updateFileTimes(file.getId(), false, true, false);
            
            request.details.sliceId = sMan.getSliceId();
            request.details.persistentOperation = true;
            
            MessageUtils.marshallResponse(request, null);
            this.notifyRequestListener(request);
            
        } catch (UserException exc) {
            throw exc;
        } catch (BrainException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void addReplica(MRCRequest request, String globalFileId,
        Map<String, Object> stripingPolicy, List<Object> osdList) throws BrainException,
        UserException {
        
        try {
            
            StringTokenizer st = new StringTokenizer(globalFileId, ":");
            if (st.countTokens() != 2)
                throw new BrainException(
                    "invalid global file ID - needs to be as follows: \"volumeIdAsString\":\"fileIdInVolumeAsNumber\"");
            String volumeId = st.nextToken();
            long fileId = -1;
            try {
                fileId = Long.parseLong(st.nextToken());
            } catch (NumberFormatException exc) {
                throw new BrainException(
                    "invalid global file ID - needs to be as follows: \"volumeIdAsString\":\"fileIdInVolumeAsNumber\"");
            }
            
            StorageManager sMan = sliceMan.getSliceDB(volumeId, fileId,
                request.syncPseudoRequest ? '*' : 'w');
            
            // check whether a striping policy is explicitly assigned to the
            // replica; if not, use the one from the file; if none is assigned
            // to the file either, use the one from the volume; if the volume
            // does not have a default striping policy, throw an exception
            StripingPolicy sPol = Converter.mapToStripingPolicy(stripingPolicy);
            if (sPol == null)
                sPol = sMan.getStripingPolicy(fileId);
            if (sPol == null)
                sPol = sMan.getVolumeStripingPolicy();
            if (sPol == null)
                throw new UserException(ErrNo.EPERM,
                    "either the replica, the file or the volume need a striping policy");
            
            AbstractFileEntity entity = sMan.getFileEntity(fileId);
            if (!(entity instanceof FileEntity))
                throw new UserException(ErrNo.EPERM, "replicas may only be added to files");
            
            // if the file refers to a symbolic link, resolve the link
            String target = sMan.getFileReference(fileId);
            if (target != null) {
                String path = target;
                Path p = new Path(path);
                
                // if the local MRC is not responsible, send a redirect
                if (!sliceMan.hasVolume(p.getVolumeName())) {
                    MessageUtils.setRedirect(request, target);
                    this.notifyRequestListener(request);
                    return;
                }
                
                VolumeInfo volume = sliceMan.getVolumeByName(p.getVolumeName());
                sMan = sliceMan.getSliceDB(volume.getId(), p.getPathWithoutVolume(), 'r');
                
                fileId = sMan.getFileEntity(p.getPathWithoutVolume()).getId();
            }
            
            if (!sMan.isReadOnly(fileId))
                throw new UserException(ErrNo.EPERM,
                    "the file has to be made read-only before adding replicas");
            
            // check whether the new replica relies on a set of OSDs which
            // hasn't been used yet
            XLocationsList xLocList = sMan.getXLocationsList(fileId);
            
            if (!BrainHelper.isAddable(xLocList, osdList))
                throw new UserException(
                    "at least one OSD already used in current X-Locations list '"
                        + JSONParser.writeJSON(Converter.xLocListToList(xLocList)) + "'");
            
            // create a new replica and add it to the client's X-Locations list
            // (this will automatically increment the X-Locations list version)
            XLocation replica = new XLocation(sPol, osdList.toArray(new String[osdList.size()]));
            if (xLocList == null)
                xLocList = new XLocationsList(new XLocation[] { replica }, 0);
            else
                xLocList.addReplica(replica);
            sMan.setXLocationsList(fileId, xLocList);
            
            // update POSIX timestamps
            sMan.updateFileTimes(fileId, false, true, false);
            
            request.details.sliceId = sMan.getSliceId();
            request.details.persistentOperation = true;
            
            MessageUtils.marshallResponse(request, null);
            this.notifyRequestListener(request);
            
        } catch (UserException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void removeReplica(MRCRequest request, String globalFileId,
        Map<String, Object> stripingPolicy, List<Object> osdList) throws BrainException,
        UserException {
        
        try {
            
            StringTokenizer st = new StringTokenizer(globalFileId, ":");
            if (st.countTokens() != 2)
                throw new BrainException(
                    "invalid global file ID - needs to look like this: \"volumeIdAsString\":\"fileIdInVolumeAsNumber\"");
            String volumeId = st.nextToken();
            long fileId = -1;
            try {
                fileId = Long.parseLong(st.nextToken());
            } catch (NumberFormatException exc) {
                throw new BrainException(
                    "invalid global file ID - needs to be as follows: \"volumeIdAsString\":\"fileIdInVolumeAsNumber\"");
            }
            
            StorageManager sMan = sliceMan.getSliceDB(volumeId, fileId,
                request.syncPseudoRequest ? '*' : 'w');
            
            XLocation repl = new XLocation(Converter.mapToStripingPolicy(stripingPolicy), osdList
                    .toArray(new String[osdList.size()]));
            
            // search for the replica in the X-Locations list
            XLocationsList xLocList = sMan.getXLocationsList(fileId);
            
            if (xLocList != null) {
                
                xLocList.removeReplica(repl);
                
                sMan.setXLocationsList(fileId, xLocList);
                
                // update POSIX timestamps
                sMan.updateFileTimes(fileId, false, true, false);
            }
            
            request.details.sliceId = sMan.getSliceId();
            request.details.persistentOperation = true;
            
            MessageUtils.marshallResponse(request, null);
            this.notifyRequestListener(request);
            
        } catch (UserException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void setReplicas(MRCRequest request, String globalFileId, List<List<Object>> replicas)
        throws BrainException, UserException {
        
        try {
            
            StringTokenizer st = new StringTokenizer(globalFileId, ":");
            if (st.countTokens() != 2)
                throw new BrainException(
                    "invalid global file ID - needs to look like this: \"volumeIdAsString\":\"fileIdInVolumeAsNumber\"");
            String volumeId = st.nextToken();
            long fileId = -1;
            try {
                fileId = Long.parseLong(st.nextToken());
            } catch (NumberFormatException exc) {
                throw new BrainException(
                    "invalid global file ID - needs to be as follows: \"volumeIdAsString\":\"fileIdInVolumeAsNumber\"");
            }
            
            StorageManager sMan = sliceMan.getSliceDB(volumeId, fileId,
                request.syncPseudoRequest ? '*' : 'w');
            
            // create an array of X-Locations from the given 'replicas' argument
            XLocation[] newRepls = null;
            if (replicas != null) {
                newRepls = new XLocation[replicas.size()];
                
                for (int i = 0; i < replicas.size(); i++) {
                    
                    Map<String, Object> spol = (Map<String, Object>) replicas.get(i).get(0);
                    List<Object> osds = (List<Object>) replicas.get(i).get(0);
                    
                    newRepls[i] = new XLocation(Converter.mapToStripingPolicy(spol), osds
                            .toArray(new String[osds.size()]));
                }
            }
            
            // create the new X-Locations list from the given array
            XLocationsList xLocList = sMan.getXLocationsList(fileId);
            if (newRepls != null) {
                
                if (xLocList == null)
                    xLocList = new XLocationsList(newRepls, 0);
                else
                    xLocList = new XLocationsList(newRepls, xLocList.getVersion() + 1);
            }

            else if (xLocList != null)
                xLocList = new XLocationsList(null, xLocList.getVersion() + 1);
            
            sMan.setXLocationsList(fileId, null);
            
            // update POSIX timestamps
            sMan.updateFileTimes(fileId, false, true, false);
            
            request.details.sliceId = sMan.getSliceId();
            request.details.persistentOperation = true;
            
            MessageUtils.marshallResponse(request, null);
            this.notifyRequestListener(request);
            
        } catch (UserException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void checkAccess(MRCRequest request, String path, String mode) throws BrainException,
        UserException {
        
        try {
            Path p = new Path(path);
            VolumeInfo volume = sliceMan.getVolumeByName(p.getVolumeName());
            StorageManager sMan = sliceMan
                    .getSliceDB(volume.getId(), p.getPathWithoutVolume(), 'r');
            
            AbstractFileEntity parentDir = sMan.getFileEntity(p.getInnerPath(), true);
            
            // check whether the parent directory is searchable
            if (p.getLastPathComponent().length() != 0)
                faMan.checkSearchPermission(volume.getId(), p.getInnerPath(),
                    request.details.userId, request.details.superUser, request.details.groupIds);
            
            AbstractFileEntity file = sMan.getChild(p.getLastPathComponent(), parentDir.getId());
            if (file == null)
                throw new UserException(ErrNo.ENOENT, "file or directory '"
                    + p.getLastPathComponent() + "' does not exist");
            
            boolean success = false;
            try {
                for (int i = 0; i < mode.length(); i++)
                    faMan.checkPermission(mode.substring(i, i + 1), volume.getId(), file.getId(),
                        parentDir.getId(), request.details.userId, request.details.superUser,
                        request.details.groupIds);
                success = true;
            } catch (UserException exc) {
                // permission denied
            }
            
            MessageUtils.marshallResponse(request, success);
            this.notifyRequestListener(request);
            
        } catch (UserException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void initFileSystem(MRCRequest request) throws BrainException {
        
        try {
            
            if (!request.details.superUser)
                throw new UserException(ErrNo.EPERM,
                    "only superusers can initialize the file system");
            
            // deregister all volumes
            List<String> volIDs = new LinkedList<String>();
            for (VolumeInfo volume : sliceMan.getVolumes())
                volIDs.add(volume.getId());
            
            request.details.context = new HashMap<String, Object>();
            request.details.context.put("volIDs", volIDs);
            
            // reset the partition manager
            sliceMan.reset();
            
            initFileSystemStep2(request);
            
        } catch (Exception e) {
            throw new BrainException(e);
        }
    }
    
    public void initFileSystemStep2(MRCRequest request) throws BrainException {
        
        if (request.sr != null)
            try {
                BrainHelper.parseResponse(request.sr);
            } catch (Exception exc) {
                Logging.logMessage(Logging.LEVEL_WARN, this, exc);
            }
        
        List<String> volIDs = (List<String>) request.details.context.get("volIDs");
        
        // if no more volumes need to be deregistered, send the response to
        // the
        // client
        if (volIDs.size() == 0) {
            MessageUtils.marshallResponse(request, null);
            this.notifyRequestListener(request);
        }

        // otherwise, deregister the next volume
        else {
            
            request.details.context.put("nextMethod", "initFileSystemStep2");
            
            String nextVolId = volIDs.remove(0);
            List<Object> args = new LinkedList<Object>();
            args.add(nextVolId);
            
            BrainHelper.submitRequest(this, request, dirService, "deregisterEntity", args,
                authString);
        }
        
    }
    
    public void query(MRCRequest request, String path, String queryString) throws UserException,
        BrainException {
        
        try {
            
            Path p = new Path(path);
            VolumeInfo volume = sliceMan.getVolumeByName(p.getVolumeName());
            StorageManager sMan = sliceMan
                    .getSliceDB(volume.getId(), p.getPathWithoutVolume(), 'r');
            
            // TODO: access control
            
            MessageUtils.marshallResponse(request, sMan.submitQuery(p.getPathWithoutVolume(),
                queryString));
            this.notifyRequestListener(request);
            
        } catch (UserException e) {
            throw e;
        } catch (Exception e) {
            throw new BrainException(e);
        }
    }
    
    public long getTotalDBSize() throws BrainException {
        
        try {
            long size = 0;
            for (SliceID slice : sliceMan.getSliceList())
                size += sliceMan.getSliceDB(slice, 'r').getDBFileSize();
            
            return size;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public long getTotalNumberOfFiles() throws BrainException {
        
        try {
            long count = 0;
            for (SliceID slice : sliceMan.getSliceList())
                count += sliceMan.getSliceDB(slice, 'r').getNumberOfFiles();
            
            return count;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public long getTotalNumberOfDirs() throws BrainException {
        
        try {
            long count = 0;
            for (SliceID slice : sliceMan.getSliceList())
                count += sliceMan.getSliceDB(slice, 'r').getNumberOfDirs();
            
            return count;
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void shutdown() throws BrainException {
        
        try {
            sliceMan.closeSliceDBs();
        } catch (Exception e) {
            throw new BrainException(e);
        }
    }
    
    public void setRequestListener(BrainRequestListener listener) {
        requestListener = listener;
    }
    
    public void getLocalVolumes(MRCRequest request) throws BrainException {
        
        try {
            List<VolumeInfo> volumes = sliceMan.getVolumes();
            
            Map<String, String> map = new HashMap<String, String>();
            for (VolumeInfo data : volumes)
                map.put(data.getId(), data.getName());
            
            MessageUtils.marshallResponse(request, map);
            this.notifyRequestListener(request);
            
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void checkpointDB() throws BrainException {
        try {
            sliceMan.compactDB();
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void completeDBCheckpoint() throws BrainException {
        try {
            sliceMan.completeDBCompaction();
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void restoreDB() throws BrainException {
        try {
            sliceMan.restoreDB();
        } catch (Exception exc) {
            throw new BrainException(exc);
        }
    }
    
    public void dumpDB(String dumpFilePath) throws Exception {
        sliceMan.dumpDB(dumpFilePath);
    }
    
    public void restoreDBFromDump(String dumpFilePath) throws Exception {
        sliceMan.restoreDBFromDump(dumpFilePath);
    }
    
    // public void createFileTree(MRCRequest request, List<Object> treeData,
    // String targetPath) throws BrainException, UserException {
    //
    // try {
    //
    // HTTPHeaders xCapHeaders = createSubtree(request, treeData,
    // targetPath);
    //
    // Path path = new Path(targetPath);
    // VolumeInfo volume = sliceMan.getVolumeByName(path.getVolumeName());
    // StorageManager sMan = sliceMan.getSliceDB(volume.getId(), path
    // .getPathWithoutVolume(), request.syncPseudoRequest ? '*'
    // : 'w');
    //
    // request.details.sliceId = sMan.getSliceId();
    // request.details.persistentOperation = true;
    //
    // MessageUtils.marshallResponse(request, null, xCapHeaders);
    // this.notifyRequestListener(request);
    //
    // } catch (UserException exc) {
    // throw exc;
    // } catch (Exception exc) {
    // throw new BrainException(exc);
    // }
    //
    // }
    //
    // public void replayMove(MRCRequest request, String sourcePath,
    // String targetPath) throws UserException, BrainException {
    //
    // try {
    // Path sPath = new Path(sourcePath);
    // Path tPath = new Path(targetPath);
    //
    // VolumeInfo sVolume = sliceMan
    // .getVolumeByName(sPath.getVolumeName());
    // StorageManager sMan = sliceMan.getSliceDB(sVolume.getId(), sPath
    // .getPathWithoutVolume(), '*');
    //
    // // if a remote move took place, only replay the local part of the
    // // operation ...
    // if (!sliceMan.hasVolume(tPath.getVolumeName())) {
    //
    // AbstractFileEntity source = sMan.getFileEntity(sPath
    // .getPathWithoutVolume());
    //
    // // delete the file/directory from the local volume
    // deleteRecursively(sMan, source.getId());
    //
    // MessageUtils.marshallResponse(request, null);
    // this.notifyRequestListener(request);
    // }
    //
    // // ... otherwise, completely replay the operation locally
    // else
    // move(request, sourcePath, targetPath);
    //
    // } catch (UserException exc) {
    // throw exc;
    // } catch (BrainException exc) {
    // throw exc;
    // } catch (Exception exc) {
    // throw new BrainException(exc);
    // }
    //
    // }
    
    // public void interVolumeMove(MRCRequest request, StorageManager sMan,
    // AbstractFileEntity source, Path targetPath, boolean executeLocally)
    // throws UserException, BrainException {
    //
    // request.details.context = new HashMap<String, Object>();
    // request.details.context.put("sMan", sMan);
    // request.details.context.put("source", source);
    // request.details.context.put("targetPath", targetPath);
    // request.details.context.put("executeLocally", executeLocally);
    //
    // if (executeLocally)
    // // if the volume resides on the local MRC, directly proceed with the
    // // next step
    // interVolumeMoveStep2(request);
    //
    // else {
    // // otherwise, retrieve the target host for the volume from the
    // // Directory Service
    //
    // Map<String, Object> queryMap = new HashMap<String, Object>();
    // queryMap.put("name", targetPath.getVolumeName());
    // queryMap.put("type", "volume");
    //
    // List<Object> attrs = new LinkedList<Object>();
    // attrs.add("mrcURL");
    //
    // List<Object> args = new LinkedList<Object>();
    // args.add(queryMap);
    // args.add(attrs);
    //
    // request.details.context.put("nextMethod", "interVolumeMoveStep2");
    // BrainHelper.submitRequest(this, request, dirService, "getEntities",
    // args, "nullauth " + url);
    // }
    // }
    //
    // public void interVolumeMoveStep2(MRCRequest request) throws
    // UserException,
    // BrainException {
    //
    // try {
    //
    // Path targetPath = (Path) request.details.context.get("targetPath");
    // StorageManager sMan = (StorageManager)
    // request.details.context.get("sMan");
    // AbstractFileEntity source = (AbstractFileEntity) request.details.context
    // .get("source");
    //
    // if ((Boolean) request.details.context.get("executeLocally")) {
    //
    // // if the target volume resides on the local MRC, create the
    // // target file tree locally
    // createFileTree(request, Converter.fileTreeToList(sMan, source),
    // targetPath.toString());
    //
    // } else {
    // // otherwise, create the target file tree remotely
    //
    // request.details.context.put("nextMethod", "interVolumeMoveStep3");
    //
    // Map<String, Map<String, Object>> response = (Map<String, Map<String,
    // Object>>) BrainHelper
    // .parseResponse(request.sr);
    //
    // if (response.size() == 0)
    // throw new UserException(ErrNo.ENOENT, "volume '"
    // + targetPath.getVolumeName() + "' unknown");
    //
    // assert (response.size() == 1);
    //
    // // get the MRC holding the volume from the query result
    // String targetEndpoint = (String) response.get(
    // response.keySet().iterator().next()).get("mrcURL");
    //
    // // serialize the entire subtree with all dependencies
    // List<Object> subTree = Converter.fileTreeToList(sMan, source);
    //
    // List<Object> params = new LinkedList<Object>();
    // params.add(subTree);
    // params.add(targetPath.toString());
    //
    // final InetSocketAddress targetMRCURL = new InetSocketAddress(
    // targetEndpoint.substring(0, targetEndpoint.indexOf(':')),
    // Integer.parseInt(targetEndpoint.substring(targetEndpoint
    // .indexOf(':') + 1)));
    //
    // // create the file tree on the remote host
    // BrainHelper.submitRequest(this, request, targetMRCURL,
    // "createFileTree", params);
    // }
    //
    // } catch (UserException exc) {
    // throw exc;
    // } catch (BrainException exc) {
    // throw exc;
    // } catch (Exception exc) {
    // throw new BrainException(exc);
    // }
    // }
    //
    // public void interVolumeMoveStep3(MRCRequest request) throws
    // BrainException {
    //
    // try {
    //
    // // check whether an exception has occured
    // Object response = BrainHelper.parseResponse(request.sr);
    // if (response != null) {
    // MessageUtils.marshallException(request,
    // (Map<String, Object>) response, false);
    // this.notifyRequestListener(request);
    // return;
    // }
    //
    // // if a capability for target file deletion has been issued, return
    // // it
    // HTTPHeaders xCapHeaders = null;
    // if (request.sr.responseHeaders
    // .getHeader(HTTPHeaders.HDR_XCAPABILITY) != null
    // && request.sr.responseHeaders
    // .getHeader(HTTPHeaders.HDR_XLOCATIONS) != null) {
    // xCapHeaders = new HTTPHeaders();
    // xCapHeaders.addHeader(HTTPHeaders.HDR_XCAPABILITY,
    // request.sr.responseHeaders
    // .getHeader(HTTPHeaders.HDR_XCAPABILITY));
    // xCapHeaders.addHeader(HTTPHeaders.HDR_XLOCATIONS,
    // request.sr.responseHeaders
    // .getHeader(HTTPHeaders.HDR_XLOCATIONS));
    // }
    //
    // StorageManager sMan = (StorageManager)
    // request.details.context.get("sMan");
    // AbstractFileEntity source = (AbstractFileEntity) request.details.context
    // .get("source");
    //
    // // delete the file/directory from the local volume
    // deleteRecursively(sMan, source.getId());
    //
    // MessageUtils.marshallResponse(request, null, xCapHeaders);
    // this.notifyRequestListener(request);
    //
    // } catch (Exception exc) {
    // throw new BrainException(exc);
    // }
    // }
    //
    // protected void deleteRecursively(StorageManager sMan, long fileId,
    // String name, long parentId) throws BackendException {
    //
    // Map<String, AbstractFileEntity> children = sMan.getChildData(fileId);
    // for (String fileName : children.keySet()) {
    // long childId = children.get(fileName).getId();
    // deleteRecursively(sMan, childId, fileName, fileId);
    // }
    //
    // sMan.unlinkFile(name, fileId, parentId);
    // }
    //
    // protected HTTPHeaders createSubtree(MRCRequest request,
    // List<Object> treeData, String targetPath) throws UserException,
    // BackendException, IOException, BrainException, JSONException {
    //
    // Path path = new Path(targetPath);
    // VolumeInfo volume = sliceMan.getVolumeByName(path.getVolumeName());
    // StorageManager sMan = sliceMan.getSliceDB(volume.getId(), path
    // .getPathWithoutVolume(), 'w');
    // long targetParentId = sMan.getFileEntity(path.getInnerPath(), true)
    // .getId();
    // AbstractFileEntity tChild = sMan.getChild(path.getLastPathComponent(),
    // targetParentId);
    //
    // long childId = 0;
    // if (tChild != null)
    // childId = tChild.getId();
    //
    // // unwrap the tree data
    // AbstractFileEntity file = Converter
    // .mapToFile((Map<String, Object>) treeData.get(0));
    // int sourceType = file.isDirectory() ? FILETYPE_DIR : FILETYPE_FILE;
    //
    // List<FileAttributeEntity> attributes = Converter
    // .attrMapsToAttrList((List<Map<String, Object>>) treeData.get(1));
    // List<List<Object>> subElements = (List<List<Object>>) treeData.get(2);
    //
    // int targetType = path.getPathWithoutVolume().length() == 0 ? FILETYPE_DIR
    // : tChild == null ? FILETYPE_NOTEXIST
    // : tChild.isDirectory() ? FILETYPE_DIR : FILETYPE_FILE;
    //
    // // peform the movement operation
    //
    // HTTPHeaders xCapHeaders = null;
    //
    // // if the source is a directory
    // if (sourceType == FILETYPE_DIR) {
    //
    // switch (targetType) {
    //
    // case FILETYPE_NOTEXIST: // target does not exist
    // {
    // // recursively cross-volume-move the source directory tree
    // // to the remote volume and link it to the parent directory
    // // of the target path
    //
    // file.setName(path.getLastPathComponent());
    // file.setParentId(targetParentId);
    //
    // // create a new file on the target storage manager with the
    // // data
    // sMan.createFile(file, attributes);
    //
    // for (List<Object> subElement : subElements)
    // createSubtree(request, subElement, targetPath + "/"
    // + ((Map<String, Object>) subElement.get(0)).get("name"));
    //
    // break;
    // }
    //
    // case FILETYPE_DIR: // target is a directory
    // {
    //
    // // chech whether the target directory may be deleted; if not,
    // // throw an exception
    //
    // if (!request.details.authorized) {
    //
    // // check whether the target directory may be overwritten
    // faMan.checkPermission(FileAccessManager.DELETE_ACCESS,
    // volume.getId(), tChild.getId(), request.details.userId,
    // request.details.groupIds);
    // }
    //
    // if (sMan.hasChildren(tChild.getId()))
    // throw new UserException(ErrNo.ENOTEMPTY,
    // "target directory '" + targetPath + "' is not empty");
    //
    // sMan.deleteFile(childId);
    //
    // // recursively cross-volume-move the source directory tree
    // // to the remote volume and link it to the parent directory
    // file.setName(path.getLastPathComponent());
    // file.setParentId(targetParentId);
    //
    // // create a new file on the target storage manager with the
    // // data
    // sMan.createFile(file, attributes);
    //
    // for (List<Object> subElement : subElements)
    // createSubtree(request, subElement, targetPath + "/"
    // + ((Map<String, Object>) subElement.get(0)).get("name"));
    //
    // break;
    // }
    //
    // case FILETYPE_FILE: // target is a file
    // throw new UserException(ErrNo.ENOTDIR,
    // "cannot rename directory '" + file.getName()
    // + "' to file '" + targetPath + "'");
    // }
    //
    // }
    //
    // // if the source is a file
    // else {
    //
    // switch (targetType) {
    //
    // case FILETYPE_NOTEXIST: // target does not exist
    // {
    // // create a new file on the remote volume and link it to the
    // // parent directory of the target path
    //
    // file.setName(path.getLastPathComponent());
    // file.setParentId(targetParentId);
    //
    // break;
    // }
    //
    // case FILETYPE_DIR: // target is a directory
    // {
    // throw new UserException(ErrNo.EISDIR, "cannot rename file '"
    // + file.getName() + "' to directory '" + targetPath + "'");
    // }
    //
    // case FILETYPE_FILE: // target is a file
    // {
    //
    // if (!request.details.authorized) {
    //
    // // obtain a deletion capability for the file
    // String aMode = faMan.translateAccessMode(volume.getId(),
    // FileAccessManager.DELETE_ACCESS);
    // String capability = BrainHelper.createCapability(faMan,
    // aMode, volume.getId(), tChild.getId(), request.details.userId,
    // request.details.groupIds).toString();
    //
    // // set the XCapability and XLocationsList headers
    // xCapHeaders = BrainHelper.createXCapHeaders(capability,
    // sMan.getXLocationsList(tChild.getId()));
    // }
    //
    // // delete the target file, rename the source file and relink
    // // it to the parent directory of the target path
    //
    // sMan.deleteFile(childId);
    //
    // file.setName(path.getLastPathComponent());
    // file.setParentId(targetParentId);
    //
    // break;
    // }
    //
    // }
    //
    // // create a new file on the target storage manager with the data
    // sMan.createFile(file, attributes);
    // }
    //
    // return xCapHeaders;
    // }
    
    protected void notifyRequestListener(MRCRequest request) {
        if (!request.syncPseudoRequest) {
            if (requestListener != null)
                requestListener.brainRequestDone(request);
            else
                throw new RuntimeException("listener must not be null!");
        }
    }
    
    protected VolumeInfo getVolumeData(String volumeId) throws UserException, BackendException {
        return sliceMan.getVolumeById(volumeId);
    }
    
    private void doSetACLEntries(MRCRequest request, String path, Map<String, Object> aclEntries,
        boolean persistentOperation) throws UserException, BackendException, BrainException {
        
        Path p = new Path(path);
        VolumeInfo volume = sliceMan.getVolumeByName(p.getVolumeName());
        StorageManager sMan = sliceMan.getSliceDB(volume.getId(), p.getPathWithoutVolume(),
            request.syncPseudoRequest ? '*' : 'w');
        
        AbstractFileEntity parentDir = sMan.getFileEntity(p.getInnerPath());
        
        if (!request.details.authorized) {
            // check whether the parent directory of the file grants search
            // access
            if (p.getLastPathComponent().length() != 0)
                faMan.checkSearchPermission(volume.getId(), p.getInnerPath(),
                    request.details.userId, request.details.superUser, request.details.groupIds);
        }
        
        AbstractFileEntity file = sMan.getChild(p.getLastPathComponent(), parentDir.getId());
        if (file == null)
            throw new UserException(ErrNo.ENOENT, "file or directory '" + p.getLastPathComponent()
                + "' does not exist");
        
        if (!request.details.authorized) {
            // check whether the access mode may be changed
            faMan.checkPrivilegedPermissions(volume.getId(), file.getId(), request.details.userId,
                request.details.superUser, request.details.groupIds);
        }
        
        // if the file refers to a symbolic link, resolve the link
        String target = sMan.getFileReference(file.getId());
        if (target != null) {
            path = target;
            p = new Path(path);
            
            // if the local MRC is not responsible, send a redirect
            if (!sliceMan.hasVolume(p.getVolumeName())) {
                MessageUtils.setRedirect(request, target);
                this.notifyRequestListener(request);
                return;
            }
            
            volume = sliceMan.getVolumeByName(p.getVolumeName());
            sMan = sliceMan.getSliceDB(volume.getId(), p.getPathWithoutVolume(), 'r');
            
            file = sMan.getFileEntity(p.getPathWithoutVolume());
        }
        
        // change the ACL
        faMan.setACLEntries(volume.getId(), file.getId(), request.details.userId,
            request.details.groupIds, aclEntries);
        
        // update POSIX timestamps
        sMan.updateFileTimes(file.getId(), false, true, false);
        
        if (persistentOperation) {
            request.details.sliceId = sMan.getSliceId();
            request.details.persistentOperation = true;
        }
    }
    
    private void doSetStripingPolicy(MRCRequest request, String path,
        Map<String, Object> stripingPolicy, boolean persistentOperation) throws UserException,
        BackendException, BrainException {
        
        Path p = new Path(path);
        VolumeInfo volume = sliceMan.getVolumeByName(p.getVolumeName());
        StorageManager sMan = sliceMan.getSliceDB(volume.getId(), p.getPathWithoutVolume(),
            request.syncPseudoRequest ? '*' : 'w');
        
        AbstractFileEntity parentDir = sMan.getFileEntity(p.getInnerPath(), true);
        
        if (!request.details.authorized) {
            // check whether the parent directory of the file grants search
            // access
            if (p.getLastPathComponent().length() != 0)
                faMan.checkSearchPermission(volume.getId(), p.getInnerPath(),
                    request.details.userId, request.details.superUser, request.details.groupIds);
        }
        
        AbstractFileEntity dir = sMan.getChild(p.getLastPathComponent(), parentDir.getId());
        if (dir == null)
            throw new UserException(ErrNo.ENOENT, "directory '" + p.getLastPathComponent()
                + "' does not exist");
        
        // if the directory refers to a symbolic link, resolve the link
        String target = sMan.getFileReference(dir.getId());
        if (target != null) {
            path = target;
            p = new Path(path);
            
            // if the local MRC is not responsible, send a redirect
            if (!sliceMan.hasVolume(p.getVolumeName())) {
                MessageUtils.setRedirect(request, target);
                this.notifyRequestListener(request);
                return;
            }
            
            volume = sliceMan.getVolumeByName(p.getVolumeName());
            sMan = sliceMan.getSliceDB(volume.getId(), p.getPathWithoutVolume(), 'r');
            
            dir = sMan.getFileEntity(p.getPathWithoutVolume());
        }
        
        if (!dir.isDirectory())
            throw new UserException(ErrNo.ENOTDIR,
                "default striping policies are restricted to directories and volumes");
        
        sMan.setStripingPolicy(dir.getId(), stripingPolicy);
        
        // update POSIX timestamps of parent directory
        sMan.updateFileTimes(dir.getId(), false, true, false);
        
        if (persistentOperation) {
            request.details.sliceId = sMan.getSliceId();
            request.details.persistentOperation = true;
        }
    }
    
    public void doChangeOwner(MRCRequest request, String path, String userId, String groupId,
        boolean persistentOperation) throws UserException, BackendException, BrainException {
        
        try {
            
            Path p = new Path(path);
            VolumeInfo volume = sliceMan.getVolumeByName(p.getVolumeName());
            StorageManager sMan = sliceMan.getSliceDB(volume.getId(), p.getPathWithoutVolume(),
                request.syncPseudoRequest ? '*' : 'w');
            
            AbstractFileEntity parentDir = sMan.getFileEntity(p.getInnerPath());
            
            if (!request.details.authorized) {
                // check whether the parent directory of the file grants search
                // access
                if (p.getLastPathComponent().length() != 0)
                    faMan
                            .checkSearchPermission(volume.getId(), p.getInnerPath(),
                                request.details.userId, request.details.superUser,
                                request.details.groupIds);
            }
            
            AbstractFileEntity file = sMan.getChild(p.getLastPathComponent(), parentDir.getId());
            if (file == null)
                throw new UserException(ErrNo.ENOENT, "file or directory '"
                    + p.getLastPathComponent() + "' does not exist");
            
            if (!request.details.authorized) {
                // check whether the owner may be changed
                faMan.checkPrivilegedPermissions(volume.getId(), file.getId(),
                    request.details.userId, request.details.superUser, request.details.groupIds);
            }
            
            if (groupId != null)
                sMan.setFileGroup(file.getId(), groupId);
            if (userId != null)
                sMan.setFileOwner(file.getId(), userId);
            
            // update POSIX timestamps
            sMan.updateFileTimes(file.getId(), false, true, false);
            
            if (persistentOperation) {
                request.details.sliceId = sMan.getSliceId();
                request.details.persistentOperation = true;
            }
            
        } catch (UserException e) {
            throw e;
        } catch (BrainException e) {
            throw e;
        } catch (Exception e) {
            throw new BrainException(e);
        }
    }
}
