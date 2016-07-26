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
import java.util.Enumeration;
import java.util.List;

import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMapping;

public class NetUtils {
    
    /**
     * Returns a list of mappings for all reachable network endpoints.
     * 
     * The returned list contains global addresses in front of local ones.
     * 
     * @param port
     *            The port to assign to the mappings.
     * @param protocol
     *            The protocol for the endpoint.
     * 
     * @return A list of mappings, containing global ones up front.
     * @throws IOException
     */
    public static List<AddressMapping.Builder> getReachableEndpoints(int port, String protocol) throws IOException {
        
        List<AddressMapping.Builder> endpoints = new ArrayList<AddressMapping.Builder>();
        List<AddressMapping.Builder> localEndpoints = new ArrayList<AddressMapping.Builder>();
        
        // Iterate over the existing network interfaces and their addresses.
        Enumeration<NetworkInterface> ifcs = NetworkInterface.getNetworkInterfaces();
        while (ifcs.hasMoreElements()) {
            NetworkInterface ifc = ifcs.nextElement();
            
            // Ignore loopback interfaces and interfaces that are down.
            if (ifc.isLoopback() || !ifc.isUp())
                continue;
            
            List<InterfaceAddress> addrs = ifc.getInterfaceAddresses();
            for (InterfaceAddress addr : addrs) {
                InetAddress inetAddr = addr.getAddress();

                // Ignore local, wildcard and multicast addresses.
                if (inetAddr.isLoopbackAddress() || inetAddr.isLinkLocalAddress() 
                        || inetAddr.isAnyLocalAddress() || inetAddr.isMulticastAddress())
                    continue;
                                               
                final String hostAddr = getHostAddress(inetAddr);
                final String uri = getURI(protocol, inetAddr, port);
                final String network = getNetworkCIDR(inetAddr, addr.getNetworkPrefixLength());
                AddressMapping.Builder amap = AddressMapping.newBuilder().setAddress(hostAddr).setPort(port)
                        .setMatchNetwork(network).setProtocol(protocol).setTtlS(3600).setUri(uri).setVersion(0)
                        .setUuid("");

                if (inetAddr.isSiteLocalAddress()) {
                    localEndpoints.add(amap);
                } else {
                    endpoints.add(amap);
                }
            }
        }
        
        // Append the local endpoints to the end of the list containing the global ones.
        endpoints.addAll(localEndpoints);

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
    
    /**
     * Creates a clone of the {@link AddressMapping.Builder} passed as <code>src</code> and replaces the
     * protocol in the corresponding field and the uri string.
     * 
     * @param src
     *            {@link AddressMapping.Builder} whose protocol should be replaced.
     * @param protocol
     *            The new protocol used for the mapping.
     * @return Clone of the passed <code>src</code> with the protocol replaced.
     */
    public static AddressMapping.Builder cloneMappingForProtocol(AddressMapping.Builder src, String protocol) {
        // An uri looks like "protocol://address:port". The following code replaces the "protocol" part.
        String uri = protocol + src.getUri().substring(src.getUri().indexOf("://"));
        AddressMapping.Builder result = src.clone()
                .setProtocol(protocol).setUri(uri);
        return result;
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
            // returned by a valid InetAddress.
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
