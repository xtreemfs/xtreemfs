/*
 * Copyright (c) 2008-2013 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.scheduler.data;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class ResourceSet {
    private double capacity;
    private double iops;
    private double seqTP;

    public double getCapacity() {
        return capacity;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public double getIops() {
        return iops;
    }

    public void setIops(double iops) {
        this.iops = iops;
    }

    public double getSeqTP() {
        return seqTP;
    }

    public void setSeqTP(double seqTP) {
        this.seqTP = seqTP;
    }
}
