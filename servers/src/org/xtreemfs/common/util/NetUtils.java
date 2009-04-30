/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Jan Stender (ZIB)
 */
package org.xtreemfs.common.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xtreemfs.include.common.logging.Logging;
import org.xtreemfs.interfaces.AddressMapping;
import org.xtreemfs.interfaces.AddressMappingSet;

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
    public static AddressMappingSet getReachableEndpoints(int port, String protocol) throws IOException {
        
        AddressMappingSet endpoints = new AddressMappingSet();
        
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
                    endpoints.add(new AddressMapping("", 0, protocol, inetAddr.getHostAddress(), port, "*",
                        3600));
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
                        endpoints.add(new AddressMapping("", 0, protocol, inetAddr.getHostAddress(), port,
                            "*", 3600));
                        break;
                    }
                }
                
                if (!endpoints.isEmpty())
                    break;
            }
        }
        
        // in case no IP address could be found at all, use 127.0.0.1 for local testing
        if (endpoints.isEmpty()) {
            Logging.logMessage(Logging.LEVEL_WARN, null,
                "could not find a valid IP address, will use 127.0.0.1 instead");
            endpoints.add(new AddressMapping("", 0, protocol, "127.0.0.1", port, "*", 3600));
        }
        
        return endpoints;
        
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
        for (AddressMapping endpoint : NetUtils.getReachableEndpoints(32640, "http"))
            System.out.println(endpoint);
    }
    
}
