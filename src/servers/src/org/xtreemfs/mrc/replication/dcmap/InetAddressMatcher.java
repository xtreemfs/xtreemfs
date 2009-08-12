/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xtreemfs.mrc.replication.dcmap;

import java.net.InetAddress;

/**
 *
 * @author bjko
 */
public interface InetAddressMatcher {

    public boolean matches(InetAddress addr);
    
}
