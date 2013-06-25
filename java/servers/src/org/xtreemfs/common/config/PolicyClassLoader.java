/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *                            Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.config;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.foundation.util.OutputUtils;

/**
 * A class loader capable of loading policy classes from a given policy
 * directory.
 * <p>
 * 
 * It allows for an efficient retrieval of policies by means of a policy ID and
 * interface. Three requirements have to be met:
 * <ul>
 * <li>the policy needs to be assignable from any of the given policy interfaces
 * <li>the policy either needs to be in the list of built-in policies, or in the
 * plug-in directory together with all its dependencies.
 * <li>the policy needs to define a field
 * <code>public static final long POLICY_ID</code>, which will be used as the ID
 * for a later retrieval
 * </ul>
 * 
 * <p>
 * The class loader distinguishes between <i>built-in</i> and <i>plug-in</i>
 * policies. Built-in policies are pre-defined and always available. Plug-in
 * policies are compiled, if necessary, and dynamically loaded from the given
 * policy directory.
 * 
 * @author stender
 * 
 */
public class PolicyClassLoader extends ClassLoader {
    
    private final Map<String, Class>           cache;
    
    private final Map<Class, Map<Long, Class>> policyMap;
    
    private final Class[]                      policyInterfaces;
    
    private final Class[]                      builtInPolicies;
    
    private File                               policyDir;
    
    private File[]                             jarFiles;
    
    /**
     * Instantiates a new policy class loader.
     * 
     * @param policyDirPath
     *            the path for the directory with all compiled and uncompiled
     *            policies
     * @param policyInterfaceNames
     *            the names of all policy interfaces
     * @param builtInPolicyNames
     *            the names of all built-in policies
     * @throws IOException
     *             if an error occurs while initializing the policy interfaces
     *             or built-in policies
     */
    public PolicyClassLoader(String policyDirPath, String[] policyInterfaceNames, String[] builtInPolicyNames)
        throws IOException {
        
        this.cache = new HashMap<String, Class>();
        this.policyMap = new HashMap<Class, Map<Long, Class>>();
        
        try {
            
            // load policy interfaces
            policyInterfaces = new Class[policyInterfaceNames.length];
            for (int i = 0; i < policyInterfaceNames.length; i++)
                policyInterfaces[i] = Class.forName(policyInterfaceNames[i]);
            
            // load built-in policies
            builtInPolicies = new Class[builtInPolicyNames.length];
            for (int i = 0; i < builtInPolicyNames.length; i++)
                builtInPolicies[i] = Class.forName(builtInPolicyNames[i]);
            
        } catch (ClassNotFoundException exc) {
            throw new IOException("could not initialize policy class loader:", exc);
        }
        
        // initialize the policy dir file if defined
        if (policyDirPath != null)
            policyDir = new File(policyDirPath);
    }
    
    /**
     * Initializes the class loader. This first causes all source code in the
     * policy directory to be compiled. In a second step, each class in the
     * directory is loaded and checked for assignability to one of the given
     * policy interfaces. If the check is successful, the class is added to a
     * map, from which it can be efficiently retrieved by means of a policy ID.
     * 
     * @throws IOException
     *             if an I/O error occurs while compiling or loading any of the
     *             classes
     */
    public void init() throws IOException {
        
        // initialize plug-in policies
        if (policyDir != null && policyDir.exists()) {
            
            // get all JAR files
            jarFiles = policyDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getAbsolutePath().endsWith(".jar");
                }
            });
            
            // get all Java files recursively
            File[] javaFiles = FSUtils.listRecursively(policyDir, new FileFilter() {
                @Override
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
                if (compiler == null) {
                    Logging.logMessage(
                            Logging.LEVEL_WARN,
                            Category.misc,
                            this,
                            "No Java compiler was found to compile additional policies. Make sure that a Java development environment is installed on your system.");
                } else {
                    StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
                    
                    Iterable<? extends JavaFileObject> compilationUnits = fileManager
                            .getJavaFileObjectsFromFiles(Arrays.asList(javaFiles));
                    if (!compiler.getTask(null, fileManager, null, options, null, compilationUnits).call())
                        Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                            "some policies in '%s' could not be compiled", policyDir.getAbsolutePath());
                    
                    fileManager.close();
                }
            }
            
            // retrieve all policies from class files
            File[] classFiles = FSUtils.listRecursively(policyDir, new FileFilter() {
                @Override
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
                    // cache it
                    checkClass(clazz);
                    
                } catch (LinkageError err) {
                    // ignore linkage errors
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
        
        // initialize all built-in policies
        for (Class polClass : builtInPolicies)
            checkClass(polClass);
    }
    
    @Override
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
        
        // if it could not be loaded w/ the system class loader, try to load it
        // from a file and
        // define it
        try {
            
            File classFile = new File(policyDir.getAbsolutePath() + "/" + name.replace('.', '/') + ".class");
            
            Class clazz = loadFromStream(new FileInputStream(classFile));
            
            if (resolve)
                resolveClass(clazz);
            
            return clazz;
            
        } catch (IOException exc) {
            
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                    "could not define class '%s', trying to load the class from a plug-in JAR file", name);
            
        } catch (LinkageError err) {
            
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this, "could not define class '%s'", name);
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this, OutputUtils.stackTraceToString(err));
            
        }
        
        // if the class could not be loaded by the system class loader, try
        // to load it from an external JAR file
        URL[] urls = new URL[jarFiles.length];
        try {
            for (int i = 0; i < jarFiles.length; i++)
                urls[i] = jarFiles[i].toURI().toURL();
        } catch (MalformedURLException exc) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this, OutputUtils.stackTraceToString(exc));
        }
        
        return new URLClassLoader(urls) {
            
            @Override
            public URL getResource(String name) {
                
                URL resource = super.getResource(name);
                if (resource != null)
                    return resource;
                
                return PolicyClassLoader.this.getResource(name);
            }
            
            @Override
            public InputStream getResourceAsStream(String name) {
                
                InputStream stream = super.getResourceAsStream(name);
                if (stream != null)
                    return stream;
                
                return PolicyClassLoader.this.getResourceAsStream(name);
            }
            
        }.loadClass(name);
    }
    
    /**
     * Returns a policy class for a given ID and interface.
     * 
     * @param id
     *            the policy ID
     * @param policyInterface
     *            the policy interface
     * @return a class that has the given ID and is assignable from the given
     *         interface, or <code>null</code>, if no such class exists
     */
    public Class<?> loadClass(long id, Class policyInterface) {
        
        Map<Long, Class> map = policyMap.get(policyInterface);
        if (map == null)
            return null;
        
        Class clazz = map.get(id);
        
        return clazz;
    }
    
    @Override
    public URL getResource(String name) {
        
        // first, try to get the resource from the parent class loader
        URL resource = super.getResource(name);
        if (resource != null)
            return resource;
        
        // if no resource could be retrieved, look into the policy directory
        File file = new File(policyDir.getAbsolutePath() + "/" + name);
        if (file.exists())
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                return null;
            }
        
        return null;
    }
    
    @Override
    public InputStream getResourceAsStream(String name) {
        
        // first, try to get the stream from the parent class loader
        InputStream stream = super.getResourceAsStream(name);
        if (stream != null)
            return stream;
        
        // if no stream could be retrieved, look into the policy directory
        File file = new File(policyDir.getAbsolutePath() + "/" + name);
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException exc) {
            return null;
        }
        
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
        for (Class ifc : policyInterfaces) {
            
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
    
}