/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.osdselection;

import java.net.Inet4Address;
import java.net.InetAddress;

/**
 *
 * @author bjko
 */
public class Inet4AddressMatcher implements InetAddressMatcher {

    private final Inet4Address addr;

    private final int          addrAsInt;

    private final int          networkPrefixLen;

    private final int          netmask;

    public static final int    NETWORK_PREFIX_SINGLE_ADDR = 32;

    public Inet4AddressMatcher(Inet4Address addr) {
        this(addr,NETWORK_PREFIX_SINGLE_ADDR);
    }

    public Inet4AddressMatcher(Inet4Address addr, int networkPrefixLen) {
        this.networkPrefixLen = networkPrefixLen;
        this.addr = addr;
        addrAsInt = bytesToInt(addr.getAddress());
        netmask = (0xFFFFFFFF << (32 - networkPrefixLen)) & 0xFFFFFFFF;
    }

    @Override
    public boolean matches(InetAddress addr) {
        try {
            Inet4Address i4addr = (Inet4Address) addr;
            if (networkPrefixLen == NETWORK_PREFIX_SINGLE_ADDR) {
                return this.addr.equals(i4addr);
            } else {
                final int otherAddrAsInt = bytesToInt(i4addr.getAddress());

                return (this.addrAsInt & netmask) == (otherAddrAsInt & netmask);
            }
        } catch (ClassCastException ex) {
            return false;
        }
    }

    private static int bytesToInt(byte[] arr) {
        int tmp = (arr[3] & 0xFF);
        tmp += (arr[2] & 0xFF) << 8;
        tmp += (arr[1] & 0xFF) << 16;
        tmp += (arr[0] & 0xFF) << 24;
        return tmp;
    }

}
