/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.utils;

import org.xtreemfs.dir.discovery.DiscoveryUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.DirService;

/**
 *
 * @author bjko
 */
public class discover_dir {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("sending XtreemFS DIR discover broadcast messages...");
        DirService dir = DiscoveryUtils.discoverDir(10);
        if (dir == null)
            System.out.println("no DIR service found in local network");
        else
            System.out.println("found DIR service: "+dir.getProtocol()+"://"+dir.getAddress()+":"+dir.getPort());
    }


}
