/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Jan Stender (ZIB)
 */
package org.xtreemfs.mrc;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.xtreemfs.common.auth.AuthenticationProvider;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.mrc.ac.FileAccessPolicy;
import org.xtreemfs.mrc.ac.POSIXFileAccessPolicy;
import org.xtreemfs.mrc.ac.VolumeACLFileAccessPolicy;
import org.xtreemfs.mrc.ac.YesToAnyoneFileAccessPolicy;
import org.xtreemfs.mrc.osdselection.DNSSelectionPolicy;
import org.xtreemfs.mrc.osdselection.OSDSelectionPolicy;
import org.xtreemfs.mrc.osdselection.ProximitySelectionPolicy;
import org.xtreemfs.mrc.osdselection.RandomSelectionPolicy;
import org.xtreemfs.mrc.replication.FQDNReplicaSelectionPolicy;
import org.xtreemfs.mrc.replication.ReplicaSelectionPolicy;
import org.xtreemfs.mrc.replication.SimpleReplicaSelectionPolicy;
import org.xtreemfs.mrc.volumes.VolumeManager;

public class PolicyContainer {
    
    static class PolicyClassLoader extends ClassLoader {
        
        private static final Class[]         POLICY_INTERFACES = { FileAccessPolicy.class,
                                                                   OSDSelectionPolicy.class,
                                                                   ReplicaSelectionPolicy.class };
        
        private static final Class[]         BUILT_IN_POLICIES = { POSIXFileAccessPolicy.class,
                                                                   VolumeACLFileAccessPolicy.class,
                                                                   YesToAnyoneFileAccessPolicy.class,
                                                                   RandomSelectionPolicy.class,
                                                                   ProximitySelectionPolicy.class,
                                                                   DNSSelectionPolicy.class,
                                                                   SimpleReplicaSelectionPolicy.class,
                                                                   FQDNReplicaSelectionPolicy.class };
        
        private Map<String, Class>           cache;
        
        private Map<Class, Map<Long, Class>> policyMap;
        
        private File[]                       jarFiles;
        
        private File                         policyDir;
        
        public PolicyClassLoader(String policyDirPath) throws IOException {
            
            this.cache = new HashMap<String, Class>();
            this.policyMap = new HashMap<Class, Map<Long, Class>>();
            
            if (policyDirPath != null)
                policyDir = new File(policyDirPath);
        }
        
        public void init() throws IOException {
            
            // initialize plug-in policies
            if ((policyDir != null) && (policyDir.exists())) {
                
                // get all JAR files
                jarFiles = policyDir.listFiles(new FileFilter() {
                    public boolean accept(File pathname) {
                        return pathname.getAbsolutePath().endsWith(".jar");
                    }
                });
                
                // get all Java files recursively
                File[] javaFiles = FSUtils.listRecursively(policyDir, new FileFilter() {
                    public boolean accept(File pathname) {
                        return pathname.getAbsolutePath().endsWith(".java");
                    }
                });
                
                // compile all Java files
                if (javaFiles.length != 0) {
                    
                    String cp = System.getProperty("java.class.path") + ":";
                    for (int i = 0; i < jarFiles.length; i++) {
                        cp += jarFiles[i];
                        if (i != jarFiles.length - 1)
                            cp += ":";
                    }
                    
                    List<String> options = new ArrayList<String>(1);
                    options.add("-cp");
                    options.add(cp);
                    
                    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                    StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
                    
                    Iterable<? extends JavaFileObject> compilationUnits = fileManager
                            .getJavaFileObjectsFromFiles(Arrays.asList(javaFiles));
                    if (!compiler.getTask(null, fileManager, null, options, null, compilationUnits).call())
                        Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                            "some policies in '%s' could not be compiled", policyDir.getAbsolutePath());
                    
                    fileManager.close();
                }
                
                // retrieve all policies from class files
                File[] classFiles = FSUtils.listRecursively(policyDir, new FileFilter() {
                    public boolean accept(File pathname) {
                        return pathname.getAbsolutePath().endsWith(".class");
                    }
                });
                
                for (File cls : classFiles) {
                    try {
                        
                        String className = cls.getAbsolutePath().substring(
                            policyDir.getAbsolutePath().length() + 1,
                            cls.getAbsolutePath().length() - ".class".length()).replace('/', '.');
                        if (cache.containsKey(className))
                            continue;
                        
                        // load the class
                        Class clazz = loadFromStream(new FileInputStream(cls));
                        
                        // check whether the class refers to a policy; if so,
                        // cache
                        // it
                        checkClass(clazz);
                        
                    } catch (Exception exc) {
                        Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                            "an error occurred while trying to load class from file " + cls);
                        Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this, OutputUtils
                                .stackTraceToString(exc));
                    }
                }
                
                // retrieve all policies from JAR files
                // for (File jar : jarFiles) {
                //
                // JarFile arch = new JarFile(jar);
                //
                // Enumeration<JarEntry> entries = arch.entries();
                // while (entries.hasMoreElements()) {
                // JarEntry entry = entries.nextElement();
                // if (entry.getName().endsWith(".class")) {
                //
                // try {
                //
                // // load the class
                // Class clazz = loadFromStream(arch.getInputStream(entry));
                //
                // // check whether the class refers to a policy; if
                // // so, cache it
                // checkClass(clazz);
                //
                // } catch (IOException exc) {
                // Logging.logMessage(Logging.LEVEL_WARN, this, "could not load
                // class '"
                // + entry.getName() + "' from JAR '" + jar.getAbsolutePath() +
                // "'");
                // Logging.logMessage(Logging.LEVEL_WARN, this, exc);
                // } catch (LinkageError err) {
                // // ignore
                // }
                // }
                // }
                // }
                
            }
            
            // init built-in policies
            initBuiltInPolicies();
        }
        
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            
            // first, check whether the class is cached
            if (cache.containsKey(name))
                return cache.get(name);
            
            // if not cached, try to resolve the class by means of the system
            // class loader
            try {
                return findSystemClass(name);
            } catch (ClassNotFoundException exc) {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                        "could not find system class '%s', trying to define the class", name);
            }
            
            if (policyDir == null || !policyDir.exists())
                throw new ClassNotFoundException("no built-in policy '" + name
                    + "' available, and no plug-in policy directory specified");
            
            // if it could not be loaded w/ the system class loader, try to
            // define it
            try {
                
                File classFile = new File(policyDir.getAbsolutePath() + "/" + name.replace('.', '/')
                    + ".class");
                
                Class clazz = loadFromStream(new FileInputStream(classFile));
                
                if (resolve)
                    resolveClass(clazz);
                
                return clazz;
                
            } catch (IOException exc) {
                
                if (Logging.isDebug())
                    Logging
                            .logMessage(
                                Logging.LEVEL_DEBUG,
                                Category.misc,
                                this,
                                "could not define class '%s', trying to load the class from a plug-in JAR file",
                                name);
                
            } catch (LinkageError err) {
                
                Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this, "could not define class '%s'",
                    name);
                Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this, OutputUtils
                        .stackTraceToString(err));
                
            }
            
            // if the class could not be loaded by the system class loader, try
            // to load it from an external JAR file
            URL[] urls = new URL[jarFiles.length];
            try {
                for (int i = 0; i < jarFiles.length; i++)
                    urls[i] = jarFiles[i].toURI().toURL();
            } catch (MalformedURLException exc) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this, OutputUtils
                        .stackTraceToString(exc));
            }
            
            return new URLClassLoader(urls).loadClass(name);
        }
        
        public Class<?> loadClass(long id, Class policyInterFace) throws ClassNotFoundException {
            
            Map<Long, Class> map = policyMap.get(policyInterFace);
            if (map == null)
                return null;
            // throw new ClassNotFoundException();
            
            Class clazz = map.get(id);
            /*
             * if (clazz == null) return null; throw new
             * ClassNotFoundException();
             */

            return clazz;
        }
        
        private Class loadFromStream(InputStream in) throws IOException {
            
            // load the binary class content
            byte[] classData = new byte[in.available()];
            in.read(classData);
            in.close();
            
            Class clazz = defineClass(null, classData, 0, classData.length);
            cache.put(clazz.getName(), clazz);
            
            return clazz;
        }
        
        private void checkClass(Class clazz) {
            
            // check whether the class matches any of the policy
            // interfaces
            for (Class ifc : POLICY_INTERFACES) {
                
                if (ifc.isAssignableFrom(clazz)) {
                    
                    // get the policy ID
                    try {
                        long policyId = clazz.getDeclaredField("POLICY_ID").getLong(null);
                        
                        // add the policy to the internal map
                        Map<Long, Class> polIdMap = policyMap.get(ifc);
                        if (polIdMap == null) {
                            polIdMap = new HashMap<Long, Class>();
                            policyMap.put(ifc, polIdMap);
                        }
                        
                        if (polIdMap.containsKey(policyId))
                            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                                "duplicate ID for policy '%s': %d", ifc.getName(), policyId);
                        
                        polIdMap.put(policyId, clazz);
                        
                    } catch (Exception exc) {
                        Logging.logMessage(Logging.LEVEL_WARN, this, "could not load malformed policy '%s'",
                            clazz.getName());
                        Logging.logMessage(Logging.LEVEL_WARN, this, OutputUtils.stackTraceToString(exc));
                    }
                }
            }
        }
        
        private void initBuiltInPolicies() {
            for (Class polClass : BUILT_IN_POLICIES)
                checkClass(polClass);
        }
        
    }
    
    private final MRCConfig   config;
    
    private PolicyClassLoader policyClassLoader;
    
    public PolicyContainer(MRCConfig config) throws IOException {
        this.config = config;
        policyClassLoader = new PolicyClassLoader(config.getPolicyDir());
        policyClassLoader.init();
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
    
    public ReplicaSelectionPolicy getReplicaSelectionPolicy(short id) throws Exception {
        
        try {
            Class policyClass = policyClassLoader.loadClass(id, ReplicaSelectionPolicy.class);
            if (policyClass == null)
                throw new MRCException("policy not found (id="+id+")");
            return (ReplicaSelectionPolicy) policyClass.newInstance();
            
        } catch (Exception exc) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                "could not load ReplicaSelectionPolicy with ID %d", id);
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
