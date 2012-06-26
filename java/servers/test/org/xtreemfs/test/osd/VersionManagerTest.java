/*
 * Copyright (c) 2010-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.osd;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.osd.storage.FileVersionLog;
import org.xtreemfs.osd.storage.VersionManager;
import org.xtreemfs.osd.storage.VersionManager.ObjectVersionInfo;

/**
 * 
 * @author stender
 */
public class VersionManagerTest extends TestCase {

    private static class OV {

        long objId;
        long timestamp;

        OV(long objId, long timestamp) {
            this.objId = objId;
            this.timestamp = timestamp;
        }
    }

    private static class FV {

        long timestamp;
        long numObjs;

        FV(long timestamp, long numObjs) {
            this.timestamp = timestamp;
            this.numObjs = numObjs;
        }
    }

    private static final File     VLOG_FILE  = new File("/tmp/vttest");
    private static final List<FV> F_VERSIONS = new ArrayList<FV>();
    private static final List<OV> O_VERSIONS = new ArrayList<OV>();

    static {
        F_VERSIONS.add(new FV(10000, 5));
        F_VERSIONS.add(new FV(10005, 5));
        F_VERSIONS.add(new FV(10020, 0));
        F_VERSIONS.add(new FV(11000, 2));
        F_VERSIONS.add(new FV(12000, 5));
        F_VERSIONS.add(new FV(32000, 10));

        O_VERSIONS.add(new OV(0, 1));
        O_VERSIONS.add(new OV(0, 5555));
        O_VERSIONS.add(new OV(0, 17773));
        O_VERSIONS.add(new OV(1, 1));
        O_VERSIONS.add(new OV(1, 11000));
        O_VERSIONS.add(new OV(1, 30000));
        O_VERSIONS.add(new OV(2, 11111));
    }

    public VersionManagerTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        VLOG_FILE.delete();
    }

    @Override
    protected void tearDown() throws Exception {
        VLOG_FILE.delete();
    }

    public void testFileVersionLogAppendLookup() throws Exception {

        FileVersionLog vlog = new FileVersionLog(VLOG_FILE);
        for (FV version : F_VERSIONS)
            vlog.appendVersion(version.timestamp, 0, 0);

        assertEquals(0, vlog.getLatestVersionBefore(9878).getTimestamp());
        assertEquals(F_VERSIONS.get(0).timestamp, vlog.getLatestVersionBefore(10001).getTimestamp());
        assertEquals(F_VERSIONS.get(1).timestamp, vlog.getLatestVersionBefore(10005).getTimestamp());
        assertEquals(F_VERSIONS.get(1).timestamp, vlog.getLatestVersionBefore(10012).getTimestamp());
        assertEquals(F_VERSIONS.get(1).timestamp, vlog.getLatestVersionBefore(10014).getTimestamp());
        assertEquals(F_VERSIONS.get(2).timestamp, vlog.getLatestVersionBefore(10200).getTimestamp());
        assertEquals(F_VERSIONS.get(2).timestamp, vlog.getLatestVersionBefore(10800).getTimestamp());
        assertEquals(F_VERSIONS.get(3).timestamp, vlog.getLatestVersionBefore(11001).getTimestamp());
        assertEquals(F_VERSIONS.get(4).timestamp, vlog.getLatestVersionBefore(12000).getTimestamp());
        assertEquals(F_VERSIONS.get(4).timestamp, vlog.getLatestVersionBefore(22000).getTimestamp());
        assertEquals(F_VERSIONS.get(5).timestamp, vlog.getLatestVersionBefore(99999).getTimestamp());
        assertEquals(F_VERSIONS.get(5).timestamp, vlog.getLatestVersionBefore(Long.MAX_VALUE).getTimestamp());
    }

    public void testFileVersionLogLoad() throws Exception {

        FileVersionLog vlog = new FileVersionLog(VLOG_FILE);
        for (FV version : F_VERSIONS)
            vlog.appendVersion(version.timestamp, 0, 0);
        vlog.load();

        assertEquals(0, vlog.getLatestVersionBefore(9878).getTimestamp());
        assertEquals(F_VERSIONS.get(0).timestamp, vlog.getLatestVersionBefore(10001).getTimestamp());
        assertEquals(F_VERSIONS.get(1).timestamp, vlog.getLatestVersionBefore(10005).getTimestamp());
        assertEquals(F_VERSIONS.get(1).timestamp, vlog.getLatestVersionBefore(10012).getTimestamp());
        assertEquals(F_VERSIONS.get(1).timestamp, vlog.getLatestVersionBefore(10014).getTimestamp());
        assertEquals(F_VERSIONS.get(2).timestamp, vlog.getLatestVersionBefore(10200).getTimestamp());
        assertEquals(F_VERSIONS.get(2).timestamp, vlog.getLatestVersionBefore(10800).getTimestamp());
        assertEquals(F_VERSIONS.get(3).timestamp, vlog.getLatestVersionBefore(11001).getTimestamp());
        assertEquals(F_VERSIONS.get(4).timestamp, vlog.getLatestVersionBefore(12000).getTimestamp());
        assertEquals(F_VERSIONS.get(4).timestamp, vlog.getLatestVersionBefore(22000).getTimestamp());
        assertEquals(F_VERSIONS.get(5).timestamp, vlog.getLatestVersionBefore(99999).getTimestamp());
        assertEquals(F_VERSIONS.get(5).timestamp, vlog.getLatestVersionBefore(Long.MAX_VALUE).getTimestamp());

        vlog.appendVersion(55555, 0, 0);
        vlog = new FileVersionLog(VLOG_FILE);
        vlog.load();

        assertEquals(55555, vlog.getLatestVersionBefore(Long.MAX_VALUE).getTimestamp());
    }

    public void testVersionManager() throws Exception {

        FileVersionLog vlog = new FileVersionLog(VLOG_FILE);
        for (FV fv : F_VERSIONS)
            vlog.appendVersion(fv.timestamp, 0, fv.numObjs);

        VersionManager vMan = new VersionManager(vlog, true);
        for (OV ov : O_VERSIONS)
            vMan.addObjectVersionInfo(ov.objId, 1, ov.timestamp, 0);

        // retrieve object versions that are bound to file versions
        assertEquals(ObjectVersionInfo.MISSING, vMan.getLatestObjectVersionBefore(0, 2000, 10));
        assertEquals(5555, vMan.getLatestObjectVersionBefore(0, 10000, 10).timestamp);
        assertEquals(5555, vMan.getLatestObjectVersionBefore(0, 10008, 10).timestamp);
        assertEquals(ObjectVersionInfo.MISSING, vMan.getLatestObjectVersionBefore(0, 10040, 10));
        assertEquals(ObjectVersionInfo.PADDED, vMan.getLatestObjectVersionBefore(0, 20000, 10));
        assertEquals(17773, vMan.getLatestObjectVersionBefore(0, Long.MAX_VALUE, 10).timestamp);
        assertEquals(ObjectVersionInfo.MISSING, vMan.getLatestObjectVersionBefore(0, Long.MAX_VALUE, 0));

        assertEquals(1, vMan.getLatestObjectVersionBefore(1, 10010, 10).timestamp);
        assertEquals(11000, vMan.getLatestObjectVersionBefore(1, 11000, 10).timestamp);
        assertEquals(30000, vMan.getLatestObjectVersionBefore(1, 100000, 10).timestamp);
        assertEquals(30000, vMan.getLatestObjectVersionBefore(1, Long.MAX_VALUE, 10).timestamp);
        assertEquals(ObjectVersionInfo.MISSING, vMan.getLatestObjectVersionBefore(1, Long.MAX_VALUE, 1));

        assertEquals(ObjectVersionInfo.MISSING, vMan.getLatestObjectVersionBefore(2, 11999, 10));
        assertEquals(11111, vMan.getLatestObjectVersionBefore(2, 12000, 10).timestamp);
        assertEquals(11111, vMan.getLatestObjectVersionBefore(2, Long.MAX_VALUE, 10).timestamp);

        assertEquals(ObjectVersionInfo.MISSING, vMan.getLatestObjectVersionBefore(3, 9999, 10));
        assertEquals(ObjectVersionInfo.PADDED, vMan.getLatestObjectVersionBefore(3, 10000, 10));
        assertEquals(ObjectVersionInfo.PADDED, vMan.getLatestObjectVersionBefore(3, 10019, 10));
        assertEquals(ObjectVersionInfo.MISSING, vMan.getLatestObjectVersionBefore(3, 10020, 10));

        // retrieve latest object versions
        assertEquals(17773, vMan.getLargestObjectVersion(0).timestamp);
        assertEquals(30000, vMan.getLargestObjectVersion(1).timestamp);
        assertEquals(11111, vMan.getLargestObjectVersion(2).timestamp);
        assertEquals(ObjectVersionInfo.MISSING, vMan.getLargestObjectVersion(3));

        // test removal
        vMan.removeObjectVersionInfo(0, 1, 5555);
        assertEquals(1, vMan.getLatestObjectVersionBefore(0, 10005, 10).timestamp);
    }

    public void testCleanup() throws Exception {

        final List<FV> fVersions = new ArrayList<FV>(F_VERSIONS);
        final List<OV> oVersions = new ArrayList<OV>(O_VERSIONS);

        // add object versions that are not bound to any prior file version
        oVersions.add(new OV(0, 60000));
        oVersions.add(new OV(0, 70000));
        oVersions.add(new OV(1, 60000));
        oVersions.add(new OV(2, 60000));

        FileVersionLog vlog = new FileVersionLog(VLOG_FILE);
        for (FV fv : fVersions) {
            fv.timestamp *= 1000;
            vlog.appendVersion(fv.timestamp, 0, fv.numObjs);
        }
        assertEquals(6 * 24, VLOG_FILE.length());

        VersionManager vMan = new VersionManager(vlog, true);
        for (OV ov : oVersions) {
            ov.timestamp *= 1000;
            vMan.addObjectVersionInfo(ov.objId, 1, ov.timestamp, 0);
        }

        // purge log with snapshot timestamps [10020, 11000, 12000, 32000]
        long[] remainingFileVersions = vMan.purgeUnboundFileVersions(new long[] { 10030 * 1000, 11010 * 1000,
                12010 * 1000, 32010 * 1000 });

        // this should implicitly remove file versions '10000' and '10005' ...
        assertEquals(fVersions.size() - 2, vMan.getFileVersionCount());
        assertEquals(fVersions.size() - 2, remainingFileVersions.length);
        assertEquals(10020L * 1000, remainingFileVersions[0]);
        assertEquals(11000L * 1000, remainingFileVersions[1]);
        assertEquals(12000L * 1000, remainingFileVersions[2]);
        assertEquals(32000L * 1000, remainingFileVersions[3]);
        assertEquals(vMan.getLatestFileVersionBefore(10020 * 1000).getTimestamp(), 10020 * 1000);
        assertEquals(vMan.getLatestFileVersionBefore(11000 * 1000).getTimestamp(), 11000 * 1000);
        assertEquals(vMan.getLatestFileVersionBefore(12000 * 1000).getTimestamp(), 12000 * 1000);
        assertEquals(vMan.getLatestFileVersionBefore(32000 * 1000).getTimestamp(), 32000 * 1000);

        // ... which in turn should only detect the first version '1' of object '0' as a superseded version
        Map<Long, Set<ObjectVersionInfo>> unboundVersions = vMan.getUnboundObjectVersions(remainingFileVersions);
        assertEquals(1, unboundVersions.size());

        // purge log with snapshot timestamps [99999]
        remainingFileVersions = vMan.purgeUnboundFileVersions(new long[] { 99999 * 1000 });
        assertEquals(1 * 24, VLOG_FILE.length());

        // this should implicitly remove all file versions but '32000' ...
        assertEquals(1, vMan.getFileVersionCount());
        assertEquals(1, remainingFileVersions.length);
        assertEquals(32000L * 1000, remainingFileVersions[0]);
        assertEquals(vMan.getLatestFileVersionBefore(32000 * 1000).getTimestamp(), 32000 * 1000);

        // ... which in turn should detect some object versions as outdated
        unboundVersions = vMan.getUnboundObjectVersions(remainingFileVersions);
        Set<ObjectVersionInfo> set = unboundVersions.get(0L);
        assertEquals(3, set.size());
        set = unboundVersions.get(1L);
        assertEquals(2, set.size());
        set = unboundVersions.get(2L);
        assertNull(set);

        // purge log with empty timestamp list
        remainingFileVersions = vMan.purgeUnboundFileVersions(new long[0]);
        assertEquals(1 * 24, VLOG_FILE.length());

        // this should implicitly remove all remaining file versions except for the ones attached to the
        // latest file versions
        assertEquals(1, vMan.getFileVersionCount());
        assertEquals(1, remainingFileVersions.length);

        // ... which in turn should detect all some more object versions as outdated
        unboundVersions = vMan.getUnboundObjectVersions(remainingFileVersions);
        set = unboundVersions.get(0L);
        assertEquals(3, set.size());
        set = unboundVersions.get(1L);
        assertEquals(2, set.size());
        set = unboundVersions.get(2L);
        assertNull(set);

    }

    public static void main(String[] args) {
        TestRunner.run(VersionManagerTest.class);
    }

}
