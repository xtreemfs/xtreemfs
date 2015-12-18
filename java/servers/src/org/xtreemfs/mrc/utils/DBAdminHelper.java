/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.xml.sax.Attributes;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseResultSet;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.database.babudb.BabuDBStorageHelper.OwnerType;
import org.xtreemfs.mrc.database.babudb.BabuDBStorageHelper.QuotaInfo;
import org.xtreemfs.mrc.database.babudb.BabuDBStorageHelper.QuotaType;
import org.xtreemfs.mrc.metadata.ACLEntry;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.StripingPolicy;
import org.xtreemfs.mrc.metadata.XAttr;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;

public class DBAdminHelper {

    public static class DBRestoreState {

        public String              currentVolumeId;

        public String              currentVolumeName;

        public short               currentVolumeACPolicy;

        public List<Long>          parentIds;

        public FileMetadata        currentEntity;

        public int                 currentXLocVersion;

        public String              currentReplUpdatePolicy;

        public StripingPolicy      currentXLocSp;

        public List<XLoc>          currentReplicaList;

        public int                 currentReplFlags;

        public List<String>        currentOSDList;

        public Map<String, Object> currentACL;

        public long                largestFileId;
        
        public long currentVolumeQuota;

        public DBRestoreState() {
            parentIds = new LinkedList<Long>();
            parentIds.add((long) 0);
            currentOSDList = new LinkedList<String>();
            currentReplicaList = new LinkedList<XLoc>();
            currentACL = new HashMap<String, Object>();
        }

    }

    public static void restoreDir(VolumeManager vMan, FileAccessManager faMan, Attributes attrs, DBRestoreState state,
            int dbVersion, boolean openTag) throws DatabaseException, UserException {

        if (openTag) {

            long id = Long.parseLong(attrs.getValue(attrs.getIndex("id")));
            String name = OutputUtils.unescapeFromXML(attrs.getValue(attrs.getIndex("name")));
            String owner = attrs.getValue(attrs.getIndex("uid"));
            String owningGroup = attrs.getValue(attrs.getIndex("gid"));
            int atime = Integer.parseInt(attrs.getValue(attrs.getIndex("atime")));
            int ctime = Integer.parseInt(attrs.getValue(attrs.getIndex("ctime")));
            int mtime = Integer.parseInt(attrs.getValue(attrs.getIndex("mtime")));

            short rights = 511; // set all rights to 511 by default
            if (attrs.getIndex("rights") != -1)
                rights = Short.parseShort(attrs.getValue(attrs.getIndex("rights")));

            long w32Attrs = 0; // set all Win32 attributes to 0 by default
            if (attrs.getIndex("w32Attrs") != -1)
                w32Attrs = Long.parseLong(attrs.getValue(attrs.getIndex("w32Attrs")));

            // if the directory is the root directory, restore the volume
            if (id == 1) {
                vMan.createVolume(faMan, state.currentVolumeId, state.currentVolumeName, state.currentVolumeACPolicy,
                        owner, owningGroup, null, rights, state.currentVolumeQuota, new LinkedList<KeyValuePair>());

                StorageManager sMan = vMan.getStorageManager(state.currentVolumeId);
                state.currentEntity = sMan.getMetadata(1);
            }

            // otherwise, restore the directory
            else {

                // there must not be hard links to directories, so it is not
                // necessary to check if the directory exists already

                StorageManager sMan = vMan.getStorageManager(state.currentVolumeId);
                AtomicDBUpdate update = sMan.createAtomicDBUpdate(null, null);
                FileMetadata dir = sMan.createDir(id, state.parentIds.get(0), name, atime, ctime, mtime, owner,
                        owningGroup, rights, w32Attrs, update);
                update.execute();
                state.currentEntity = dir;
            }

            state.parentIds.add(0, id);

            if (state.currentEntity.getId() > state.largestFileId)
                state.largestFileId = state.currentEntity.getId();
        }

        else
            state.parentIds.remove(0);
    }

    public static void restoreFile(VolumeManager vMan, FileAccessManager faMan, Attributes attrs, DBRestoreState state,
            int dbVersion, boolean openTag) throws DatabaseException, UserException {

        if (!openTag)
            return;

        long id = Long.parseLong(attrs.getValue(attrs.getIndex("id")));
        String name = OutputUtils.unescapeFromXML(attrs.getValue(attrs.getIndex("name")));

        StorageManager sMan = vMan.getStorageManager(state.currentVolumeId);

        FileMetadata file = sMan.getMetadata(id);

        // First, check whether a file with the same ID already exists. This is
        // necessary, since files may be linked to multiple directories.
        if (file == null) {

            String owner = attrs.getValue(attrs.getIndex("uid"));
            String owningGroup = attrs.getValue(attrs.getIndex("gid"));
            int atime = Integer.parseInt(attrs.getValue(attrs.getIndex("atime")));
            int ctime = Integer.parseInt(attrs.getValue(attrs.getIndex("ctime")));
            int mtime = Integer.parseInt(attrs.getValue(attrs.getIndex("mtime")));
            long size = Long.parseLong(attrs.getValue(attrs.getIndex("size")));

            int epoch = 0; // set the epoch to 0 by default
            if (attrs.getIndex("epoch") != -1)
                epoch = Integer.parseInt(attrs.getValue(attrs.getIndex("epoch")));

            int issuedEpoch = 0; // set the issued epoch to 0 by default
            if (attrs.getIndex("issuedEpoch") != -1)
                epoch = Integer.parseInt(attrs.getValue(attrs.getIndex("issuedEpoch")));

            int rights = 511; // set all rights to 511 by default
            if (attrs.getIndex("rights") != -1)
                rights = Integer.parseInt(attrs.getValue(attrs.getIndex("rights")));

            long w32Attrs = 0; // set all Win32 attributes to 0 by default
            if (attrs.getIndex("w32Attrs") != -1)
                w32Attrs = Long.parseLong(attrs.getValue(attrs.getIndex("w32Attrs")));

            boolean readOnly = false; // set readOnly attribute to false by
            // default
            if (attrs.getIndex("readOnly") != -1)
                readOnly = Boolean.getBoolean(attrs.getValue(attrs.getIndex("readOnly")));

            AtomicDBUpdate update = sMan.createAtomicDBUpdate(null, null);
            file = sMan.createFile(id, state.parentIds.get(0), name, atime, ctime, mtime, owner, owningGroup, rights,
                    w32Attrs, size, readOnly, epoch, issuedEpoch, update);
            update.execute();
        }

        // otherwise, create a link
        else {
            AtomicDBUpdate update = sMan.createAtomicDBUpdate(null, null);
            sMan.link(file, state.parentIds.get(0), name, update);
            update.execute();
        }

        state.currentEntity = file;

        if (state.currentEntity.getId() > state.largestFileId)
            state.largestFileId = state.currentEntity.getId();
    }

    public static void restoreXLocList(VolumeManager vMan, FileAccessManager faMan, Attributes attrs,
            DBRestoreState state, int dbVersion, boolean openTag) throws DatabaseException, UserException {

        StorageManager sMan = vMan.getStorageManager(state.currentVolumeId);

        if (openTag) {
            state.currentXLocVersion = Integer.parseInt(attrs.getValue(attrs.getIndex("version")));
            if (attrs.getIndex("ruPolicy") != -1)
                state.currentReplUpdatePolicy = attrs.getValue(attrs.getIndex("ruPolicy"));
            else
                state.currentReplUpdatePolicy = ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE;
        }

        else {

            state.currentEntity.setXLocList(sMan.createXLocList(
                    state.currentReplicaList.toArray(new XLoc[state.currentReplicaList.size()]),
                    state.currentReplUpdatePolicy, state.currentXLocVersion));
            state.currentReplicaList.clear();

            AtomicDBUpdate update = sMan.createAtomicDBUpdate(null, null);
            sMan.setMetadata(state.currentEntity, FileMetadata.RC_METADATA, update);
            update.execute();
        }
    }

    public static void restoreXLoc(VolumeManager vMan, FileAccessManager faMan, Attributes attrs, DBRestoreState state,
            int dbVersion, boolean openTag) throws DatabaseException, UserException {

        StorageManager sMan = vMan.getStorageManager(state.currentVolumeId);

        if (openTag) {
            state.currentXLocSp = Converter.stringToStripingPolicy(sMan, attrs.getValue(attrs.getIndex("pattern")));
            state.currentReplFlags = attrs.getIndex("replFlags") == -1 ? 0 : Integer.parseInt(attrs.getValue(attrs
                    .getIndex("replFlags")));

        } else {

            state.currentReplicaList.add(sMan.createXLoc(state.currentXLocSp,
                    state.currentOSDList.toArray(new String[state.currentOSDList.size()]), state.currentReplFlags));
            state.currentOSDList.clear();
        }
    }

    public static void restoreOSD(VolumeManager vMan, FileAccessManager faMan, Attributes attrs, DBRestoreState state,
            int dbVersion, boolean openTag) throws DatabaseException {

        if (openTag)
            state.currentOSDList.add(attrs.getValue(attrs.getIndex("location")));
    }

    public static void restoreACL(VolumeManager vMan, FileAccessManager faMan, Attributes attrs, DBRestoreState state,
            int dbVersion, boolean openTag) throws DatabaseException, UserException {

        // convert old ACLs to POSIX access rights
        if (!openTag) {

            StorageManager sMan = vMan.getStorageManager(state.currentVolumeId);
            AtomicDBUpdate update = sMan.createAtomicDBUpdate(null, null);
            try {
                faMan.updateACLEntries(sMan, state.currentEntity, state.parentIds.get(0), state.currentACL, update);
            } catch (MRCException e) {
                throw new DatabaseException(e);
            } catch (UserException e) {
                throw new DatabaseException(e);
            }

            update.execute();
            state.currentACL.clear();
        }
    }

    public static void restoreEntry(VolumeManager vMan, FileAccessManager faMan, Attributes attrs,
            DBRestoreState state, int dbVersion, boolean openTag) throws DatabaseException {

        if (openTag) {

            String entityId = OutputUtils.unescapeFromXML(attrs.getValue(attrs.getIndex("entity")));
            if (dbVersion < 3 && entityId.contains("::"))
                entityId = entityId.replace("::", ":");

            short rights = (short) Long.parseLong(attrs.getValue(attrs.getIndex("rights")));
            state.currentACL.put(entityId, rights);
        }
    }

    public static void restoreAttr(VolumeManager vMan, FileAccessManager faMan, Attributes attrs, DBRestoreState state,
            int dbVersion, boolean openTag) throws DatabaseException, UserException {

        if (openTag) {

            int encIndex = attrs.getIndex("enc");
            boolean base64 = encIndex != -1 && "BASE64".equals(attrs.getValue(encIndex));

            int oIndex = attrs.getIndex("owner");
            String owner = oIndex == -1 ? "" : attrs.getValue(oIndex);

            String key = OutputUtils.unescapeFromXML(attrs.getValue(attrs.getIndex("key")));
            String valueEncoded = attrs.getValue(attrs.getIndex("value"));

            try {
                byte[] value = base64 ? OutputUtils.decodeBase64(valueEncoded) : OutputUtils.unescapeFromXML(
                        valueEncoded).getBytes();

                StorageManager sMan = vMan.getStorageManager(state.currentVolumeId);

                // if the value refers to a read-only flag, set it directly
                if (key.equals("ro")) {

                    String valueString = new String(value);

                    AtomicDBUpdate update = sMan.createAtomicDBUpdate(null, null);
                    state.currentEntity.setReadOnly(Boolean.getBoolean(valueString));
                    update.execute();
                }

                else {

                    if (owner.isEmpty() && key.equals("ref"))
                        key = "lt";

                    if (owner.isEmpty() && key.equals("spol"))
                        key = "sp";

                    AtomicDBUpdate update = sMan.createAtomicDBUpdate(null, null);
                    sMan.setXAttr(state.currentEntity.getId(), owner, key, value, update);
                    update.execute();
                }

            } catch (IOException exc) {
                throw new DatabaseException(
                        "BASE64 decoding of extended attribute value '" + valueEncoded + "' failed", exc);
            }
        }
    }

    public static void restoreQuota(VolumeManager vMan, FileAccessManager faMan, Attributes attrs,
            DBRestoreState state, int dbVersion, boolean openTag) throws UserException, DatabaseException {

        if (openTag) {

            String type = attrs.getValue(attrs.getIndex("type"));
            String name = OutputUtils.unescapeFromXML(attrs.getValue(attrs.getIndex("name")));
            Long quota = Long.valueOf(attrs.getValue(attrs.getIndex("quota")));
            Long usedSpace = Long.valueOf(attrs.getValue(attrs.getIndex("used")));
            Long blockedSpace = Long.valueOf(attrs.getValue(attrs.getIndex("blocked")));

            if (type == null) {
                throw new IllegalArgumentException("Missing quota type!");
            } else if (type.equals(QuotaType.VOLUME.getKey()) && name.isEmpty()) {
                throw new IllegalArgumentException("Name is not specified although required!");
            }

            StorageManager sMan = vMan.getStorageManager(state.currentVolumeId);
            AtomicDBUpdate update = sMan.createAtomicDBUpdate(null, null);

            QuotaType quotaType = QuotaType.getByKey(type);
            if (quotaType == null) {
                throw new IllegalArgumentException("Unsupported quota type!");
            } else {
                switch (quotaType) {
                case USER:
                    sMan.setUserQuota(name, quota, update);
                    sMan.setUserUsedSpace(name, usedSpace, update);
                    sMan.setUserBlockedSpace(name, blockedSpace, update);
                    break;
                case GROUP:
                    sMan.setGroupQuota(name, quota, update);
                    sMan.setGroupUsedSpace(name, usedSpace, update);
                    sMan.setGroupBlockedSpace(name, blockedSpace, update);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported quota type!");
                }
            }

            update.execute();
        }
    }

    /**
     * Creates an XML dump from a volume. The dump contains all files and directories of the volume, including
     * their attributes and ACLs.
     * 
     * @param xmlWriter
     *            the XML writer creating the dump
     * @param sMan
     *            the volume's storage manager
     * @throws IOException
     *             if an I/O error occurs
     * @throws DatabaseException
     *             if an error occurs while accessing the database
     */
    public static void dumpVolume(BufferedWriter xmlWriter, StorageManager sMan) throws IOException, DatabaseException {
        try {
            dumpDir(xmlWriter, sMan, 1);

            dumpQuotas(xmlWriter, sMan);
        } catch (JSONException exc) {
            throw new DatabaseException(exc);
        }
    }

    private static void dumpQuotas(BufferedWriter xmlWriter, StorageManager sMan) throws IOException, DatabaseException {

        xmlWriter.write("<quotas>\n");

        // volume quota not necessary, because it will be dumped as an extended attribute

        // user quotas
        Map<String, Map<String, Long>> allOwnerQuotaInfo = sMan.getAllOwnerQuotaInfo(OwnerType.USER, "");

        for (Entry<String, Map<String, Long>> userQuotaEntry : allOwnerQuotaInfo.entrySet()) {
            Map<String, Long> userValues = userQuotaEntry.getValue();
            xmlWriter.write("<quota type=\"" + QuotaType.USER.getKey() + "\" name=\""
                    + OutputUtils.escapeToXML(userQuotaEntry.getKey()) + "\" quota=\""
                    + userValues.get(QuotaInfo.QUOTA.getValueAsString()) + "\" used=\""
                    + userValues.get(QuotaInfo.USED.getValueAsString()) + "\" blocked=\""
                    + userValues.get(QuotaInfo.BLOCKED.getValueAsString()) + "\" />\n");
        }

        // group quotas
        Map<String, Map<String, Long>> allOwnerGroupQuotaInfo = sMan.getAllOwnerQuotaInfo(OwnerType.GROUP, "");

        for (Entry<String, Map<String, Long>> userQuotaEntry : allOwnerGroupQuotaInfo.entrySet()) {
            Map<String, Long> userValues = userQuotaEntry.getValue();
            xmlWriter.write("<quota type=\"" + QuotaType.GROUP.getKey() + "\" name=\""
                    + OutputUtils.escapeToXML(userQuotaEntry.getKey()) + "\" quota=\""
                    + userValues.get(QuotaInfo.QUOTA.getValueAsString()) + "\" used=\""
                    + userValues.get(QuotaInfo.USED.getValueAsString()) + "\" blocked=\""
                    + userValues.get(QuotaInfo.BLOCKED.getValueAsString()) + "\" />\n");
        }

        xmlWriter.write("</quotas>\n");
    }

    private static void dumpDir(BufferedWriter xmlWriter, StorageManager sMan, long parentId) throws DatabaseException,
            IOException, JSONException {

        FileMetadata parent = sMan.getMetadata(parentId);

        // serialize the parent directory
        xmlWriter.write("<dir id=\"" + parent.getId() + "\" name=\"" + OutputUtils.escapeToXML(parent.getFileName())
                + "\" uid=\"" + parent.getOwnerId() + "\" gid=\"" + parent.getOwningGroupId() + "\" atime=\""
                + parent.getAtime() + "\" ctime=\"" + parent.getCtime() + "\" mtime=\"" + parent.getMtime()
                + "\" rights=\"" + parent.getPerms() + "\" w32Attrs=\"" + parent.getW32Attrs() + "\">\n");

        // serialize the root directory's ACL
        dumpACL(xmlWriter, sMan.getACL(parentId));

        // serialize the root directory's attributes
        dumpAttrs(xmlWriter, sMan.getXAttrs(parentId));

        // serialize all nested elements
        DatabaseResultSet<FileMetadata> children = sMan.getChildren(parentId, 0, Integer.MAX_VALUE);
        while (children.hasNext()) {

            FileMetadata child = children.next();

            if (child.isDirectory())
                dumpDir(xmlWriter, sMan, child.getId());
            else
                dumpFile(xmlWriter, sMan, child);
        }
        children.destroy();

        xmlWriter.write("</dir>\n");
    }

    private static void dumpFile(BufferedWriter xmlWriter, StorageManager sMan, FileMetadata file)
            throws DatabaseException, IOException, JSONException {

        // serialize the file
        xmlWriter.write("<file id=\"" + file.getId() + "\" name=\"" + OutputUtils.escapeToXML(file.getFileName())
                + "\" size=\"" + file.getSize() + "\" epoch=\"" + file.getEpoch() + "\" issuedEpoch=\""
                + file.getIssuedEpoch() + "\" uid=\"" + file.getOwnerId() + "\" gid=\"" + file.getOwningGroupId()
                + "\" atime=\"" + file.getAtime() + "\" ctime=\"" + file.getCtime() + "\" mtime=\"" + file.getMtime()
                + "\" rights=\"" + file.getPerms() + "\" w32Attrs=\"" + file.getW32Attrs() + "\" readOnly=\""
                + file.isReadOnly() + "\">\n");

        // serialize the file's xLoc list
        XLocList xloc = file.getXLocList();
        if (xloc != null) {
            xmlWriter.write("<xlocList version=\"" + xloc.getVersion() + "\" ruPolicy=\"" + xloc.getReplUpdatePolicy()
                    + "\">\n");
            for (int i = 0; i < xloc.getReplicaCount(); i++) {
                XLoc repl = xloc.getReplica(i);
                xmlWriter.write("<xloc pattern=\""
                        + OutputUtils.escapeToXML(Converter.stripingPolicyToString(repl.getStripingPolicy()))
                        + "\" replFlags=\"" + repl.getReplicationFlags() + "\">\n");
                for (int j = 0; j < repl.getOSDCount(); j++)
                    xmlWriter.write("<osd location=\"" + repl.getOSD(j) + "\"/>\n");
                xmlWriter.write("</xloc>\n");
            }
            xmlWriter.write("</xlocList>\n");
        }

        // serialize the file's ACL
        dumpACL(xmlWriter, sMan.getACL(file.getId()));

        // serialize the file's attributes
        dumpAttrs(xmlWriter, sMan.getXAttrs(file.getId()));

        xmlWriter.write("</file>\n");
    }

    private static void dumpAttrs(BufferedWriter xmlWriter, DatabaseResultSet<XAttr> attrs) throws IOException {

        if (attrs.hasNext()) {
            xmlWriter.write("<attrs>\n");

            while (attrs.hasNext()) {

                XAttr attr = attrs.next();
                String key = attr.getKey();
                byte[] value = attr.getValue();

                xmlWriter.write("<attr key=\"" + OutputUtils.escapeToXML(key) + "\" value=\""
                        + OutputUtils.encodeBase64(value) + "\" enc=\"BASE64\" owner=\"" + attr.getOwner() + "\"/>\n");
            }
            xmlWriter.write("</attrs>\n");
        }

        attrs.destroy();
    }

    private static void dumpACL(BufferedWriter xmlWriter, DatabaseResultSet<ACLEntry> acl) throws IOException {

        if (acl.hasNext()) {
            xmlWriter.write("<acl>\n");
            while (acl.hasNext()) {
                ACLEntry entry = acl.next();
                xmlWriter.write("<entry entity=\"" + OutputUtils.escapeToXML(entry.getEntity()) + "\" rights=\""
                        + entry.getRights() + "\"/>\n");
            }
            xmlWriter.write("</acl>\n");
        }

        acl.destroy();
    }
}
