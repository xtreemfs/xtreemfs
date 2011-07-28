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
import java.util.ArrayList;
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
        
        List<AddressMapping.Builder> endpoints = new ArrayList(10);
        
        // first, try to find a globally reachable endpoint
        Enumeration<NetworkInterface> ifcs = NetworkInterface.getNetworkInterfaces();
        while (ifcs.hasMoreElements()) {
            
            NetworkInterface ifc = ifcs.nextElement();
            List<InterfaceAddress> addrs = ifc.getInterfaceAddresses();
            
            // // prefer global addresses to local ones
            // Collections.sort(addrs, new Comparator<InterfaceAddress>() {
            // public int compare(InterfaceAddress o1, InterfaceAddress o2) {
            // int o1global = o1.getAddress().isAnyLocalAddress() ? -1 : 1;
            // int o2global = o2.getAddress().isAnyLocalAddress() ? -1 : 1;
            // return o1global - o2global;
            // }
            //                
            // });
            
            for (InterfaceAddress addr : addrs) {
                
                InetAddress inetAddr = addr.getAddress();
                if (inetAddr.isLoopbackAddress() || inetAddr.isLinkLocalAddress())
                    continue;
                
                if (!(inetAddr.isLinkLocalAddress() || inetAddr.isSiteLocalAddress())) {
                    final String hostAddr = getHostAddress(inetAddr);
                    final String uri = getURI(protocol,inetAddr,port);
                    AddressMapping.Builder amap = AddressMapping.newBuilder().setAddress(hostAddr).setPort(port).setProtocol(protocol).setTtlS(3600).setMatchNetwork("*").setUri(uri).setVersion(0).setUuid("");
                    endpoints.add(amap);
                    /*endpoints.add(new AddressMapping("", 0, protocol, hostAddr, port, "*",
                        3600,uri));*/
                    break;
                }
                
                // endpoints.add(RPCClient.generateMap("address",
                // inetAddr.getHostAddress(), "port",
                // port, "protocol", protocol, "ttl", 3600, "match_network",
                // (inetAddr
                // .isLinkLocalAddress()
                // || inetAddr.isSiteLocalAddress() ? inetAddr.getHostAddress()
                // + "/"
                // + getSubnetMaskString(addr.getNetworkPrefixLength()) :
                // "*")));
            }
            
            // stop searching for endpoints if an endpoint has been found
            if (!endpoints.isEmpty())
                break;
        }
        
        // if no globally reachable endpoints are available, pick the first
        // locally reachable endpoint
        if (endpoints.isEmpty()) {
            
            ifcs = NetworkInterface.getNetworkInterfaces();
            
            while (ifcs.hasMoreElements()) {
                
                NetworkInterface ifc = ifcs.nextElement();
                List<InterfaceAddress> addrs = ifc.getInterfaceAddresses();
                
                // if there is no "public" IP check for a site local address to
                // use
                for (InterfaceAddress addr : addrs) {
                    
                    InetAddress inetAddr = addr.getAddress();
                    
                    if (inetAddr.isSiteLocalAddress()) {
                        final String hostAddr = getHostAddress(inetAddr);
                        final String uri = getURI(protocol,inetAddr,port);
                        //endpoints.add(new AddressMapping("", 0, protocol, hostAddr, port, "*", 3600, uri));
                        AddressMapping.Builder amap = AddressMapping.newBuilder().setAddress(hostAddr).setPort(port).setProtocol(protocol).setTtlS(3600).setMatchNetwork("*").setUri(uri).setVersion(0).setUuid("");
                        endpoints.add(amap);
                        break;
                    }
                }
                
                if (!endpoints.isEmpty())
                    break;
            }
        }
        
        // in case no IP address could be found at all, use 127.0.0.1 for local
        // testing
        if (endpoints.isEmpty()) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.net, null,
                "could not find a valid IP address, will use 127.0.0.1 instead", new Object[0]);
            //endpoints.add(new AddressMapping("", 0, protocol, "127.0.0.1", port, "*", 3600, getURI(protocol, InetAddress.getLocalHost(), port)));
            AddressMapping.Builder amap = AddressMapping.newBuilder().setAddress("127.0.0.1").setPort(port).setProtocol(protocol).setTtlS(3600).setMatchNetwork("*").setUri(getURI(protocol, InetAddress.getLocalHost(), port)).setVersion(0).setUuid("");
            endpoints.add(amap);
        }
        
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
            System.out.println(endpoint);
    }
    
}
