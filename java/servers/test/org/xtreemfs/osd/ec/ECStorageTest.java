/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.intervals.ObjectInterval;
import org.xtreemfs.test.TestHelper;

public class ECStorageTest {
    @Rule
    public final TestRule testLog = TestHelper.testLog;

    @Test
    public void testcalculateIntervalsToCommitAbort() throws Exception {
        LinkedList<Interval> expectedCommit = new LinkedList<Interval>();
        LinkedList<Interval> expectedAbort = new LinkedList<Interval>();

        boolean failed;
        LinkedList<Interval> toCommitAcc = new LinkedList<Interval>();
        LinkedList<Interval> toAbortAcc = new LinkedList<Interval>();
        LinkedList<Interval> reqVecIntervals = new LinkedList<Interval>();
        LinkedList<Interval> curVecIntervals = new LinkedList<Interval>();
        LinkedList<Interval> nextVecIntervals = new LinkedList<Interval>();

        Interval interval;


        // Commit to an empty cur vector
        // *****************************************
        clearAll(expectedCommit, expectedAbort, toCommitAcc, toAbortAcc, reqVecIntervals, curVecIntervals,
                nextVecIntervals);
        interval = new ObjectInterval(0, 12, 1, 1);
        nextVecIntervals.add(interval);
        reqVecIntervals.add(interval);
        expectedCommit.add(interval);

        failed = ECStorage.calculateIntervalsToCommitAbort(reqVecIntervals, curVecIntervals, nextVecIntervals,
                toCommitAcc, toAbortAcc);
        assertFalse(failed);
        assertEquals(expectedAbort, toAbortAcc);
        assertEquals(expectedCommit, toCommitAcc);
        

        // Abort failed op overlapping with two commited intervals
        // *******************************************************
        clearAll(expectedCommit, expectedAbort, toCommitAcc, toAbortAcc, reqVecIntervals, curVecIntervals,
                nextVecIntervals);
        
        interval = new ObjectInterval(0, 6, 1, 1);
        curVecIntervals.add(interval);
        reqVecIntervals.add(interval);
        interval = new ObjectInterval(6, 12, 2, 2);
        curVecIntervals.add(interval);
        reqVecIntervals.add(interval);
        interval = new ObjectInterval(3, 9, 3, 3);
        nextVecIntervals.add(interval);
        expectedAbort.add(interval);

        failed = ECStorage.calculateIntervalsToCommitAbort(reqVecIntervals, curVecIntervals, nextVecIntervals,
                toCommitAcc, toAbortAcc);
        assertFalse(failed);
        assertEquals(expectedAbort, toAbortAcc);
        assertEquals(expectedCommit, toCommitAcc);


        // Abort two failed ops overlapping with one larger committed interval
        // *******************************************************************
        clearAll(expectedCommit, expectedAbort, toCommitAcc, toAbortAcc, reqVecIntervals, curVecIntervals,
                nextVecIntervals);

        interval = new ObjectInterval(0, 12, 1, 1);
        curVecIntervals.add(interval);
        reqVecIntervals.add(interval);

        interval = new ObjectInterval(3, 6, 2, 2);
        nextVecIntervals.add(interval);
        expectedAbort.add(interval);
        interval = new ObjectInterval(6, 9, 2, 3);
        nextVecIntervals.add(interval);
        expectedAbort.add(interval);

        failed = ECStorage.calculateIntervalsToCommitAbort(reqVecIntervals, curVecIntervals, nextVecIntervals,
                toCommitAcc, toAbortAcc);
        assertFalse(failed);
        assertEquals(expectedAbort, toAbortAcc);
        assertEquals(expectedCommit, toCommitAcc);


        // Commit with splitting op
        // *****************************************
        clearAll(expectedCommit, expectedAbort, toCommitAcc, toAbortAcc, reqVecIntervals, curVecIntervals,
                nextVecIntervals);

        interval = new ObjectInterval(0, 6, 1, 1);
        curVecIntervals.add(interval);
        interval = new ObjectInterval(6, 12, 2, 2);
        curVecIntervals.add(interval);

        interval = new ObjectInterval(0, 3, 1, 1);
        reqVecIntervals.add(interval);

        interval = new ObjectInterval(3, 9, 3, 3);
        nextVecIntervals.add(interval);
        reqVecIntervals.add(interval);
        expectedCommit.add(interval);

        interval = new ObjectInterval(9, 12, 2, 2);
        reqVecIntervals.add(interval);

        failed = ECStorage.calculateIntervalsToCommitAbort(reqVecIntervals, curVecIntervals, nextVecIntervals,
                toCommitAcc, toAbortAcc);
        assertFalse(failed);
        assertEquals(expectedAbort, toAbortAcc);
        assertEquals(expectedCommit, toCommitAcc);


        // Test failure with missing interval
        // *****************************************
        clearAll(expectedCommit, expectedAbort, toCommitAcc, toAbortAcc, reqVecIntervals, curVecIntervals,
                nextVecIntervals);

        interval = new ObjectInterval(0, 12, 1, 2);
        reqVecIntervals.add(interval);

        failed = ECStorage.calculateIntervalsToCommitAbort(reqVecIntervals, curVecIntervals, nextVecIntervals,
                toCommitAcc, toAbortAcc);
        assertTrue(failed);


        // Test failure with incomplete write from the same version, but older id
        // **********************************************************************
        clearAll(expectedCommit, expectedAbort, toCommitAcc, toAbortAcc, reqVecIntervals, curVecIntervals,
                nextVecIntervals);

        interval = new ObjectInterval(0, 6, 1, 2);
        reqVecIntervals.add(interval);
        interval = new ObjectInterval(6, 12, 2, 3);
        reqVecIntervals.add(interval);

        interval = new ObjectInterval(0, 6, 1, 1);
        nextVecIntervals.add(interval);
        interval = new ObjectInterval(6, 12, 1, 1);
        nextVecIntervals.add(interval);

        failed = ECStorage.calculateIntervalsToCommitAbort(reqVecIntervals, curVecIntervals, nextVecIntervals,
                toCommitAcc, toAbortAcc);
        assertTrue(failed);


        // Test IOError if reqVector is smaller then curVector
        // **********************************************************************
        clearAll(expectedCommit, expectedAbort, toCommitAcc, toAbortAcc, reqVecIntervals, curVecIntervals,
                nextVecIntervals);

        interval = new ObjectInterval(0, 12, 1, 1);
        reqVecIntervals.add(interval);

        interval = new ObjectInterval(0, 12, 2, 2);
        curVecIntervals.add(interval);

        try {
            failed = ECStorage.calculateIntervalsToCommitAbort(reqVecIntervals, curVecIntervals, nextVecIntervals,
                    toCommitAcc, toAbortAcc);
            fail();
        } catch (IOException ex) {
            // expected
        }
    }

    static void clearAll(List... lists) {
        for (List list : lists) {
            list.clear();
        }
    }

}
