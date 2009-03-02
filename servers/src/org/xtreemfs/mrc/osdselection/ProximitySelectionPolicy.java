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
 * AUTHORS: Nele Andersen (ZIB)
 */

package org.xtreemfs.mrc.osdselection;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.PriorityQueue;

import java.util.Map;
import java.net.URI;
import java.net.UnknownHostException;
import org.xtreemfs.interfaces.ServiceRegistry;
import org.xtreemfs.interfaces.ServiceRegistrySet;

public class ProximitySelectionPolicy extends AbstractSelectionPolicy{

    public static final short POLICY_ID = 2;

    private byte[] clientAddress;
    private long clientAddressLong;

    public String[] getOSDsForNewFile(ServiceRegistrySet osdSet,
            InetAddress clientAddress, int amount, String args) {

        this.clientAddress = clientAddress.getAddress();
        clientAddressLong = inetAddressToLong(this.clientAddress);

        // sort all OSDs with sufficient free capacity according to the value
        // returned by the method distance
        String[] osds = new String[amount];
        PriorityQueue<Pair> queue = new PriorityQueue<Pair>();
        LinkedList<String> list = new LinkedList<String>();

        for (ServiceRegistry osd : osdSet) {
            if (hasFreeCapacity(osd)) {
                try {
                    queue.add(new Pair(osd, distance(osd)));
                } catch (UnknownHostException e) {
                }
            }
        }

        for (int i = 0; !queue.isEmpty()
                && (queue.peek().getDistance() == 0 || i < amount); i++)
            list.add(queue.poll().getOsd().getUuid());

        for (int i = 0; !list.isEmpty() && i < amount; i++)
            osds[i] = list.remove((int) (Math.random() * list.size()));

        return osds;

    }

    private long distance(ServiceRegistry osd) throws UnknownHostException {

        /*byte[] osdAddress = InetAddress.getByName(
                (URI.create((String) osd.get("uri")).getHost())).getAddress();

        // if osd in same subnet as client
        if (osdAddress[0] == clientAddress[0]
                && osdAddress[1] == clientAddress[1]
                && osdAddress[2] == clientAddress[2])
            return 0;

        return Math.abs(inetAddressToLong(osdAddress) - clientAddressLong);*/
        return 1;
    }

    public long inetAddressToLong(byte[] address) {

        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < address.length; i++) {

            if (address[i] < 0)
                sb.append(256 + address[i]);
            else if (address[i] < 10)
                sb.append("00" + address[i]);
            else if (address[i] < 100)
                sb.append("0" + address[i]);
            else
                sb.append(address[i]);
        }
        return Long.parseLong(sb.toString());
    }

    class Pair implements Comparable<Pair> {

        private ServiceRegistry osd;
        private long distance;

        Pair(ServiceRegistry osd, long distance) {
            this.osd = osd;
            this.distance = distance;
        }

        public String toString() {
            return "(" + osd + ", " + distance + ")";
        }

        public long getDistance() {
            return distance;
        }

        public ServiceRegistry getOsd() {
            return osd;
        }

        public int compareTo(Pair other) {
            if (this.distance < other.distance)
                return -1;
            if (this.distance > other.distance)
                return 1;
            else
                return 0;
        }
    }

}
