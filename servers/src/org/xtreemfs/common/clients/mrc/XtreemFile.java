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

package org.xtreemfs.common.clients.mrc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.mrc.utils.MessageUtils;

/**
 *
 * @author bjko
 */
public class XtreemFile {

    public static String                                pathSeparator     = "/";

    public static char                                  pathSeparatorChar = '/';

    private final String                                volumeName;

    private final String                                filename;

    private Map<String, Object>                         statInfo;

    private MRCClient                                   client;

    private InetSocketAddress                           mrc;

    private final boolean                               isVolumeList;

    private final boolean                               invalidVolume;

    private final boolean                               isSysDir;

    private static final HashMap<String, VolCacheEntry> vcache            = new HashMap();

    /** Creates a new instance of XtreemFile */
    public XtreemFile(MRCClient client, InetSocketAddress dirService, String filename)
        throws IOException {
        System.out.println("created new file for: " + filename);
        this.client = client;
        // first extract the volume name
        String woPrefix = filename;
        if (filename.startsWith("/xtreemfs")) {
            woPrefix = filename.substring("/xtreemfs".length());
        }
        if (woPrefix.length() == 0)
            woPrefix = "/";
        int posSecondslash = woPrefix.substring(1).indexOf(pathSeparatorChar);
        if (posSecondslash == -1) {
            volumeName = woPrefix.substring(1);
            this.filename = "/";
        } else {
            volumeName = woPrefix.substring(1, posSecondslash + 1);
            this.filename = woPrefix.substring(posSecondslash + 1);
        }
        System.out.println("XtreemFile: voumeName=" + volumeName + "   filename=" + this.filename);

        if (volumeName.length() > 0) {
            // ask the dir service for the MRC holding the volume

            // check, if it is a system dir
            if (woPrefix.endsWith("/.") || woPrefix.endsWith("/..")) {
                isSysDir = true;
                statInfo = null;
                isVolumeList = false;
                invalidVolume = false;
            } else {
                // check my cache
                VolCacheEntry vci = vcache.get(this.volumeName);
                if (vci != null) {
                    if (vci.created > System.currentTimeMillis() + 1000 * 60) {
                        vci = null;
                    }
                }
                if (vci == null) {
                    ArrayList<Object> params = new ArrayList();
                    params.add(volumeName);
                    Object o = null;
                    // try {
                    // RPCResponse<Object> resp =
                    // client.sendGenericRequest(dirService,"getVolumeInfo",params);
                    // o = resp.get();
                    // } catch (JSONException ex) {
                    // throw new IOException("cannot encode/decode message",ex);
                    // }
                    // FIXME: adapt to new Directory Service
                    System.out.println("VVOLINFO is " + o);
                    if (o == null) {
                        invalidVolume = true;
                    } else {
                        Map<String, Object> volInfo = (Map) o;
                        Map<String, Object> mrcMap = (Map) volInfo.get("mrcMap");
                        mrc = MessageUtils.addrFromString((String) mrcMap.keySet().toArray()[0]);
                        vci = new VolCacheEntry();
                        vci.created = System.currentTimeMillis();
                        vci.volName = this.volumeName;
                        vci.mrc = mrc;
                        vcache.put(this.volumeName, vci);
                        invalidVolume = false;
                    }
                } else {
                    mrc = vci.mrc;
                    invalidVolume = false;
                }
                if (!invalidVolume) {
                    try {

                        // now we have a mrc..lets fetch the file details
                        statInfo = client.stat(mrc, this.volumeName + this.filename, true, true,
                            true, NullAuthProvider.createAuthString("1", "1"));
                        System.out.println("STAT INFO:" + statInfo);
                    } catch (Exception ex) {
                        Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
                        statInfo = null;
                    } /*
                         * catch (IOException ex) { ex.printStackTrace();
                         * statInfo = null; }
                         */
                }
                isVolumeList = false;
                isSysDir = false;
            }
        } else {
            isSysDir = false;
            isVolumeList = true;
            invalidVolume = false;
            mrc = dirService;
        }
    }

    public Map getStatInfo() {
        return statInfo;
    }

    public boolean isDirectory() {
        if (isSysDir)
            return true;
        if (isVolumeList)
            return true;
        if (statInfo == null) {
            System.out.println("no stat info");
            return false;
        }
        Long oType = (Long) statInfo.get("objType");
        System.out.println("isDir= " + (oType == 2) + " type=" + oType);
        return (oType == 2);
    }

    public boolean isFile() {
        if (isSysDir)
            return false;
        if (isVolumeList)
            return false;
        if (statInfo == null) {
            System.out.println("no stat info");
            return false;
        }
        Long oType = (Long) statInfo.get("objType");
        System.out.println("isFile= " + (oType == 1) + " type=" + oType);
        return (oType == 1);
    }

    public boolean delete() {
        if (isSysDir)
            return false;
        if (isVolumeList)
            return false;
        try {
            client.delete(mrc, this.volumeName + this.filename, NullAuthProvider.createAuthString(
                "1", "1"));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public long length() {
        if (isSysDir)
            return 0l;
        if (isVolumeList)
            return 0l;
        if (statInfo == null)
            return 0l;
        return (Long) statInfo.get("size");
    }

    public String toString() {
        return this.volumeName + this.filename;
    }

    public boolean exists() {
        if (isSysDir)
            return true;
        if (isVolumeList && invalidVolume)
            return false;
        if (isVolumeList)
            return true;
        return (statInfo != null);
    }

    public boolean renameTo(XtreemFile dest) {
        if (isSysDir)
            return false;
        if (isVolumeList)
            return false;
        if (statInfo == null)
            return false;

        try {
            client.move(mrc, this.volumeName + this.filename, dest.volumeName + dest.filename,
                NullAuthProvider.createAuthString("1", "1"));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean canExecute() {
        if (isSysDir)
            return false;
        if (isVolumeList)
            return false;
        if (statInfo == null)
            return false;
        Long posixAccessMode = (Long) statInfo.get("posixAccessMode");
        return (posixAccessMode.intValue() & 64) > 0;
    }

    public boolean canRead() {
        if (isSysDir)
            return true;
        if (isVolumeList)
            return true;
        if (statInfo == null)
            return false;
        Long posixAccessMode = (Long) statInfo.get("posixAccessMode");
        return (posixAccessMode.intValue() & 256) > 0;
    }

    public boolean canWrite() {
        if (isSysDir)
            return false;
        if (isVolumeList)
            return false;
        if (statInfo == null)
            return false;
        Long posixAccessMode = (Long) statInfo.get("posixAccessMode");
        return (posixAccessMode.intValue() & 128) > 0;
    }

    public long lastModified() {
        if (isSysDir)
            return System.currentTimeMillis();
        if (isVolumeList)
            return 0l;
        if (statInfo == null)
            return 0l;
        Long ll = (Long) statInfo.get("mtime") * 1000;
        return ll;
    }

    public String[] list() {
        if (isSysDir)
            return null;
        if (isVolumeList) {
            // list volumes...
            Object o = null;
            try {
                RPCResponse resp = client.sendRPC(mrc, "getVolumeInfos", new ArrayList(),
                    NullAuthProvider.createAuthString("1", "1"), null);
                o = resp.get();
            } catch (Exception ex) {
                System.out.println("cannot get volumes: " + ex);
                return null;
            }
            List<Object> vols = (List) o;
            List<String> volNames = new LinkedList();
            for (Object vol : vols) {
                Map<String, Object> mrcMap = (Map) vol;
                volNames.add((String) mrcMap.get("name"));
            }
            return volNames.toArray(new String[0]);
        } else {
            if (isDirectory() == false)
                return null;
            try {

                List<String> entries = client.readDir(mrc, this.volumeName + this.filename,
                    NullAuthProvider.createAuthString("1", "1"));
                return entries.toArray(new String[0]);
            } catch (Exception ex) {
                Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
                return null;
            }
        }
    }

    public MiniStatInfo[] listAndStat() {
        if (isSysDir)
            return null;
        if (isVolumeList) {
            // list volumes...
            Object o = null;
            try {
                RPCResponse resp = client.sendRPC(mrc, "getVolumeInfos", new ArrayList(),
                    NullAuthProvider.createAuthString("1", "1"), null);
                o = resp.get();
            } catch (Exception ex) {
                System.out.println("cannot get volumes: " + ex);
                return null;
            }
            List<Object> vols = (List) o;
            List<MiniStatInfo> volNames = new LinkedList();
            for (Object vol : vols) {
                Map<String, Object> mrcMap = (Map) vol;
                MiniStatInfo mi = new MiniStatInfo();
                mi.type = "vol";
                mi.name = (String) mrcMap.get("name");
                volNames.add(mi);
            }
            return volNames.toArray(new MiniStatInfo[0]);
        } else {
            if (isDirectory() == false)
                return null;
            try {

                Map<String, Map<String, Object>> entries = client.readDirAndStat(mrc,
                    this.volumeName + this.filename, NullAuthProvider.createAuthString("1", "1"));
                List<MiniStatInfo> dir = new LinkedList();
                for (String entry : entries.keySet()) {
                    MiniStatInfo mi = new MiniStatInfo();
                    Long otype = (Long) entries.get(entry).get("objType");
                    if (otype == 1)
                        mi.type = "file";
                    else
                        mi.type = "dir";
                    mi.name = entry;
                    dir.add(mi);
                }
                return dir.toArray(new MiniStatInfo[0]);
            } catch (Exception ex) {
                Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
                return null;
            }
        }
    }

    public boolean mkdir() {
        if (isSysDir)
            return false;
        if (isVolumeList)
            return false;
        try {
            client.createDir(mrc, this.volumeName + this.filename, NullAuthProvider
                    .createAuthString("1", "1"));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean touch() {
        if (isSysDir)
            return false;
        if (isVolumeList)
            return false;
        try {
            client.createFile(mrc, this.volumeName + this.filename, NullAuthProvider
                    .createAuthString("1", "1"));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public byte[] read(long start, long numBytes) throws IOException {
        try {
            // FIXME:not finished yet
            Map<String, String> capability = client.open(mrc, this.volumeName + this.filename, "r",
                NullAuthProvider.createAuthString("1", "1"));
        } catch (Exception ex) {
            throw new IOException(ex);
        }

        // OSDClient oc = new OSDClient();

        return new byte[0];
    }

    public boolean write(long start, long numBytes) {
        return true;
    }

    public static class VolCacheEntry {
        public long              created;

        public String            volName;

        public InetSocketAddress mrc;
    }

    public static class MiniStatInfo {
        public String type;

        public String name;
    }

}
