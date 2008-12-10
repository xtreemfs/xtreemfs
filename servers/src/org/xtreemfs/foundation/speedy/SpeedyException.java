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
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.foundation.speedy;

/**
 * 
 * @author bjko
 */
public class SpeedyException extends java.lang.Exception {

    boolean abortConnection;

    /**
     * Creates a new instance of <code>SpeedyException</code> without detail
     * message.
     */
    public SpeedyException() {
    }

    /**
     * Constructs an instance of <code>SpeedyException</code> with the
     * specified detail message.
     * 
     * @param msg
     *            the detail message.
     */
    public SpeedyException(String msg) {
        super(msg);
    }

    public SpeedyException(String msg, boolean abort) {
        super(msg);
        this.abortConnection = abort;
    }

    public boolean isAbort() {
        return this.abortConnection;
    }
}
