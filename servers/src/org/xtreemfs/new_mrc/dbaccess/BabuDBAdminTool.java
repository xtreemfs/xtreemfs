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

import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.new_mrc.metadata.ACLEntry;
import org.xtreemfs.new_mrc.metadata.FileMetadata;
import org.xtreemfs.new_mrc.metadata.XAttr;
import org.xtreemfs.new_mrc.metadata.XLoc;
import org.xtreemfs.new_mrc.metadata.XLocList;
import org.xtreemfs.new_mrc.utils.Converter;

public class BabuDBAdminTool {
    
    public static void dumpVolume(BufferedWriter xmlWriter, BabuDBStorageManager sMan)
        throws IOException, DatabaseException {
        try {
            dumpDir(xmlWriter, sMan, 1);
        } catch (JSONException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    private static void dumpDir(BufferedWriter xmlWriter, BabuDBStorageManager sMan, long parentId)
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
    
    private static void dumpFile(BufferedWriter xmlWriter, BabuDBStorageManager sMan,
        FileMetadata file) throws DatabaseException, IOException, JSONException {
        
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
                    + OutputUtils.escapeToXML(JSONParser.writeJSON(Converter.stripingPolicyToMap(repl.getStripingPolicy())))
                    + "\">\n");
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
