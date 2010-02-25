/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.sandbox;

import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.File;
import org.xtreemfs.common.clients.Volume;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.UserCredentials;

/**
 *
 * @author bjko
 */
public class WiredUTFCharsForMinor {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            Logging.start(Logging.LEVEL_DEBUG, Category.all);
            TimeSync.initializeLocal(50, 50);
            // TODO code application logic here
            Client c = new Client(new InetSocketAddress[]{new InetSocketAddress("xtreem.zib.de", 32638)}, 15000, 60000, null);
            c.start();

            StringSet grps = new StringSet();
            grps.add("users");
            Volume v = c.getVolume("testminor", new UserCredentials("bjko", grps, ""));
            File f = v.getFile("/ŸŸœœΑΑΩΩ١١٢٢٣٣٤٤٥٥٦٦٧٧٨٨٩٩");
            f.mkdir(0777);
            c.stop();

        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
            Logger.getLogger(WiredUTFCharsForMinor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
