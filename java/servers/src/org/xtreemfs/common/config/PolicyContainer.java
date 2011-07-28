/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *                            Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.config;

import java.io.IOException;

import org.xtreemfs.foundation.SSLOptions.TrustManager;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

public class PolicyContainer {
    
    protected final ServiceConfig     config;
    
    protected final PolicyClassLoader policyClassLoader;
    
    public PolicyContainer(ServiceConfig config) throws IOException {
        
        this.config = config;
        this.policyClassLoader = new PolicyClassLoader(config.getPolicyDir(), new String[0], new String[0]);
        
        policyClassLoader.init();
    }
    
    protected PolicyContainer(ServiceConfig config, PolicyClassLoader policyClassLoader) throws IOException {
        
        this.config = config;
        this.policyClassLoader = policyClassLoader;
        
        policyClassLoader.init();
    }
    
    public TrustManager getTrustManager() throws ClassNotFoundException, InstantiationException,
        IllegalAccessException {
        
        String trustManager = config.getTrustManager();
        if (trustManager == null || trustManager.equals(""))
            return null;
        
        // first, check whether a built-in policy exists with the given name
        try {
            return (TrustManager) Class.forName(trustManager).newInstance();
        } catch (Exception exc) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                    "no built-in policy '%s' exists, searching for plug-in policies...", config
                            .getTrustManager());
        }
        
        // if no built-in policy could be found, check for plug-in policy
        // directory
        
        // if the class file could be found, load it
        Class cls = policyClassLoader.loadClass(trustManager);
        
        return (TrustManager) cls.newInstance();
        
    }
    
}
