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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc.brain.storage;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.xtreemfs.common.logging.Logging;

/**
 * Generates globally unique IDs for volumes based on the MAC, time and a
 * random component.
 * @author bjko
 */
public class VolIDGen {

    private String ADDR_PART;

    private byte[] addr;

    private static VolIDGen instance = null;


    /** Creates a new instance of VolIDGen
     * @throws SocketException if something is wrong with the sockets and MACs cannto be read
     */
    public VolIDGen() throws SocketException {
        byte[] mac = null;
        byte[] virtualMac = null;
        Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
        while (nifs.hasMoreElements()) {
            NetworkInterface nif = nifs.nextElement();
            byte[] tmp = nif.getHardwareAddress();
            if (tmp != null) {
                //okay, IF has a MAC address. Not all have one, think of lo

                //we do not want a VM MAC, but take it if there is nothing else
                if ( (tmp[0] == 0) && (tmp[1] == 0x50) && (tmp[2] == 0x56)) {
                    //VMWARE's MACs
                    virtualMac = tmp;
                } else if ( (tmp[0] == 0) && (tmp[1] == 0x16) && ((0xFF & tmp[2]) >= 0xE0)) {
                    //XEN's MACs
                    virtualMac = tmp;
                } else {
                    //take a real MAC
                    mac = tmp;
                }
            }
        }
        if (mac == null) {
            if (virtualMac == null) {
                //THIS IS BAD!
                //GOTO shoot the MRC in the foot ;-)

                Logging.logMessage(Logging.LEVEL_WARN, this,
                        "cannot find a Hardware/MAC address to initialize, using random number instead");

                mac = new byte[6];
                for (int i = 0; i < mac.length; i++)
                    mac[i] = (byte) (Math.random() * 256 - 128);

            } else {
                Logging.logMessage(Logging.LEVEL_WARN, this,
                        "using a mac that is part of a virual machine - this may cause volumeID collisions!");

                mac = virtualMac;
            }
        }

        //make it a six byte HEXTRING w/ leading zzzzzeros
        ADDR_PART = byteToHex(mac[0] & 0x00FF)+byteToHex(mac[1] & 0x00FF)+byteToHex(mac[2] & 0x00FF)+
                    byteToHex(mac[3] & 0x00FF)+byteToHex(mac[4] & 0x00FF)+byteToHex(mac[5] & 0x00FF);
        addr = mac;

    }

    /** Creates a new unique volume ID
     *  @return the new volumeID
     *  @deprecated use SliceID instead
     */
    public String getNewVolID() {
        int time = (int)(System.currentTimeMillis()/1000l);

        return ADDR_PART+intToHex(time)+shortToHex((int)(Math.random()*0xFFFF));

    }

    public byte[] getNewVolIDBytes() {
        int time = (int)(System.currentTimeMillis()/1000l);
        int rand = (int)(Math.random()*0xFFFF);
        byte[] arr = new byte[]{addr[0],addr[1],addr[2],addr[3],addr[4],addr[5],
                    (byte)(time & 0xFF), (byte)(time >> 8 & 0xFF), (byte)(time >> 16 & 0xFF),
                    (byte)(time >> 24 & 0xFF), (byte)(rand & 0xFF),  (byte)(rand >> 8 & 0xFF)};
        return arr;
    }

    /** Gets the single instance of VolIDGen
     */
    public static synchronized VolIDGen getGenerator() throws SocketException {
        if (instance == null) {
            instance = new VolIDGen();
        }
        return instance;
    }

    //helper too simple to document
    public static String byteToHex(int in) {
        return String.format("%02X",in);
    }
    public static String intToHex(int in) {
        return String.format("%08X",in);
    }
    public static String shortToHex(int in) {
        return String.format("%04X",in);
    }

}
