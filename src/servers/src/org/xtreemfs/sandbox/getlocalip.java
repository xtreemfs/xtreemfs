/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xtreemfs.sandbox;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import org.xtreemfs.common.util.NetUtils;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.interfaces.AddressMapping;
import org.xtreemfs.interfaces.AddressMappingSet;

/**
 *
 * @author bjko
 */
public class getlocalip {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here

        try {
            AddressMappingSet endpoints = new AddressMappingSet();
            final String protocol = "oncrpc";
            final int port = 12345;

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
                    if (inetAddr.isLoopbackAddress() || inetAddr.isLinkLocalAddress()) {
                        continue;
                    }

                    if (!(inetAddr.isLinkLocalAddress() || inetAddr.isSiteLocalAddress())) {
                        final String hostAddr = NetUtils.getHostAddress(inetAddr);
                        final String uri = NetUtils.getURI(protocol, inetAddr, port);
                        endpoints.add(new AddressMapping("", 0, protocol, hostAddr, port, "*",
                                3600, uri));
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
                if (!endpoints.isEmpty()) {
                    break;
                }
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
                            final String hostAddr = NetUtils.getHostAddress(inetAddr);
                            final String uri = NetUtils.getURI(protocol, inetAddr, port);
                            endpoints.add(new AddressMapping("", 0, protocol, hostAddr, port, "*", 3600, uri));
                            break;
                        }
                    }

                    if (!endpoints.isEmpty()) {
                        break;
                    }
                }
            }

            // in case no IP address could be found at all, use 127.0.0.1 for local
            // testing
            if (endpoints.isEmpty()) {
                Logging.logMessage(Logging.LEVEL_WARN, Category.net, null,
                        "could not find a valid IP address, will use 127.0.0.1 instead", new Object[0]);
                endpoints.add(new AddressMapping("", 0, protocol, "127.0.0.1", port, "*", 3600, NetUtils.getURI(protocol, InetAddress.getLocalHost(), port)));
            }
            System.out.println("");
            System.out.println("result: " + endpoints);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }
}
