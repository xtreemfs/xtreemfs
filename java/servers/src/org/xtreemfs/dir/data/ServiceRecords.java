/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.data;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;

/**
 *
 * @author bjko
 */
public class ServiceRecords {

    ArrayList<ServiceRecord> records;

    public ServiceRecords() {
        records = new ArrayList<ServiceRecord>(1);
    }

    public ServiceRecords(ReusableBuffer rb) throws IOException {
        try {
            int numEntries = rb.getInt();
            records = new ArrayList<ServiceRecord>(numEntries);
            for (int i = 0; i < numEntries; i++) {
                ServiceRecord m = new ServiceRecord(rb);
                records.add(m);
            }
        } catch (BufferUnderflowException ex) {
            throw new IOException("corrupted ServiceRecords entry: "+ex,ex);
        }
    }

    public ServiceRecords(ServiceSet set) {
        records = new ArrayList<ServiceRecord>(set.getServicesCount());
        for (Service serv : set.getServicesList()) {
            records.add(new ServiceRecord(serv));
        }
    }

    public ServiceSet getServiceSet() {
        ServiceSet.Builder set = ServiceSet.newBuilder();
        for (ServiceRecord rec : records) {
            set.addServices(rec.getService());
        }
        return set.build();
    }

    public void add(ServiceRecords otherList) {
        records.addAll(otherList.records);
    }

    public void add(ServiceRecord item) {
        records.add(item);
    }

    public int size() {
        return records.size();
    }

    public ServiceRecord getRecord(int index) {
        return records.get(index);
    }

    public List<ServiceRecord> getList() {
        return records;
    }

    public int getSize() {
        final int INT_SIZE = Integer.SIZE/8;
        int size = INT_SIZE;
        for (ServiceRecord rec: records) {
            size += rec.getSize();
        }
        return size;
    }

    public void serialize(ReusableBuffer rb) throws IOException {
        try {
            rb.putInt(records.size());
            for (ServiceRecord rec: records) {
                rec.serialize(rb);
            }
        } catch (BufferOverflowException ex) {
            throw new IOException("buffer too small",ex);
        }
    }

}
