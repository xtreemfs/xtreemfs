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

package org.xtreemfs.test.mrc;

import java.net.InetAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceDataMap;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.ServiceType;
import org.xtreemfs.mrc.osdselection.ProximitySelectionPolicy;

public class SelectionPolicyTest extends TestCase {
    
    private static final long                MIN_FREE_CAPACITY = 32 * 1024 * 1024;
    
    private ProximitySelectionPolicy         policy;
    
    private ServiceSet osdMap;
    
    private InetAddress                      clientAddress;
    
    public void setUp() throws Exception {
        
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        
        policy = new ProximitySelectionPolicy();
        
        clientAddress = InetAddress.getByName(URI.create("http://01.xtreemfs.com").getHost());
        
        osdMap = new ServiceSet();

        ServiceDataMap dmap = new ServiceDataMap();
        dmap.put("free",Long.toString(MIN_FREE_CAPACITY + 1));
        dmap.put("uri", "http://itu.dk");
        Service attr1 = new Service(ServiceType.SERVICE_TYPE_OSD,"attr1",0,"",0,dmap);
        osdMap.add(attr1);

        dmap = new ServiceDataMap();
        dmap.put("free",Long.toString(MIN_FREE_CAPACITY + 1));
        dmap.put("uri", "http://wiut.uz");
        Service attr2 = new Service(ServiceType.SERVICE_TYPE_OSD,"attr2",0,"",0,dmap);
        osdMap.add(attr2);
         
        dmap = new ServiceDataMap();
        dmap.put("free",Long.toString(MIN_FREE_CAPACITY + 1));
        dmap.put("uri", "http://pku.edu.cn");
        Service attr3 = new Service(ServiceType.SERVICE_TYPE_OSD,"attr3",0,"",0,dmap);
        osdMap.add(attr3);

        dmap = new ServiceDataMap();
        dmap.put("free",Long.toString(MIN_FREE_CAPACITY + 1));
        dmap.put("uri", "http://xtreemfs2.zib.de");
        Service attr4 = new Service(ServiceType.SERVICE_TYPE_OSD,"attr4",0,"",0,dmap);
        osdMap.add(attr4);


        dmap = new ServiceDataMap();
        dmap.put("free",Long.toString(MIN_FREE_CAPACITY + 1));
        dmap.put("uri", "http://xtreemfs2.zib.de");
        Service attr5 = new Service(ServiceType.SERVICE_TYPE_OSD,"attr5",0,"",0,dmap);
        osdMap.add(attr5);
        
    }
    
    public void tearDown() throws Exception {
        
    }
    
    public void testInetAddressToInteger() throws Exception {
        
        byte[] bytes = { (byte) 130, (byte) 226, (byte) 142, 3 };
        assertEquals(130226142003L, policy.inetAddressToLong(bytes));
        
        byte[] bytes2 = { (byte) 80, (byte) 80, (byte) 214, 93 };
        assertEquals(80080214093L, policy.inetAddressToLong(bytes2));
    }
    
    public void testGetOSDsForNewFile() {
        
        String[] osds = policy.getOSDsForNewFile(osdMap, clientAddress, 4, null);
        assertEquals(osds.length, 4);
        assertTrue(contains(osds, "attr1"));
        assertTrue(contains(osds, "attr3"));
        assertTrue(contains(osds, "attr4"));
        assertTrue(contains(osds, "attr5"));
        
        osds = policy.getOSDsForNewFile(osdMap, clientAddress, 2, null);
        assertEquals(osds.length, 2);
        assertTrue(contains(osds, "attr4"));
        assertTrue(contains(osds, "attr5"));
        
        osds = policy.getOSDsForNewFile(osdMap, clientAddress, 1, null);
        assertEquals(osds.length, 1);
        assertTrue(contains(osds, "attr4") || contains(osds, "attr5"));
        
        osds = policy.getOSDsForNewFile(osdMap, clientAddress, 6, null);
        assertEquals(osds.length, 6);
        assertEquals(osds[5], null);
    }
    
    private boolean contains(String[] array, String s) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(s))
                return true;
        }
        return false;
    }
    
    public static void main(String[] args) {
        TestRunner.run(SelectionPolicyTest.class);
    }
    
}
