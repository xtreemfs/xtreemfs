/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd;

import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;

/**
 * Class as a replacement for old ObjectData which is now split into ObjectData message and data sent via RPC channel (data)
 * @author bjko
 */
public class InternalObjectData {

    ReusableBuffer data;
    ObjectData     metadata;

    public InternalObjectData(ObjectData metadata, ReusableBuffer data) {
        this.metadata = metadata;
        this.data = data;
    }

    public InternalObjectData( int checksum, boolean invalid_checksum_on_osd, int zero_padding, ReusableBuffer data ) {
        metadata = ObjectData.newBuilder().setChecksum(checksum).setInvalidChecksumOnOsd(invalid_checksum_on_osd).setZeroPadding(zero_padding).build();
        this.data = data;
    }

    public ReusableBuffer getData() {
        return data;
    }

    public int getChecksum() { return metadata.getChecksum(); }
    public boolean getInvalid_checksum_on_osd() { return metadata.getInvalidChecksumOnOsd(); }
    public int getZero_padding() { return metadata.getZeroPadding(); }

    public void setData(ReusableBuffer data) {
        this.data = data;
    }

    public void setZero_padding(int zero_padding) {
        metadata = metadata.toBuilder().setZeroPadding(zero_padding).build();
    }

    public ObjectData getMetadata() {
        return metadata;
    }

}
