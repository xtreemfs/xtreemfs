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
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMapping;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMappingSet;

/**
 *
 * @author bjko
 */
public class AddressMappingRecords {

    ArrayList<AddressMappingRecord> records;

    public AddressMappingRecords() {
        records = new ArrayList<AddressMappingRecord>(2);
    }

    public AddressMappingRecords(ReusableBuffer rb) throws IOException {
        try {
            int numEntries = rb.getInt();
            records = new ArrayList<AddressMappingRecord>(numEntries);
            for (int i = 0; i < numEntries; i++) {
                AddressMappingRecord m = new AddressMappingRecord(rb);
                records.add(m);
            }
        } catch (BufferUnderflowException ex) {
            throw new IOException("corrupted AddressMappingRecords entry: "+ex,ex);
        }
    }

    public AddressMappingRecords(AddressMappingSet set) {
        records = new ArrayList<AddressMappingRecord>(set.getMappingsCount());
        for (AddressMapping am : set.getMappingsList()) {
            records.add(new AddressMappingRecord(am));
        }
    }

    public AddressMappingSet getAddressMappingSet() {
        AddressMappingSet.Builder set = AddressMappingSet.newBuilder();
        for (AddressMappingRecord rec : records) {
            set.addMappings(rec.getAddressMapping());
        }
        return set.build();
    }

    public void add(AddressMappingRecords otherList) {
        records.addAll(otherList.records);
    }

    public int size() {
        return records.size();
    }

    public AddressMappingRecord getRecord(int index) {
        return records.get(index);
    }

    public List<AddressMappingRecord> getRecords() {
        return records;
    }

    public int getSize() {
        final int INT_SIZE = Integer.SIZE/8;
        int size = INT_SIZE;
        for (AddressMappingRecord rec: records) {
            size += rec.getSize();
        }
        return size;
    }

    public void serialize(ReusableBuffer rb) throws IOException {
        try {
            rb.putInt(records.size());
            for (AddressMappingRecord rec: records) {
                rec.serialize(rb);
            }
        } catch (BufferOverflowException ex) {
            throw new IOException("buffer too small",ex);
        }
    }

}
