/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.dir.data;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.List;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceSet;

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
        records = new ArrayList<ServiceRecord>(set.size());
        for (Service serv : set) {
            records.add(new ServiceRecord(serv));
        }
    }

    public ServiceSet getServiceSet() {
        ServiceSet set = new ServiceSet();
        for (ServiceRecord rec : records) {
            set.add(rec.getService());
        }
        return set;
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
