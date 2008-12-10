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

package org.xtreemfs.mrc.brain.storage.entities;

import java.io.Serializable;

public class XLocation implements Serializable {

    private StripingPolicy stripingPolicy;

    private String[] osdList;

    public XLocation() {
    }

    public XLocation(StripingPolicy stripingPolicy, String[] osdList) {
        this.stripingPolicy = stripingPolicy;
        this.osdList = osdList;
    }

    public String[] getOsdList() {
        return osdList;
    }

    public void setOsdList(String[] osdList) {
        this.osdList = osdList;
    }

    public StripingPolicy getStripingPolicy() {
        return stripingPolicy;
    }

    public void setStripingPolicy(StripingPolicy stripingPolicy) {
        this.stripingPolicy = stripingPolicy;
    }

    public boolean equals(Object obj) {

        if(!(obj instanceof XLocation))
            return false;

        XLocation xloc = (XLocation) obj;
        if(!stripingPolicy.equals(xloc.stripingPolicy))
            return false;

        if(osdList.length != xloc.osdList.length)
            return false;

        for(int i = 0; i < osdList.length; i++)
            if(!osdList[i].equals(xloc.osdList[i]))
                return false;

        return true;
    }

}
