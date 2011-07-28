/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.uuids;

import java.net.InetSocketAddress;

public class Mapping {
    
    public String            protocol;
    
    public InetSocketAddress resolvedAddr;
    
    public Mapping(String protocol, InetSocketAddress resolvedAddr) {
        this.protocol = protocol;
        this.resolvedAddr = resolvedAddr;
    }
    
    public String toString() {
        return this.protocol + "://" + this.resolvedAddr.getAddress().getHostAddress() + ":"
            + this.resolvedAddr.getPort();
    }
    
}