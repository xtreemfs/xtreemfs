/*
 * Copyright (c) 2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc;

import java.io.IOException;

import org.xtreemfs.common.auth.AuthenticationProvider;
import org.xtreemfs.common.config.PolicyClassLoader;
import org.xtreemfs.common.config.PolicyContainer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.mrc.ac.FileAccessPolicy;
import org.xtreemfs.mrc.ac.POSIXFileAccessPolicy;
import org.xtreemfs.mrc.ac.VolumeACLFileAccessPolicy;
import org.xtreemfs.mrc.ac.YesToAnyoneFileAccessPolicy;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.osdselection.FileNamePrefixPolicy;
import org.xtreemfs.mrc.osdselection.FilterDefaultPolicy;
import org.xtreemfs.mrc.osdselection.FilterFQDNPolicy;
import org.xtreemfs.mrc.osdselection.FilterUUIDPolicy;
import org.xtreemfs.mrc.osdselection.GroupDCMapPolicy;
import org.xtreemfs.mrc.osdselection.GroupFQDNPolicy;
import org.xtreemfs.mrc.osdselection.OSDSelectionPolicy;
import org.xtreemfs.mrc.osdselection.PreferredUUIDPolicy;
import org.xtreemfs.mrc.osdselection.SortDCMapPolicy;
import org.xtreemfs.mrc.osdselection.SortFQDNPolicy;
import org.xtreemfs.mrc.osdselection.SortHostRoundRobinPolicy;
import org.xtreemfs.mrc.osdselection.SortLastUpdatedPolicy;
import org.xtreemfs.mrc.osdselection.SortRandomPolicy;
import org.xtreemfs.mrc.osdselection.SortReversePolicy;
import org.xtreemfs.mrc.osdselection.SortUUIDPolicy;
import org.xtreemfs.mrc.osdselection.SortVivaldiPolicy;

public class MRCPolicyContainer extends PolicyContainer {
    
    private static final String[] POLICY_INTERFACES = { FileAccessPolicy.class.getName(),
        OSDSelectionPolicy.class.getName()         };
    
    private static final String[] BUILT_IN_POLICIES = { POSIXFileAccessPolicy.class.getName(),
            VolumeACLFileAccessPolicy.class.getName(), YesToAnyoneFileAccessPolicy.class.getName(),
            FilterDefaultPolicy.class.getName(), FilterFQDNPolicy.class.getName(), FilterUUIDPolicy.class.getName(),
            GroupDCMapPolicy.class.getName(), GroupFQDNPolicy.class.getName(), SortDCMapPolicy.class.getName(),
            SortFQDNPolicy.class.getName(), SortRandomPolicy.class.getName(), SortVivaldiPolicy.class.getName(),
            SortUUIDPolicy.class.getName(), SortReversePolicy.class.getName(), SortHostRoundRobinPolicy.class.getName(),
            SortLastUpdatedPolicy.class.getName(), PreferredUUIDPolicy.class.getName(), FileNamePrefixPolicy.class.getName()
    };
    
    private final MRCConfig             config;
    
    public MRCPolicyContainer(MRCConfig config) throws IOException {
        super(config, new PolicyClassLoader(config.getPolicyDir(), POLICY_INTERFACES, BUILT_IN_POLICIES));
        this.config = config;
    }
    
    public AuthenticationProvider getAuthenticationProvider() throws InstantiationException,
        IllegalAccessException, ClassNotFoundException {
        
        String authPolicy = config.getAuthenticationProvider();
        
        // first, check whether a built-in policy exists with the given name
        try {
            return (AuthenticationProvider) Class.forName(authPolicy).newInstance();
        } catch (Exception exc) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                    "no built-in policy '%s' exists, searching for plug-in policies...", config
                            .getAuthenticationProvider());
        }
        
        // if no built-in policy could be found, check for plug-in policy
        // directory
        
        // if the class file could be found, load it
        Class cls = policyClassLoader.loadClass(authPolicy);
        
        return (AuthenticationProvider) cls.newInstance();
    }
    
    public FileAccessPolicy getFileAccessPolicy(short id, VolumeManager volMan) throws Exception {
        
        try {
            // load the class
            Class policyClass = policyClassLoader.loadClass(id, FileAccessPolicy.class);
            
            if (policyClass == null)
                throw new MRCException("policy not found");
            
            // check whether a default constructor exists; if so, invoke the
            // default
            // constructor
            try {
                return (FileAccessPolicy) policyClass.newInstance();
            } catch (InstantiationException exc) {
                // ignore
            }
            
            // otherwise, check whether a constructor exists that needs the
            // slice
            // manager; if so, invoke it
            try {
                return (FileAccessPolicy) policyClass.getConstructor(new Class[] { VolumeManager.class })
                        .newInstance(volMan);
            } catch (InstantiationException exc) {
                // ignore
            }
            
            // otherwise, throw an exception indicating that no suitable
            // constructor
            // was found
            throw new InstantiationException("policy " + policyClass
                + " does not have a suitable constructor");
            
        } catch (Exception exc) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                "could not load FileAccessPolicy with ID %d", id);
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this, OutputUtils.stackTraceToString(exc));
            throw exc;
        }
        
    }
    
    public OSDSelectionPolicy getOSDSelectionPolicy(short id) throws Exception {
        
        try {
            Class policyClass = policyClassLoader.loadClass(id, OSDSelectionPolicy.class);
            if (policyClass == null)
                throw new MRCException("policy not found");
            return (OSDSelectionPolicy) policyClass.newInstance();
            
        } catch (Exception exc) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                "could not load OSDSelectionPolicy with ID %d", id);
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this, OutputUtils.stackTraceToString(exc));
            throw exc;
        }
    }
    
    // public static void main(String[] args) throws Exception {
    //
    // Logging.start(Logging.LEVEL_DEBUG);
    //
    // PolicyClassLoader loader = new PolicyClassLoader("/tmp/policies");
    // loader.init();
    // System.out.println(loader.loadClass(3, FileAccessPolicy.class));
    // }
}
