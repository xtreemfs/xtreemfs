/*
 * Copyright (c) 2009-2010 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.monitoring;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The class provides the ability to monitor data. It could monitor multiple data for each key.<br>
 * NOTE: This class is thread-safe. <br>
 * 22.07.2009
 */
public class ListMonitoring<V> extends Monitoring<List<V>> {
}
