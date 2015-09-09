/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.metadata;

import org.xtreemfs.mrc.metadata.BufferBackedOwnerQuotaInfo.OwnerType;
import org.xtreemfs.mrc.metadata.BufferBackedOwnerQuotaInfo.QuotaInfo;

public interface OwnerQuotaInfo {

    public String getId();

    public OwnerType getOwnerType();

    public QuotaInfo getQuotaInfo();

    public long getValue();

    public void setValue(long value);
}
