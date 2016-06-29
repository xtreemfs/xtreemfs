/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.osdselection;

import java.net.InetAddress;

/**
 *
 * @author bjko
 */
public interface InetAddressMatcher {

    public boolean matches(InetAddress addr);
    
}
