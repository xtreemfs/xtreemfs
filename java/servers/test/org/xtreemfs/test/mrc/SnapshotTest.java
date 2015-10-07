/*
 * Copyright (c) 2010-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.mrc;

import java.io.IOException;
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
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntries;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntry;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.timestampResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestEnvironment.Services;
import org.xtreemfs.test.TestHelper;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

/**
 * XtreemFS MRC test
 * 
 * @author stender
 */
public class SnapshotTest {
    @Rule
    public final TestRule testLog = TestHelper.testLog;

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

    private MRCServiceClient  client;

    private InetSocketAddress mrcAddress;

    private String            uid;

    private List<String>      gids;

    private UserCredentials   uc;

    private TestEnvironment   testEnv;

    public SnapshotTest() {
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }

    @Before
    public void setUp() throws Exception {
        uid = "userXY";
        gids = createGIDs("groupZ");

        uc = createUserCredentials(uid, gids);

        // register an OSD at the directory service (needed in order to assign
        // it to a new file on 'open')

        testEnv = new TestEnvironment(Services.DIR_CLIENT, Services.TIME_SYNC, Services.UUID_RESOLVER,
                Services.MRC_CLIENT, Services.DIR_SERVICE, Services.MRC, Services.OSD);
        testEnv.start();

        mrcAddress = testEnv.getMRCAddress();
        client = testEnv.getMrcClient();
    }

    @After
    public void tearDown() throws Exception {
        testEnv.shutdown();
        Logging.logMessage(Logging.LEVEL_DEBUG, this, BufferPool.getStatus());
    }

    @Test
    public void testSnapshots() throws Exception {

        final int maxFilesPerDir = 4;
        final int numDirs = 20;
        final String volumeName = "testVolume";

        final int numSnaps = 50;

        // create a volume
        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, getDefaultStripingPolicy(), "", 0775,
                volumeName, "", "", new LinkedList<KeyValuePair>(), 0, 0));

        // enable snapshots on the volume
        invokeSync(client.setxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "",
                "xtreemfs.snapshots_enabled", "", ByteString.copyFrom("true".getBytes()), 0));

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
                invokeSync(client.mkdir(mrcAddress, RPCAuthentication.authNone, uc, volumeName,
                        path.getName(), 0775));
            else
                invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName,
                        path.getName(), FileAccessManager.O_CREAT, 0775, 0, getDefaultCoordinates()));

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
                invokeSync(client.rmdir(mrcAddress, RPCAuthentication.authNone, uc, volumeName,
                        path.getName()));
            else
                invokeSync(client.unlink(mrcAddress, RPCAuthentication.authNone, uc, volumeName,
                        path.getName()));

        TreeSet<TreeEntry> emptyTree = new TreeSet<TreeEntry>();

        // check the empty tree
        assertTree(emptyTree, volumeName, "", true);

        // check the old snapshot trees
        for (Entry<Integer, Object[]> snap : snaps.entrySet())
            assertTree(tree, volumeName + "@" + snap.getKey(), (String) snap.getValue()[0],
                    (Boolean) snap.getValue()[1]);
    }

    private RPCResponse<timestampResponse> createSnapshot(String vol, String dir, String name,
            boolean recursive) throws Exception {
        String cmd = "c" + (recursive ? "r" : "") + " " + name;
        return client.setxattr(mrcAddress, RPCAuthentication.authNone, uc, vol, dir, "xtreemfs.snapshots",
                "", ByteString.copyFrom(cmd.getBytes()), 0);
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

            invokeSync(client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volume, relPath, -1));

        }

        checkDir(volume, subtree, "", path, recursive);
    }

    private void checkDir(String volume, SortedSet<TreeEntry> subtree, String relPath, String fullPath,
            boolean recursive) throws Exception {

        DirectoryEntries entries = invokeSync(client.readdir(mrcAddress, RPCAuthentication.authNone, uc,
                volume, relPath, -1, 1000, false, 0));
        for (DirectoryEntry entry : entries.getEntriesList()) {

            boolean isDir = (entry.getStbuf().getMode() & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFDIR.getNumber()) > 0;

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

    private static StripingPolicy getDefaultStripingPolicy() {
        return StripingPolicy.newBuilder().setType(StripingPolicyType.STRIPING_POLICY_RAID0)
                .setStripeSize(1000).setWidth(1).build();
    }

    private static List<String> createGIDs(String gid) {
        List<String> list = new LinkedList<String>();
        list.add(gid);
        return list;
    }

    private static <T extends Message> T invokeSync(RPCResponse<T> response) throws PBRPCException,
            IOException, InterruptedException {

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

    private static UserCredentials createUserCredentials(String uid, List<String> gids) {
        return UserCredentials.newBuilder().setUsername(uid).addAllGroups(gids).build();
    }

    private static VivaldiCoordinates getDefaultCoordinates() {
        return VivaldiCoordinates.newBuilder().setXCoordinate(0).setYCoordinate(0).setLocalError(0).build();
    }
}
