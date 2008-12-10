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

package org.xtreemfs.mrc.brain;

/**
 * This exception is thrown if something
 * 
 * @author bjko, stender
 */
public class UserException extends java.lang.Exception {

    private int errno = 0;

    /**
     * Creates a new instance of <code>XtreemFSException</code> without detail
     * message.
     */
    public UserException() {
    }

    public UserException(int errno) {
        this.errno = errno;
    }

    public UserException(String message) {
        super(message);
    }

    public UserException(int errno, String message) {
        super(message + " (errno=" + errno + ")");
        this.errno = errno;
    }

    public int getErrno() {
        return this.errno;
    }
}
