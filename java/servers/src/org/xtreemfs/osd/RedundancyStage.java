/*
 * Copyright (c) 2015 by Jan Fajerski,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd;

import org.xtreemfs.foundation.flease.FleaseMessageSenderInterface;
import org.xtreemfs.osd.stages.Stage;

/**
 * @author Jan Fajerski
 */
public abstract class RedundancyStage extends Stage implements FleaseMessageSenderInterface{

    public RedundancyStage(String name, int queueCapacity) {
        super(name, queueCapacity);

    }
}
