/*
 * Copyright (c) 2008-2013 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.scheduler.simulator;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class SimulationStatistics {
    private Queue<StatisticsRecord> records;

    public SimulationStatistics() {
        this.records = new LinkedBlockingQueue<StatisticsRecord>();
    }

    public void addRecord(StatisticsRecord record) {
        this.records.add(record);
    }

    class StatisticsRecord {
    }
}
