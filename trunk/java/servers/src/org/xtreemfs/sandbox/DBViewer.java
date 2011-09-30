/*
 * Copyright (c) 2009-2011 by Jan Stender, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xtreemfs.babudb.log.DiskLogFile;
import org.xtreemfs.babudb.log.LogEntry;
import org.xtreemfs.babudb.lsmdb.InsertRecordGroup;
import org.xtreemfs.babudb.lsmdb.LSN;
import org.xtreemfs.babudb.lsmdb.InsertRecordGroup.InsertRecord;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.mrc.metadata.BufferBackedRCMetadata;

public class DBViewer {
    
    public static void main(String[] args) throws Exception {
        
        final String logDir = "/home/stender/mrc_data/";
        final String logFile = "1.4657052.dbl";
        
        LSN nextLSN = null;
        // read list of logs and create a list ordered from min LSN to max LSN
        SortedSet<LSN> orderedLogList = new TreeSet<LSN>();
        Pattern p = Pattern.compile("(\\d+)\\.(\\d+)\\.dbl");
        
        Matcher m = p.matcher(logFile);
        m.matches();
        String tmp = m.group(1);
        int viewId = Integer.valueOf(tmp);
        tmp = m.group(2);
        int seqNo = Integer.valueOf(tmp);
        orderedLogList.add(new LSN(viewId, seqNo));
        
        int i = 0;
        
        // apply log entries to databases...
        for (LSN logLSN : orderedLogList) {
            i++;
            DiskLogFile dlf = new DiskLogFile(logDir, logLSN);
            LogEntry le = null;
            while (dlf.hasNext()) {
                le = dlf.next();
                // do something
                ReusableBuffer payload = le.getPayload();
                InsertRecordGroup ai = InsertRecordGroup.deserialize(payload);
                System.out.println("DB ID: " + ai.getDatabaseId());
                System.out.println("inserts:");
                for (InsertRecord rec : ai.getInserts()) {
                    System.out.println(rec.getIndexId()
                        + ": "
                        + Arrays.toString(rec.getKey())
                        + " = "
                        + (rec.getValue() == null ? null : rec.getValue().length > 20 ? ("[...] " + rec
                                .getValue().length) : Arrays.toString(rec.getValue())));
                    if (rec.getValue() != null && rec.getIndexId() == 3 && rec.getKey()[rec.getKey().length - 1] == 1) {
                        BufferBackedRCMetadata md = new BufferBackedRCMetadata(rec.getKey(), rec.getValue());
                        System.out.println(md.getXLocList().getLength());
                        System.out.println(md.getXLocList().getReplica(0));
                    }
                }
                le.free();
            }
            // set lsn'
            if (le != null) {
                nextLSN = new LSN(le.getViewId(), le.getLogSequenceNo() + 1);
            }
            
            if (i == 100)
                break;
        }
        
    }
}
