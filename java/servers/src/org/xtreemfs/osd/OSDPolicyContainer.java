/*
 * Copyright (c) 2008-2015 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd;

import org.xtreemfs.common.config.PolicyClassLoader;
import org.xtreemfs.common.config.PolicyContainer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.osd.tracing.FileOutputTracingPolicy;
import org.xtreemfs.osd.tracing.SocketOutputTracingPolicy;
import org.xtreemfs.osd.tracing.TracingPolicy;

import java.io.IOException;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class OSDPolicyContainer extends PolicyContainer {
    private static final String[] POLICY_INTERFACES = { TracingPolicy.class.getName() };

    private static final String[] BUILT_IN_POLICIES = { FileOutputTracingPolicy.class.getName(),
            SocketOutputTracingPolicy.class.getName() };

    private final OSDConfig config;

    public OSDPolicyContainer(OSDConfig config) throws IOException {
        super(config, new PolicyClassLoader(config.getPolicyDir(), POLICY_INTERFACES, BUILT_IN_POLICIES));
        this.config = config;
    }

    public TracingPolicy getTracingPolicy(short id) throws Exception {
        try {
            Class policyClass = policyClassLoader.loadClass(id, TracingPolicy.class);
            if (policyClass == null)
                throw new Exception("policy not found");
            return (TracingPolicy) policyClass.newInstance();

        } catch (Exception exc) {
            Logging.logMessage(Logging.LEVEL_WARN, Logging.Category.misc, this,
                    "could not load TracingPolicy with ID %d", id);
            Logging.logMessage(Logging.LEVEL_WARN, Logging.Category.misc, this, OutputUtils.stackTraceToString(exc));
            throw exc;
        }
    }
}
