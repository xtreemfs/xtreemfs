/*  Copyright (c) 2010 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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

package org.xtreemfs.test.mrc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.DirectoryEntry;
import org.xtreemfs.interfaces.DirectoryEntrySet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.VivaldiCoordinates;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.ac.POSIXFileAccessPolicy;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestEnvironment.Services;

/**
 * XtreemFS MRC test
 * 
 * @author stender
 */
public class SnapshotTest extends TestCase {
    
    static class TreeEntry implements Comparable<TreeEntry>, Serializable {
        
        private String  name;
        
        private boolean dir;
        
        public TreeEntry(String name, boolean dir) {
            this.name = name;
            this.dir = dir;
        }
        
        public String getName() {
            return name;
        }
        
        public boolean isDir() {
            return dir;
        }
        
        public String toString() {
            return name;
        }
        
        @Override
        public int compareTo(TreeEntry o) {
            return name.compareTo(o.name);
        }
        
    }
    
    private static Random     rnd = new Random();
    
    private MRCClient         client;
    
    private InetSocketAddress mrcAddress;
    
    private String            uid;
    
    private List<String>      gids;
    
    private UserCredentials   uc;
    
    private TestEnvironment   testEnv;
    
    public SnapshotTest() {
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }
    
    protected void setUp() throws Exception {
        
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        
        mrcAddress = SetupUtils.getMRC1Addr();
        
        uid = "userXY";
        gids = createGIDs("groupZ");
        
        uc = MRCClient.getCredentials(uid, gids);
        
        // register an OSD at the directory service (needed in order to assign
        // it to a new file on 'open')
        
        testEnv = new TestEnvironment(Services.DIR_CLIENT, Services.TIME_SYNC, Services.UUID_RESOLVER,
            Services.MRC_CLIENT, Services.DIR_SERVICE, Services.MRC, Services.MOCKUP_OSD,
            Services.MOCKUP_OSD2, Services.MOCKUP_OSD3);
        testEnv.start();
        
        client = testEnv.getMrcClient();
    }
    
    protected void tearDown() throws Exception {
        testEnv.shutdown();
        Logging.logMessage(Logging.LEVEL_DEBUG, this, BufferPool.getStatus());
    }
    
    public void testSnapshots() throws Exception {
        
        final int maxFilesPerDir = 4;
        final int numDirs = 20;
        final String volumeName = "testVolume";
        
        final int numSnaps = 50;
        
        // create a volume
        invokeSync(client.mkvol(mrcAddress, uc, volumeName, getDefaultStripingPolicy(),
            POSIXFileAccessPolicy.POLICY_ID, 0775));
        
        // enable snapshots on the volume
        invokeSync(client.setxattr(mrcAddress, uc, volumeName, "", "xtreemfs.snapshots_enabled", "true", 0));
        
        // create a random tree with some files and directories
        SortedSet<TreeEntry> tree = createRandomTree(numDirs, maxFilesPerDir);
        
        // ObjectOutputStream out = new ObjectOutputStream(new
        // FileOutputStream("/tmp/tree.bin"));
        // out.writeObject(tree);
        // out.close();
        
        // ObjectInputStream in = new ObjectInputStream(new
        // FileInputStream("/tmp/tree.bin"));
        // tree = (SortedSet) in.readObject();
        // in.close();
        
        for (TreeEntry path : tree)
            if (path.isDir())
                invokeSync(client.mkdir(mrcAddress, uc, volumeName, path.getName(), 0775));
            else
                invokeSync(client.open(mrcAddress, uc, volumeName, path.getName(), FileAccessManager.O_CREAT,
                    0775, 0, new VivaldiCoordinates()));
        
        assertTree(tree, volumeName, "", true);
        
        // create and test a recursive snapshot from the root directory
        invokeSync(createSnapshot(volumeName, "", "rootSnap1", true));
        assertTree(tree, volumeName + "@rootSnap1", "", true);
        
        // create and test a non-recursive snapshot from the root directory
        invokeSync(createSnapshot(volumeName, "", "rootSnap2", false));
        assertTree(tree, volumeName + "@rootSnap2", "", false);
        
        // create and test some random snapshots
        Map<Integer, Object[]> snaps = new HashMap<Integer, Object[]>();
        for (int i = 0; i < numSnaps; i++) {
            
            String dir = getRandomDir(tree);
            boolean recursive = rnd.nextBoolean();
            
            snaps.put(i, new Object[] { dir, recursive });
            
            invokeSync(createSnapshot(volumeName, dir, i + "", recursive));
            assertTree(tree, volumeName + "@" + i, dir, recursive);
        }
        
        // delete everything
        
        ArrayList<TreeEntry> entries = new ArrayList<TreeEntry>(tree);
        Collections.sort(entries, new Comparator<TreeEntry>() {
            public int compare(TreeEntry o1, TreeEntry o2) {
                return o2.getName().length() - o1.getName().length();
            }
        });
        
        for (TreeEntry path : entries)
            if (path.isDir())
                invokeSync(client.rmdir(mrcAddress, uc, volumeName, path.getName()));
            else
                invokeSync(client.unlink(mrcAddress, uc, volumeName, path.getName()));
        
        TreeSet<TreeEntry> emptyTree = new TreeSet<TreeEntry>();
        
        // check the empty tree
        assertTree(emptyTree, volumeName, "", true);
        
        // check the old snapshot trees
        for (Entry<Integer, Object[]> snap : snaps.entrySet())
            assertTree(tree, volumeName + "@" + snap.getKey(), (String) snap.getValue()[0], (Boolean) snap
                    .getValue()[1]);
    }
    
    private RPCResponse<Object> createSnapshot(String vol, String dir, String name, boolean recursive) {
        return client.setxattr(mrcAddress, uc, vol, dir, "xtreemfs.snapshots", "c" + (recursive ? "r" : "")
            + " " + name, 0);
    }
    
    private void assertTree(SortedSet<TreeEntry> tree, String volume, String path, boolean recursive)
        throws Exception {
        
        SortedSet<TreeEntry> subtree = getSubtree(tree, path, recursive);
        
        int offs = 0;
        for (TreeEntry entry : subtree) {
            
            if (path.equals(entry.getName()))
                offs = entry.getName().length();
            
            String relPath = entry.getName().substring(offs);
            if (relPath.startsWith("/"))
                relPath = relPath.substring(1);
            
            invokeSync(client.getattr(mrcAddress, uc, volume, relPath));
            
        }
        
        checkDir(volume, subtree, "", path, recursive);
    }
    
    private void checkDir(String volume, SortedSet<TreeEntry> subtree, String relPath, String fullPath,
        boolean recursive) throws Exception {
        
        DirectoryEntrySet entries = invokeSync(client.readdir(mrcAddress, uc, volume, relPath));
        for (DirectoryEntry entry : entries) {
            
            boolean isDir = (entry.getStbuf().get(0).getMode() & Constants.SYSTEM_V_FCNTL_H_S_IFDIR) > 0;
            
            if (!entry.getName().equals(".") && !entry.getName().equals("..")) {
                
                if (!subtree.contains(new TreeEntry(fullPath + (fullPath.equals("") ? "" : "/")
                    + entry.getName(), isDir)))
                    throw new Exception(entry.getName() + " not contained in subtree");
                
                if (recursive && isDir)
                    checkDir(volume, subtree, relPath + (relPath.equals("") ? "" : "/") + entry.getName(),
                        fullPath + (fullPath.equals("") ? "" : "/") + entry.getName(), recursive);
                
            }
            
        }
    }
    
    public static void main(String[] args) {
        TestRunner.run(SnapshotTest.class);
    }
    
    private static StripingPolicy getDefaultStripingPolicy() {
        return new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 1000, 1);
    }
    
    private static List<String> createGIDs(String gid) {
        List<String> list = new LinkedList<String>();
        list.add(gid);
        return list;
    }
    
    private static <T> T invokeSync(RPCResponse<T> response) throws ONCRPCException, IOException,
        InterruptedException {
        
        try {
            return response.get();
        } finally {
            response.freeBuffers();
        }
    }
    
    private static String createRandomString(int minLength, int maxLength) {
        
        char[] chars = new char[(int) (rnd.nextDouble() * (maxLength + 1)) + minLength];
        for (int i = 0; i < chars.length; i++)
            chars[i] = (char) (rnd.nextDouble() * 26 + 65);
        
        return new String(chars);
    }
    
    private static SortedSet<TreeEntry> createRandomTree(int numDirs, int maxFilesPerDir) {
        
        SortedSet<TreeEntry> tree = new TreeSet<TreeEntry>();
        
        // create the directory tree
        String currentPath = "";
        for (int i = 0; i < numDirs; i++) {
            
            if ("".equals(currentPath) || rnd.nextBoolean())
                currentPath = createRandomString(1, 10);
            else
                currentPath = currentPath + "/" + createRandomString(1, 10);
            
            tree.add(new TreeEntry(currentPath, true));
        }
        
        // populate the directory tree with files
        List<TreeEntry> files = new LinkedList<TreeEntry>();
        for (TreeEntry entry : tree)
            for (int i = 0; i < rnd.nextDouble() * (maxFilesPerDir + 1); i++)
                files.add(new TreeEntry(entry.getName() + "/" + createRandomString(1, 10) + ".xyz", false));
        
        tree.addAll(files);
        return tree;
    }
    
    private static SortedSet<TreeEntry> modifyTree(SortedSet<TreeEntry> originalTree, double deleteRate,
        double createRate, double dirCreateRate, int maxFilesPerDir) {
        
        SortedSet<TreeEntry> result = new TreeSet<TreeEntry>(originalTree.comparator());
        for (TreeEntry entry : originalTree) {
            int index = entry.getName().lastIndexOf('/');
            if (index == -1)
                index = 0;
            if (!entry.isDir() && !result.contains(new TreeEntry(entry.getName().substring(0, index), true)))
                continue;
            if (rnd.nextDouble() >= deleteRate) {
                result.add(entry);
                if (entry.isDir()) {
                    if (rnd.nextDouble() <= dirCreateRate) {
                        result.add(new TreeEntry(createRandomString(1, 10), true));
                        for (int i = 0; i < (int) (rnd.nextDouble() * 5); i++)
                            result.add(new TreeEntry(createRandomString(1, 10) + ".xyz", false));
                    } else if (rnd.nextDouble() < createRate)
                        result.add(new TreeEntry(createRandomString(1, 10) + ".xyz", false));
                }
            }
        }
        
        return result;
    }
    
    private static SortedSet<TreeEntry> getSubtree(SortedSet<TreeEntry> tree, String path, boolean recursive) {
        
        String[] pathComps = path.equals("") ? new String[0] : path.split("/");
        
        SortedSet<TreeEntry> subTree = new TreeSet<TreeEntry>(tree.comparator());
        
        for (TreeEntry entry : tree) {
            
            String[] comps = entry.getName().split("/");
            
            // include top-level dir
            if (equals(comps, pathComps)) {
                subTree.add(entry);
                
            } else if (recursive) {
                
                if (startsWith(comps, pathComps))
                    subTree.add(entry);
                
            } else {
                
                if (startsWith(comps, pathComps) && comps.length - pathComps.length == 1)
                    subTree.add(entry);
                
            }
            
        }
        
        return subTree;
    }
    
    private static boolean equals(String[] path1, String[] path2) {
        
        if (path1.length != path2.length)
            return false;
        
        for (int i = 0; i < path1.length; i++)
            if (!(path1[i].equals(path2[i])))
                return false;
        
        return true;
    }
    
    private static boolean startsWith(String[] path1, String[] path2) {
        
        if (path1.length < path2.length)
            return false;
        
        for (int i = 0; i < path2.length; i++)
            if (!(path2[i].equals(path1[i])))
                return false;
        
        return true;
    }
    
    private static String getRandomDir(SortedSet<TreeEntry> tree) {
        
        List<String> dirs = new ArrayList<String>(tree.size());
        Iterator<TreeEntry> it = tree.iterator();
        while (it.hasNext()) {
            TreeEntry entry = it.next();
            if (entry.isDir())
                dirs.add(entry.getName());
        }
        
        return dirs.get((int) (rnd.nextDouble() * dirs.size()));
    }
    
    private static void printTree(SortedSet<TreeEntry> tree) {
        for (TreeEntry entry : tree)
            System.out.println(entry);
    }
    
}
