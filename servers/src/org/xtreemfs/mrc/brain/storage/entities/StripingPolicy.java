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

public class StripingPolicy implements Serializable {

    private String policy;

    private long   stripeSize;

    private long   width;

    public StripingPolicy() {
    }

    public StripingPolicy(String policy, long stripeSize, long width) {
        this.policy = policy;
        this.stripeSize = stripeSize;
        this.width = width;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public long getStripeSize() {
        return stripeSize;
    }

    public void setStripeSize(long stripeSize) {
        this.stripeSize = stripeSize;
    }

    public long getWidth() {
        return width;
    }

    public void setWidth(long width) {
        this.width = width;
    }

    public boolean equals(Object obj) {

        if (!(obj instanceof StripingPolicy))
            return false;

        StripingPolicy sp = (StripingPolicy) obj;
        return policy.equals(sp.policy) && stripeSize == sp.stripeSize
            && width == sp.width;
    }

    public String toString() {
        return policy + ", " + stripeSize + ", " + width;
    }

}
