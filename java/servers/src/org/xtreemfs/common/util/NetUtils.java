/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.util;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMapping;

public class NetUtils {
    
    /**
     * Returns a list of mappings for all reachable network endpoints.
     * 
     * @param port
     *            the port to assign to the mappings
     * @param protocol
     *            the protocol for the endpoint
     * @return a list of mappings
     * @throws IOException
     */
    public static List<AddressMapping.Builder> getReachableEndpoints(int port, String protocol) throws IOException {
        
        List<AddressMapping.Builder> endpoints = new ArrayList<AddressMapping.Builder>(10);
        
        // Try to find the global address by resolving the local hostname.
        String localHostAddress = null;
        try {
            localHostAddress = getHostAddress(InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.net, null, "Could not resolve the local hostnamme.",
                    new Object[0]);
        }

        // Use every ip, that is assigned to an interface, and its corresponding network as an endpoint.
        Enumeration<NetworkInterface> ifcs = NetworkInterface.getNetworkInterfaces();
        while (ifcs.hasMoreElements()) {
            NetworkInterface ifc = ifcs.nextElement();
            
            if (ifc.isLoopback() || !ifc.isUp())
                continue;
            
            List<InterfaceAddress> addrs = ifc.getInterfaceAddresses();
            for (InterfaceAddress addr : addrs) {
                InetAddress inetAddr = addr.getAddress();

                // ignore local and wildcard addresses
                if (inetAddr.isLoopbackAddress() || inetAddr.isLinkLocalAddress() || inetAddr.isAnyLocalAddress())
                    continue;
                
                final String hostAddr = getHostAddress(inetAddr);
                final String uri = getURI(protocol, inetAddr, port);
                final String network = getNetworkCIDR(inetAddr, addr.getNetworkPrefixLength());
                AddressMapping.Builder amap = AddressMapping.newBuilder().setAddress(hostAddr).setPort(port)
                        .setProtocol(protocol).setTtlS(3600).setUri(uri).setVersion(0).setUuid("");

                // Add the wildcard network if this is the local host address.
                if (hostAddr.equals(localHostAddress)) {
                    amap.setMatchNetwork("*");
                } else {
                    amap.setMatchNetwork(network);
                }
                
                endpoints.add(amap);
            }
        }
        
        // in case no IP address could be found at all, use 127.0.0.1 for local testing
        if (endpoints.isEmpty()) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.net, null,
                "could not find a valid IP address, will use 127.0.0.1 instead", new Object[0]);
            AddressMapping.Builder amap = AddressMapping.newBuilder().setAddress("127.0.0.1").setPort(port)
                    .setProtocol(protocol).setTtlS(3600).setMatchNetwork("*")
                    .setUri(getURI(protocol, InetAddress.getLocalHost(), port)).setVersion(0).setUuid("");
            endpoints.add(amap);
        }
        
        // The sorting will assure globally reachable endpoints are at the head of the collection.
        Collections.sort(endpoints, new Comparator<AddressMapping.Builder>() {
            public int compare(AddressMapping.Builder o1, AddressMapping.Builder o2) {
                int o1global = "*".equals(o1.getMatchNetwork()) ? -1 : 1;
                int o2global = "*".equals(o2.getMatchNetwork()) ? -1 : 1;
                return o1global - o2global;
            }
        });

        return endpoints;
    }

    public static String getHostAddress(InetAddress host) {

        String hostAddr = host.getHostAddress();
        if (host instanceof Inet6Address) {
            if (hostAddr.lastIndexOf('%') >= 0) {
                hostAddr = hostAddr.substring(0, hostAddr.lastIndexOf('%'));
            }
        }

        return hostAddr;

    }

    public static String getURI(String protocol, InetAddress host, int port) {

        String hostAddr = host.getHostAddress();
        if (host instanceof Inet6Address) {
            if (hostAddr.lastIndexOf('%') >= 0) {
                hostAddr = hostAddr.substring(0, hostAddr.lastIndexOf('%'));
            }
            hostAddr = "["+hostAddr+"]";
        }

        return protocol+"://"+hostAddr+":"+port;

    }
    
    public static String getDomain(String hostName) {
        int i = hostName.indexOf('.');
        return i == -1? "": hostName.substring(i + 1);
    }
    
    private static String getSubnetMaskString(short prefixLength) {
        
        long addr = (0xFFFFFFFFL << (32 - prefixLength)) & 0xFFFFFFFFL;
        StringBuffer sb = new StringBuffer();
        for (int i = 3; i >= 0; i--) {
            sb.append((addr & (0xFF << (i * 8))) >> (i * 8));
            if (i > 0)
                sb.append(".");
        }
        
        return sb.toString();
    }
    
    private static String getNetworkCIDR(InetAddress addr, short prefixLength) {
        byte[] raw = addr.getAddress();

        // If the address is more then 32bit long it has to be an v6 address.
        boolean isV6 = raw.length > 4;

        // Get the number of fields that are network specific and null the remaining host fields.
        int networkFields = prefixLength / 8;
        for (int i = networkFields + 1; i < raw.length; i++) {
            raw[i] = 0x00;
        }

        // Get the remaining bytes attributed to the network amidst a byte field.
        int networkRemainder = prefixLength % 8;
        if (networkFields < raw.length) {
            // Construct a 8bit mask, with bytes set for the network.
            byte mask = (byte) (0xFF << (8 - networkRemainder));
            raw[networkFields] = (byte) (raw[networkFields] & mask);
        }

        StringBuilder sb = new StringBuilder();

        // Use the InetAddress implementation to convert the raw byte[] to a string.
        try {
            sb.append(InetAddress.getByAddress(raw).getHostAddress());
        } catch (UnknownHostException e) {
            // This should never happen, since the foundation of every calculation is the byte array
            // returned by a valid InetAddress
            throw new RuntimeException(e);
        }

        sb.append("/").append(prefixLength);

        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        
        System.out.println("all network interfaces: ");
        Enumeration<NetworkInterface> ifcs = NetworkInterface.getNetworkInterfaces();
        while (ifcs.hasMoreElements()) {
            for (InterfaceAddress addr : ifcs.nextElement().getInterfaceAddresses()) {
                InetAddress inetAddr = addr.getAddress();
                System.out.println(inetAddr + ", loopback: " + inetAddr.isLoopbackAddress() + ", linklocal: "
                    + inetAddr.isLinkLocalAddress() + ", reachable: " + inetAddr.isReachable(1000));
            }
        }
        
        System.out.println("\nsuitable network interfaces: ");
        for (AddressMapping.Builder endpoint : NetUtils.getReachableEndpoints(32640, "http"))
            System.out.println(endpoint.build().toString());
    }
    
}
