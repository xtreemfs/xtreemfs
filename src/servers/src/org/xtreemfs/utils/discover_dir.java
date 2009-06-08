/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.utils;

import org.xtreemfs.dir.discovery.DiscoveryUtils;
import org.xtreemfs.interfaces.DirService;

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
