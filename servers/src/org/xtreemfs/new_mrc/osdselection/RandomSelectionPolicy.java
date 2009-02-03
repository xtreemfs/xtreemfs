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

package org.xtreemfs.new_mrc.osdselection;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A very simple policy that accepts all osds!
 *
 * @author bjko
 */
public class RandomSelectionPolicy extends AbstractSelectionPolicy{

    public static final short  POLICY_ID         = 1;

    /** Creates a new instance of SimpleSelectionPolicy */
    public RandomSelectionPolicy() {
    }

    public String[] getOSDsForNewFile(Map<String, Map<String, Object>> osdMap,
        InetAddress clientAddress, int amount, String args) {

        // first, sort out all OSDs with insufficient free capacity
        String[] osds = new String[amount];
        List<String> list = new LinkedList<String>();
        for (String osd : osdMap.keySet()) {
            if (hasFreeCapacity(osdMap.get(osd)))
                list.add(osd);
        }

        // from the remaining set, take a random subset of OSDs
        for (int i = 0; i < amount; i++)
            osds[i] = list.remove((int) (Math.random() * list.size()));

        return osds;
    }

}
