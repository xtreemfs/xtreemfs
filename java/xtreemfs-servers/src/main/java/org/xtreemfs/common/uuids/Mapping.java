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

    /** Address of the service as returned by the DIR, Format: host:port. */
    public String            address;

    public Mapping(String protocol, InetSocketAddress resolvedAddr, String address) {
        this.protocol = protocol;
        this.resolvedAddr = resolvedAddr;
        this.address = address;
    }

    public String toString() {
        return this.protocol + "://" + this.resolvedAddr.getAddress().getHostAddress() + ":"
                + this.resolvedAddr.getPort();
    }

}