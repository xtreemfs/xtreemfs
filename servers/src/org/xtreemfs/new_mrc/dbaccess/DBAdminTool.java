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
package org.xtreemfs.new_mrc.dbaccess;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.new_mrc.UserException;
import org.xtreemfs.new_mrc.ac.FileAccessManager;
import org.xtreemfs.new_mrc.metadata.ACLEntry;
import org.xtreemfs.new_mrc.metadata.FileMetadata;
import org.xtreemfs.new_mrc.metadata.XAttr;
import org.xtreemfs.new_mrc.metadata.XLoc;
import org.xtreemfs.new_mrc.metadata.XLocList;
import org.xtreemfs.new_mrc.utils.Converter;
import org.xtreemfs.new_mrc.volumes.VolumeManager;
import org.xtreemfs.new_mrc.volumes.metadata.VolumeInfo;

public class DBAdminTool {
    
    public static class DBRestoreState {
        
        public VolumeInfo   currentVolume;
        
        public List<Long>   parentIds;
        
        public FileMetadata currentEntity;
        
        public XLoc         currentXLoc;
        
        public DBRestoreState() {
            parentIds = new LinkedList<Long>();
            parentIds.add((long) 0);
        }
        
    }
    
    /**
     * Creates an XML dump from a volume. The dump contains all files and
     * directories of the volume, including their attributes and ACLs.
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
    public static void dumpVolume(BufferedWriter xmlWriter, BabuDBStorageManager sMan)
        throws IOException, DatabaseException {
        try {
            dumpDir(xmlWriter, sMan, 1);
        } catch (JSONException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    public static void restoreDir(VolumeManager vMan, FileAccessManager faMan, String entity,
        Attributes attrs, DBRestoreState state, int dbVersion, boolean openTag)
        throws DatabaseException, UserException {
        
        if (openTag) {
            
            StorageManager sMan = vMan.getStorageManager(state.currentVolume.getId());
            
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
            
            // if the directory is the root directory, create the volume
            // first
            if (id == 1) {
                vMan.createVolume(faMan, state.currentVolume.getId(),
                    state.currentVolume.getName(), state.currentVolume.getAcPolicyId(),
                    state.currentVolume.getOsdPolicyId(), state.currentVolume.getOsdPolicyArgs(),
                    owner, owningGroup, null, null, null);
                sMan = vMan.getStorageManager(state.currentVolume.getId());
            }
            
            // there must not be hard links to directories, so it is not
            // necessary to check if the directory exists already
            
            AtomicDBUpdate update = sMan.createAtomicDBUpdate(null, null);
            FileMetadata dir = sMan.createDir(id, state.parentIds.get(0), name, atime, ctime,
                mtime, owner, owningGroup, rights, w32Attrs, update);
            update.execute();
            
            state.parentIds.add(0, id);
            state.currentEntity = dir;
        }

        else
            state.parentIds.remove(0);
    }
    
    public static void restoreFile(VolumeManager vMan, FileAccessManager faMan, String entity,
        Attributes attrs, DBRestoreState state, int dbVersion, boolean openTag)
        throws DatabaseException {
        
        if (!openTag)
            return;
        
        long id = Long.parseLong(attrs.getValue(attrs.getIndex("id")));
        String name = OutputUtils.unescapeFromXML(attrs.getValue(attrs.getIndex("name")));
        
        StorageManager sMan = vMan.getStorageManager(state.currentVolume.getId());
        
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
            
            short rights = 511; // set all rights to 511 by default
            if (attrs.getIndex("rights") != -1)
                rights = Short.parseShort(attrs.getValue(attrs.getIndex("rights")));
            
            long w32Attrs = 0; // set all Win32 attributes to 0 by default
            if (attrs.getIndex("w32Attrs") != -1)
                w32Attrs = Long.parseLong(attrs.getValue(attrs.getIndex("w32Attrs")));
            
            boolean readOnly = false; // set readOnly attribute to false by
            // default
            if (attrs.getIndex("readOnly") != -1)
                w32Attrs = Long.parseLong(attrs.getValue(attrs.getIndex("readOnly")));
            
            AtomicDBUpdate update = sMan.createAtomicDBUpdate(null, null);
            file = sMan.createFile(id, state.parentIds.get(0), name, atime, ctime, mtime, owner,
                owningGroup, rights, w32Attrs, size, readOnly, epoch, issuedEpoch, update);
            update.execute();
        }

        // otherwise, create a link
        else {
            // TODO
            // linkFile(name, id, state.parentIds.get(0));
        }
        
        state.currentEntity = file;
    }
    
    public static void restoreXLocList(VolumeManager vMan, FileAccessManager faMan, String entity,
        Attributes attrs, DBRestoreState state, int dbVersion, boolean openTag)
        throws DatabaseException {
        
        if (!openTag)
            return;
        
        StorageManager sMan = vMan.getStorageManager(state.currentVolume.getId());
        
        int version = Integer.parseInt(attrs.getValue(attrs.getIndex("version")));
        state.currentEntity.setXLocList(sMan.createXLocList(null, version));
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(null, null);
        sMan.setMetadata(state.currentEntity, FileMetadata.XLOC_METADATA, update);
        update.execute();
    }
    
    public static void restoreXLoc(VolumeManager vMan, FileAccessManager faMan, String entity,
        Attributes attrs, DBRestoreState state, int dbVersion, boolean openTag)
        throws DatabaseException {
        
        if (!openTag)
            return;
        
        StorageManager sMan = vMan.getStorageManager(state.currentVolume.getId());
        
        String pattern = attrs.getValue(attrs.getIndex("pattern"));
        StringTokenizer st = new StringTokenizer(pattern, " ,");
        String policy = st.nextToken();
        int size = Integer.parseInt(st.nextToken());
        int width = Integer.parseInt(st.nextToken());
        
        XLocList xLocList = state.currentEntity.getXLocList();
        state.currentXLoc = sMan.createXLoc(sMan.createStripingPolicy(policy, size, width),
            new String[0]);
        // TODO: add replica
    }
    
    public static void restoreOSD(VolumeManager vMan, FileAccessManager faMan, String entity,
        Attributes attrs, DBRestoreState state, int dbVersion, boolean openTag)
        throws DatabaseException {
        
        if (!openTag)
            return;
        
        // String osd = attrs.getValue(attrs.getIndex("location"));
        //        
        // String[] osdList = state.currentXLoc.getOsdList();
        // if (osdList == null)
        // osdList = new String[] { osd };
        // else {
        // String[] newOSDList = new String[osdList.length + 1];
        // System.arraycopy(osdList, 0, newOSDList, 0, osdList.length);
        // newOSDList[newOSDList.length - 1] = osd;
        // osdList = newOSDList;
        // }
        //        
        // state.currentXLoc.setOsdList(osdList);
    }
    
    public static void restoreEntry(VolumeManager vMan, FileAccessManager faMan, String entity,
        Attributes attrs, DBRestoreState state, int dbVersion, boolean openTag)
        throws DatabaseException {
        
        if (!openTag)
            return;
        
        // String userId = attrs.getValue(attrs.getIndex("entity"));
        // long rights =
        // Long.parseLong(attrs.getValue(attrs.getIndex("rights")));
        //        
        // ACLEntry[] acl = state.currentEntity.getAcl();
        // if (acl == null)
        // acl = new ACLEntry[] { new ACLEntry(userId, rights) };
        // else {
        // ACLEntry[] newACL = new ACLEntry[acl.length + 1];
        // System.arraycopy(acl, 0, newACL, 0, acl.length);
        // newACL[newACL.length - 1] = new ACLEntry(userId, rights);
        // acl = newACL;
        // }
        //        
        // state.currentEntity.setAcl(acl);
        // if (state.currentEntity instanceof FileEntity)
        // backend.put((FileEntity) state.currentEntity);
        // else
        // backend.put((DirEntity) state.currentEntity);
        
    }
    
    public static void restoreAttr(VolumeManager vMan, FileAccessManager faMan, String entity,
        Attributes attrs, DBRestoreState state, int dbVersion, boolean openTag)
        throws DatabaseException {
        
        if (!openTag)
            return;
        
        // String key =
        // OutputUtils.unescapeFromXML(attrs.getValue(attrs.getIndex("key")));
        // Object value =
        // OutputUtils.unescapeFromXML(attrs.getValue(attrs.getIndex("value")));
        //        
        // // if the value refers to a striping policy, parse it
        // if (key.equals("spol")) {
        // StringTokenizer st = new StringTokenizer(value.toString(), ", ");
        // value = new StripingPolicy(st.nextToken(),
        // Long.parseLong(st.nextToken()), Long
        // .parseLong(st.nextToken()));
        // } else if (key.equals("ro"))
        // value = Boolean.valueOf((String) value);
        //        
        // long type = Long.parseLong(attrs.getValue(attrs.getIndex("type")));
        // String uid = attrs.getValue(attrs.getIndex("uid"));
        //        
        // backend.put(new FileAttributeEntity(key, value, type,
        // state.currentEntity.getId(), uid));
    }
    
    private static void dumpDir(BufferedWriter xmlWriter, StorageManager sMan, long parentId)
        throws DatabaseException, IOException, JSONException {
        
        FileMetadata parent = sMan.getMetadata(parentId);
        
        // serialize the parent directory
        xmlWriter.write("<dir id=\"" + parent.getId() + "\" name=\""
            + OutputUtils.escapeToXML(parent.getFileName()) + "\" uid=\"" + parent.getOwnerId()
            + "\" gid=\"" + parent.getOwningGroupId() + "\" atime=\"" + parent.getAtime()
            + "\" ctime=\"" + parent.getCtime() + "\" mtime=\"" + parent.getMtime()
            + "\" rights=\"" + parent.getPerms() + "\" w32Attrs=\"" + parent.getW32Attrs()
            + "\">\n");
        
        // serialize the root directory's ACL
        dumpACL(xmlWriter, sMan.getACL(parentId));
        
        // serialize the root directory's attributes
        dumpAttrs(xmlWriter, sMan.getXAttrs(parentId));
        
        // serialize all nested elements
        Iterator<FileMetadata> children = sMan.getChildren(parentId);
        while (children.hasNext()) {
            
            FileMetadata child = children.next();
            
            if (child.isDirectory())
                dumpDir(xmlWriter, sMan, child.getId());
            else
                dumpFile(xmlWriter, sMan, child);
        }
        
        xmlWriter.write("</dir>\n");
    }
    
    private static void dumpFile(BufferedWriter xmlWriter, StorageManager sMan, FileMetadata file)
        throws DatabaseException, IOException, JSONException {
        
        // serialize the file
        xmlWriter.write("<file id=\"" + file.getId() + "\" name=\""
            + OutputUtils.escapeToXML(file.getFileName()) + "\" size=\"" + file.getSize()
            + "\" epoch=\"" + file.getEpoch() + "\" issuedEpoch=\"" + file.getIssuedEpoch()
            + "\" uid=\"" + file.getOwnerId() + "\" gid=\"" + file.getOwningGroupId()
            + "\" atime=\"" + file.getAtime() + "\" ctime=\"" + file.getCtime() + "\" mtime=\""
            + file.getMtime() + "\" rights=\"" + file.getPerms() + "\" w32Attrs=\""
            + file.getW32Attrs() + "\" readOnly=\"" + file.isReadOnly() + "\">\n");
        
        // serialize the file's xLoc list
        XLocList xloc = file.getXLocList();
        if (xloc != null) {
            xmlWriter.write("<xlocList version=\"" + xloc.getVersion() + "\">\n");
            for (int i = 0; i < xloc.getReplicaCount(); i++) {
                XLoc repl = xloc.getReplica(i);
                xmlWriter.write("<xloc pattern=\""
                    + OutputUtils.escapeToXML(JSONParser.writeJSON(Converter
                            .stripingPolicyToMap(repl.getStripingPolicy()))) + "\">\n");
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
    
    private static void dumpAttrs(BufferedWriter xmlWriter, Iterator<XAttr> attrs)
        throws IOException {
        
        if (attrs.hasNext()) {
            xmlWriter.write("<attrs>\n");
            while (attrs.hasNext()) {
                XAttr attr = attrs.next();
                xmlWriter.write("<attr key=\"" + OutputUtils.escapeToXML(attr.getKey())
                    + "\" value=\"" + OutputUtils.escapeToXML(attr.getValue()) + "\" owner=\""
                    + attr.getOwner() + "\"/>\n");
            }
            xmlWriter.write("</attrs>\n");
        }
    }
    
    private static void dumpACL(BufferedWriter xmlWriter, Iterator<ACLEntry> acl)
        throws IOException {
        
        if (acl.hasNext()) {
            xmlWriter.write("<acl>\n");
            while (acl.hasNext()) {
                ACLEntry entry = acl.next();
                xmlWriter.write("<entry entity=\"" + entry.getEntity() + "\" rights=\""
                    + entry.getRights() + "\"/>\n");
            }
            xmlWriter.write("</acl>\n");
        }
    }
    
}
